package dev.verloren.midnight.annotator;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.execution.util.ExecUtil;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.ExternalAnnotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import dev.verloren.midnight.psi.CompactFile;
import dev.verloren.midnight.run.CompactToolchainUtil;
import dev.verloren.midnight.settings.MidnightSettingsState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs the official Compact compiler ({@code compactc}) asynchronously as an {@link ExternalAnnotator}.
 *
 * <h3>IntelliJ Platform Subsystem Role & Calling Pipeline</h3>
 * <p>
 * Registered in {@code plugin.xml} under the {@code <externalAnnotator>} extension point:
 * <ul>
 *   <li><b>Deep Semantic Verification:</b> Supplements fast in-memory syntax checks with deep compiler-level
 *       verification (type checking, ZK constraint checking, pragma validation, and IR lowering errors).</li>
 *   <li><b>Asynchronous 3-Phase Lifecycle:</b>
 *     <ol>
 *       <li>{@link #collectInformation(PsiFile, Editor, boolean)}: Executed on the Event Dispatch Thread (EDT)
 *           inside a Read Action. Gathers file metadata, captures unsaved buffer snapshots, and reads user settings.</li>
 *       <li>{@link #doAnnotate(InitialInfo)}: Executed completely asynchronously on a pooled background thread
 *           <b>without holding any locks</b>. Spawns {@code compactc} with a 5000ms safety timeout and parses output.</li>
 *       <li>{@link #apply(PsiFile, AnnotationResult, AnnotationHolder)}: Executed back on the EDT inside a Read Action.
 *           Translates coordinates into editor text ranges and attaches annotations to the {@link AnnotationHolder}.</li>
 *     </ol>
 *   </li>
 * </ul>
 * </p>
 *
 * <h3>External Annotator Lifecycle Visual Diagram</h3>
 * <pre>
 *   [Editor Active]
 *          │
 *          ▼
 *   1. collectInformation()  [EDT / Read Lock]   ---&gt; Captures unsaved Document buffer snapshot
 *          │
 *          ▼
 *   2. doAnnotate()          [Background / No Lock] ---&gt; Runs compactc via ExecUtil (timeout: 5s)
 *          │                                           ---&gt; Parses stdout/stderr via OutputParser
 *          ▼
 *   3. apply()               [EDT / Read Lock]   ---&gt; Maps coordinates to Document TextRanges
 *                                                ---&gt; Verifies modificationStamp to avoid race
 * </pre>
 *
 * <h3>Edge Case Mitigations & Race Guards</h3>
 * <ul>
 *   <li><b>Unsaved Document Shadowing:</b> If the user has unsaved edits in memory, {@link #doAnnotate} writes
 *       the unsaved buffer to a temporary shadow file and points {@code compactc} to the shadow file, ensuring
 *       diagnostics reflect the live keystroke state without forcing unwanted disk saves.</li>
 *   <li><b>Stale Annotation Race Guard:</b> {@link #apply} compares {@code document.getModificationStamp()}
 *       against {@code result.modificationStamp()}. If the document was edited while the compiler was executing,
 *       the stale compiler results are discarded silently.</li>
 *   <li><b>WSL & Windows Path Translation:</b> Intelligently translates Windows drive paths to WSL paths
 *       ({@code /mnt/c/...}) when running under WSL toolchains and normalizes diagnostic paths back for editor display.</li>
 *   <li><b>ZK Optimization:</b> Passes {@code --skip-zk} by default to speed up compilation speeds during
 *       on-the-fly typing, controlled via {@link MidnightSettingsState#skipZkDefault}.</li>
 * </ul>
 *
 * @see dev.verloren.midnight.annotator.CompactCompilerOutputParser
 * @see dev.verloren.midnight.annotator.CompactCompilerDiagnostic
 * @see dev.verloren.midnight.run.CompactToolchainUtil
 */
public class CompactExternalAnnotator extends ExternalAnnotator<CompactExternalAnnotator.InitialInfo, CompactExternalAnnotator.AnnotationResult> {
  private static final Logger LOG = Logger.getInstance(CompactExternalAnnotator.class);

  /**
   * Maximum process execution timeout (5 seconds) to prevent hanging background worker threads.
   */
  private static final int COMPILER_TIMEOUT_MS = 5000;

  /**
   * Information captured during Phase 1 (EDT) and passed to the background compiler worker.
   */
  public record InitialInfo(
      @NotNull PsiFile file,
      @NotNull String filePath,
      @Nullable String unsavedContent,
      boolean skipZk,
      long modificationStamp
  ) {}

  /**
   * Diagnostic results produced during Phase 2 (background) and applied in Phase 3 (EDT).
   */
  public record AnnotationResult(
      long modificationStamp,
      @NotNull List<CompactCompilerDiagnostic> diagnostics
  ) {}

  /**
   * Phase 1: Gathers snapshot information on the EDT before kicking off the background compiler task.
   */
  @Override
  public @Nullable InitialInfo collectInformation(@NotNull PsiFile file, @NotNull Editor editor, boolean hasErrors) {
    if (!(file instanceof CompactFile)) {
      return null;
    }
    VirtualFile vFile = file.getVirtualFile();
    if (vFile == null || !vFile.isInLocalFileSystem()) {
      return null;
    }

    Document document = editor.getDocument();
    FileDocumentManager docManager = FileDocumentManager.getInstance();
    // Capture unsaved memory buffer if the document has uncommitted edits
    String unsavedContent = docManager.isDocumentUnsaved(document) ? document.getText() : null;

    MidnightSettingsState state = MidnightSettingsState.getInstance();
    boolean skipZk = state == null || state.skipZkDefault;

    return new InitialInfo(file, vFile.getPath(), unsavedContent, skipZk, document.getModificationStamp());
  }

  /**
   * Phase 2: Runs {@code compactc} on a background thread without locks.
   */
  @Override
  public @Nullable AnnotationResult doAnnotate(@NotNull InitialInfo info) {
    CompactToolchainUtil.ToolchainInfo toolchain = CompactToolchainUtil.getToolchainInfo(info.file().getProject());
    if (toolchain == null || !toolchain.isValid()) {
      return null;
    }

    // Step 1: Determine output directory (native Linux /tmp for WSL, Windows temp dir otherwise)
    File outDir = null;
    File shadowDir = null;
    String outDirPath;
    if (toolchain.isWsl()) {
      outDirPath = "/tmp/compact-annotator-" + Math.abs(info.filePath().hashCode());
    } else {
      try {
        outDir = FileUtil.createTempDirectory("compact-annotator", null);
        outDirPath = outDir.getAbsolutePath();
      } catch (Exception e) {
        LOG.warn("Failed to create temporary output directory for compactc", e);
        return null;
      }
    }

    try {
      // Step 2: Write unsaved editor text to a temporary shadow file if needed
      String sourcePath = info.filePath();
      if (info.unsavedContent() != null) {
        try {
          shadowDir = FileUtil.createTempDirectory("compact-shadow", null);
          File shadowFile = new File(shadowDir, info.file().getName());
          FileUtil.writeToFile(shadowFile, info.unsavedContent());
          sourcePath = shadowFile.getAbsolutePath();
        } catch (Exception e) {
          LOG.warn("Failed to create shadow source file for compactc", e);
          sourcePath = info.filePath();
        }
      }

      // Step 3: Build command line arguments
      List<String> args = buildCompilerArgs(info, outDirPath, sourcePath);

      GeneralCommandLine commandLine = CompactToolchainUtil.createCommandLine(
          info.file().getProject(),
          args,
          info.file().getProject().getBasePath()
      );

      // Step 4: Execute compiler process with timeout safety
      ProcessOutput output = ExecUtil.execAndGetOutput(commandLine, COMPILER_TIMEOUT_MS);
      String combinedOutput = output.getStdout() + "\n" + output.getStderr();

      // Step 5: Parse stdout/stderr diagnostics and filter for target file
      List<CompactCompilerDiagnostic> allDiagnostics = CompactCompilerOutputParser.parse(combinedOutput);
      List<CompactCompilerDiagnostic> fileDiagnostics = new ArrayList<>();
      for (CompactCompilerDiagnostic diag : allDiagnostics) {
        if (isDiagnosticForFile(diag, info.file())) {
          fileDiagnostics.add(diag);
        }
      }

      return new AnnotationResult(info.modificationStamp(), fileDiagnostics);
    } catch (ExecutionException e) {
      LOG.warn("Failed to run compactc external annotator", e);
      return new AnnotationResult(info.modificationStamp(), List.of());
    } finally {
      // Clean up temporary working directories
      if (outDir != null) {
        FileUtil.delete(outDir);
      }
      if (shadowDir != null) {
        FileUtil.delete(shadowDir);
      }
    }
  }

  /**
   * Constructs the CLI arguments for the compiler invocation.
   */
  private static @NotNull List<String> buildCompilerArgs(
      @NotNull InitialInfo info,
      @NotNull String outDirPath,
      @NotNull String sourcePath
  ) {
    List<String> args = new ArrayList<>();
    if (info.skipZk()) {
      args.add("--skip-zk");
    }

    // Add parent directory of target source file to compact search path for relative includes
    VirtualFile parent = info.file().getVirtualFile() != null ? info.file().getVirtualFile().getParent() : null;
    if (parent != null) {
      args.add("--compact-path");
      args.add(parent.getPath());
    }

    args.add("-o");
    args.add(outDirPath);
    args.add(sourcePath);
    return args;
  }

  /**
   * Phase 3: Applies diagnostics as editor annotations on the EDT.
   */
  @Override
  public void apply(@NotNull PsiFile file, @Nullable AnnotationResult result, @NotNull AnnotationHolder holder) {
    if (result == null || result.diagnostics().isEmpty()) {
      return;
    }

    Document document = file.getViewProvider().getDocument();
    if (document == null) {
      return;
    }

    // Stale annotation race guard: verify that the document was not edited while compactc was running
    if (document.getModificationStamp() != result.modificationStamp()) {
      return;
    }

    // Register annotations with matching ranges and severities
    for (CompactCompilerDiagnostic diagnostic : result.diagnostics()) {
      TextRange range = getRange(document, diagnostic.line(), diagnostic.column());
      HighlightSeverity severity = diagnostic.severity();

      holder.newAnnotation(severity, diagnostic.message())
          .range(range)
          .create();
    }
  }

  /**
   * Validates whether a parsed diagnostic belongs to the analyzed source file.
   */
  static boolean isDiagnosticForFile(@NotNull CompactCompilerDiagnostic diagnostic, @NotNull PsiFile file) {
    VirtualFile vFile = file.getVirtualFile();
    if (vFile != null && isDiagnosticForVirtualFile(diagnostic, vFile)) {
      return true;
    }

    String diagPath = diagnostic.filePath().replace('\\', '/');
    String fileName = file.getName();
    return diagPath.equals(fileName) || diagPath.endsWith("/" + fileName);
  }

  /**
   * Checks whether a diagnostic path matches a VirtualFile, handling WSL path translations.
   */
  static boolean isDiagnosticForVirtualFile(@NotNull CompactCompilerDiagnostic diagnostic, @NotNull VirtualFile vFile) {
    String diagPath = diagnostic.filePath().replace('\\', '/');
    String filePath = vFile.getPath().replace('\\', '/');
    if (filePath.startsWith("/") && filePath.length() >= 3 && Character.isLetter(filePath.charAt(1)) && filePath.charAt(2) == ':') {
      filePath = filePath.substring(1);
    }

    if (diagPath.equalsIgnoreCase(filePath)) {
      return true;
    }

    if (diagPath.startsWith("/mnt/")) {
      String translated = CompactToolchainUtil.toWindowsPath(diagPath).replace('\\', '/');
      return translated.equalsIgnoreCase(filePath);
    }

    return false;
  }

  /**
   * Translates 1-based line/column coordinates into a word-bounded editor {@link TextRange}.
   */
  static @NotNull TextRange getRange(@NotNull Document document, int line, int column) {
    int lineCount = document.getLineCount();
    if (lineCount == 0) {
      return TextRange.EMPTY_RANGE;
    }

    int lineIndex = Math.clamp(line - 1, 0, lineCount - 1);
    int lineStart = document.getLineStartOffset(lineIndex);
    int lineEnd = document.getLineEndOffset(lineIndex);

    if (lineStart >= lineEnd) {
      return new TextRange(lineStart, lineEnd);
    }

    int startOffset = lineStart + Math.max(0, column - 1);
    if (startOffset > lineEnd) {
      startOffset = lineEnd;
    }

    // Find end of identifier or word at the error offset
    CharSequence text = document.getCharsSequence();
    int endOffset = startOffset;
    while (endOffset < lineEnd && isIdentifierPart(text.charAt(endOffset))) {
      endOffset++;
    }

    if (endOffset == startOffset) {
      endOffset = Math.min(startOffset + 1, lineEnd);
    }

    return new TextRange(startOffset, Math.max(startOffset, endOffset));
  }

  private static boolean isIdentifierPart(char c) {
    return Character.isJavaIdentifierPart(c) || c == '-' || c == '\'';
  }
}
