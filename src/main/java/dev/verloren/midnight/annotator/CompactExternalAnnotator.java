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
 * Runs {@code compactc} asynchronously as an external annotator to provide
 * deep semantic diagnostics, type mismatches, and syntax errors in the editor.
 */
public class CompactExternalAnnotator extends ExternalAnnotator<CompactExternalAnnotator.InitialInfo, CompactExternalAnnotator.AnnotationResult> {
  private static final Logger LOG = Logger.getInstance(CompactExternalAnnotator.class);
  private static final int COMPILER_TIMEOUT_MS = 5000;

  public record InitialInfo(
      @NotNull PsiFile file,
      @NotNull String filePath,
      @Nullable String unsavedContent,
      boolean skipZk,
      long modificationStamp
  ) {}

  public record AnnotationResult(
      long modificationStamp,
      @NotNull List<CompactCompilerDiagnostic> diagnostics
  ) {}

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
    String unsavedContent = docManager.isDocumentUnsaved(document) ? document.getText() : null;

    MidnightSettingsState state = MidnightSettingsState.getInstance();
    boolean skipZk = state == null || state.skipZkDefault;

    return new InitialInfo(file, vFile.getPath(), unsavedContent, skipZk, document.getModificationStamp());
  }

  @Override
  public @Nullable AnnotationResult doAnnotate(@NotNull InitialInfo info) {
    CompactToolchainUtil.ToolchainInfo toolchain = CompactToolchainUtil.getToolchainInfo(info.file().getProject());
    if (toolchain == null || !toolchain.isValid()) {
      return null;
    }

    // Determine output directory: native Linux /tmp for WSL, Windows temp dir otherwise
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

      GeneralCommandLine commandLine = CompactToolchainUtil.createCommandLine(
          info.file().getProject(),
          args,
          info.file().getProject().getBasePath()
      );

      ProcessOutput output = ExecUtil.execAndGetOutput(commandLine, COMPILER_TIMEOUT_MS);
      String combinedOutput = output.getStdout() + "\n" + output.getStderr();

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
      if (outDir != null) {
        FileUtil.delete(outDir);
      }
      if (shadowDir != null) {
        FileUtil.delete(shadowDir);
      }
    }
  }

  @Override
  public void apply(@NotNull PsiFile file, @Nullable AnnotationResult result, @NotNull AnnotationHolder holder) {
    if (result == null || result.diagnostics().isEmpty()) {
      return;
    }

    Document document = file.getViewProvider().getDocument();
    if (document == null) {
      return;
    }

    if (document.getModificationStamp() != result.modificationStamp()) {
      return;
    }

    for (CompactCompilerDiagnostic diagnostic : result.diagnostics()) {
      TextRange range = getRange(document, diagnostic.line(), diagnostic.column());
      HighlightSeverity severity = diagnostic.severity();

      holder.newAnnotation(severity, diagnostic.message())
          .range(range)
          .create();
    }
  }

  static boolean isDiagnosticForFile(@NotNull CompactCompilerDiagnostic diagnostic, @NotNull PsiFile file) {
    VirtualFile vFile = file.getVirtualFile();
    if (vFile != null && isDiagnosticForVirtualFile(diagnostic, vFile)) {
      return true;
    }

    String diagPath = diagnostic.filePath().replace('\\', '/');
    String fileName = file.getName();
    if (diagPath.equals(fileName) || diagPath.endsWith("/" + fileName)) {
      return true;
    }

    return false;
  }

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
      if (translated.equalsIgnoreCase(filePath)) {
        return true;
      }
    }

    return false;
  }

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
