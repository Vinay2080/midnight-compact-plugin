package dev.verloren.midnight.annotator;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.CapturingProcessHandler;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.ExternalAnnotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.util.PathUtil;
import dev.verloren.midnight.psi.CompactFile;
import dev.verloren.midnight.run.CompactToolchainUtil;
import dev.verloren.midnight.settings.MidnightSettingsState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CompactExternalAnnotator extends ExternalAnnotator<CompactExternalAnnotator.InitialInfo, CompactExternalAnnotator.AnnotationResult> {
  private static final Logger LOG = Logger.getInstance(CompactExternalAnnotator.class);

  public record InitialInfo(
      @NotNull Project project,
      @NotNull VirtualFile virtualFile,
      @NotNull String compilerPath,
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

    MidnightSettingsState state = MidnightSettingsState.getInstance();
    String compilerPath = state != null && state.compilerPath != null && !state.compilerPath.isEmpty()
        ? state.compilerPath
        : "compactc";

    boolean skipZk = state == null || state.skipZkDefault;
    return new InitialInfo(file.getProject(), vFile, compilerPath, skipZk, file.getModificationStamp());
  }

  @Override
  public @Nullable AnnotationResult doAnnotate(@NotNull InitialInfo info) {
    ProgressManager.checkCanceled();
    File tempDir = null;
    try {
      tempDir = FileUtil.createTempDirectory("compact-annotator-output", null, true);

      List<String> args = new ArrayList<>();
      args.add("--vscode");
      if (info.skipZk()) {
        args.add("--skip-zk");
      }
      args.add(info.virtualFile().getPath());
      args.add(tempDir.getAbsolutePath());

      GeneralCommandLine cmd = CompactToolchainUtil.createCommandLine(
          info.project(),
          args,
          info.project().getBasePath()
      );
      cmd.setRedirectErrorStream(true);

      ProgressManager.checkCanceled();
      ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
      CapturingProcessHandler processHandler = new CapturingProcessHandler(cmd);
      ProcessOutput processOutput = indicator != null
          ? processHandler.runProcessWithProgressIndicator(indicator, 10000)
          : processHandler.runProcess(10000);

      if (processOutput.isCancelled()) {
        return null;
      }

      String stdout = processOutput.getStdout();
      String stderr = processOutput.getStderr();
      String output = stdout + (stderr.isEmpty() ? "" : "\n" + stderr);

      List<CompactCompilerDiagnostic> diagnostics = CompactCompilerOutputParser.parse(output);
      return new AnnotationResult(info.modificationStamp(), diagnostics);
    } catch (ProcessCanceledException e) {
      throw e;
    } catch (Exception e) {
      LOG.debug("Compact compiler external annotation failed", e);
      return null;
    } finally {
      if (tempDir != null) {
        FileUtil.delete(tempDir);
      }
    }
  }

  @Override
  public void apply(@NotNull PsiFile file, @Nullable AnnotationResult result, @NotNull AnnotationHolder holder) {
    if (result == null || result.diagnostics().isEmpty()) {
      return;
    }

    if (file.getModificationStamp() != result.modificationStamp()) {
      return;
    }

    Document document = PsiDocumentManager.getInstance(file.getProject()).getDocument(file);
    if (document == null) {
      return;
    }

    for (CompactCompilerDiagnostic diagnostic : result.diagnostics()) {
      if (!isDiagnosticForFile(diagnostic, file)) {
        continue;
      }

      TextRange range = getRange(diagnostic, document, file);
      HighlightSeverity severity = diagnostic.severity();

      holder.newAnnotation(severity, diagnostic.message())
          .range(range)
          .create();
    }
  }

  static boolean isDiagnosticForFile(@NotNull CompactCompilerDiagnostic diagnostic, @NotNull PsiFile file) {
    VirtualFile vFile = file.getVirtualFile();
    if (vFile == null) {
      return false;
    }
    return isDiagnosticForVirtualFile(diagnostic, vFile);
  }

  static boolean isDiagnosticForVirtualFile(@NotNull CompactCompilerDiagnostic diagnostic, @NotNull VirtualFile vFile) {
    String diagPath = diagnostic.filePath().trim();
    if (diagPath.isEmpty()) {
      return true;
    }

    String fileName = PathUtil.getFileName(vFile.getName().replace('\\', '/'));
    String diagFileName = PathUtil.getFileName(diagPath.replace('\\', '/'));
    if (!diagFileName.equalsIgnoreCase(fileName)) {
      return false;
    }

    if (diagPath.equalsIgnoreCase(diagFileName)) {
      return true;
    }

    String vPath = vFile.getPath().replace('\\', '/');
    if (vPath.startsWith("/") && vPath.length() >= 3 && Character.isLetter(vPath.charAt(1)) && vPath.charAt(2) == ':') {
      vPath = vPath.substring(1);
    }

    String normalizedDiag = diagPath.replace('\\', '/');

    if (normalizedDiag.startsWith("/mnt/") && normalizedDiag.length() >= 7 && normalizedDiag.charAt(6) == '/') {
      char driveLetter = Character.toUpperCase(normalizedDiag.charAt(5));
      normalizedDiag = driveLetter + ":" + normalizedDiag.substring(6);
    }

    if (vPath.equalsIgnoreCase(normalizedDiag)) {
      return true;
    }

    return vPath.toLowerCase().endsWith("/" + normalizedDiag.toLowerCase())
        || vPath.toLowerCase().endsWith(normalizedDiag.toLowerCase())
        || normalizedDiag.toLowerCase().endsWith("/" + vPath.toLowerCase())
        || normalizedDiag.toLowerCase().endsWith(vPath.toLowerCase());
  }

  private static @NonNull TextRange getRange(
      @NotNull CompactCompilerDiagnostic diagnostic,
      @NotNull Document document,
      @NotNull PsiFile file
  ) {
    int maxLine = Math.max(0, document.getLineCount() - 1);
    int lineIndex = Math.clamp(diagnostic.line() - 1, 0, maxLine);
    int lineStart = document.getLineStartOffset(lineIndex);
    int lineEnd = document.getLineEndOffset(lineIndex);

    int colOffset = Math.min(lineStart + Math.max(0, diagnostic.column() - 1), lineEnd);

    if (colOffset < lineEnd) {
      PsiElement element = file.findElementAt(colOffset);
      if (element != null && !(element instanceof PsiWhiteSpace)) {
        TextRange elemRange = element.getTextRange();
        if (elemRange.getStartOffset() >= lineStart && elemRange.getEndOffset() <= lineEnd && !elemRange.isEmpty()) {
          return elemRange;
        }
      }
    }

    int endOffset = Math.min(colOffset + 1, lineEnd);
    if (colOffset >= endOffset && lineStart < lineEnd) {
      endOffset = Math.min(colOffset + 1, lineEnd);
    }

    return new TextRange(Math.min(colOffset, endOffset), Math.max(colOffset, endOffset));
  }
}
