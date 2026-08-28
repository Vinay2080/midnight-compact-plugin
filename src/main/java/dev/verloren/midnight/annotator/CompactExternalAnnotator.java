package dev.verloren.midnight.annotator;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.ExternalAnnotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import dev.verloren.midnight.psi.CompactFile;
import dev.verloren.midnight.settings.MidnightSettingsState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class CompactExternalAnnotator extends ExternalAnnotator<CompactExternalAnnotator.InitialInfo, List<CompactCompilerDiagnostic>> {

  public record InitialInfo(
      @NotNull Project project,
      @NotNull VirtualFile virtualFile,
      @NotNull String compilerPath,
      boolean skipZk
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
    return new InitialInfo(file.getProject(), vFile, compilerPath, skipZk);
  }

  @Override
  public @Nullable List<CompactCompilerDiagnostic> doAnnotate(@NotNull InitialInfo info) {
    try {
      File tempDir = new File(System.getProperty("java.io.tmpdir"), "compact-annotator-output");
      tempDir.mkdirs();

      List<String> args = new ArrayList<>();
      args.add("--vscode");
      if (info.skipZk()) {
        args.add("--skip-zk");
      }
      args.add(info.virtualFile().getPath());
      args.add(tempDir.getAbsolutePath());

      com.intellij.execution.configurations.GeneralCommandLine cmd =
          dev.verloren.midnight.run.CompactToolchainUtil.createCommandLine(
              info.project(),
              args,
              info.project().getBasePath()
          );
      cmd.setRedirectErrorStream(true);

      Process process = cmd.createProcess();
      boolean finished = process.waitFor(5, TimeUnit.SECONDS);
      if (!finished) {
        process.destroyForcibly();
        return null;
      }

      String output = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))
          .lines().collect(Collectors.joining("\n"));

      return CompactCompilerOutputParser.parse(output);
    } catch (Exception ignored) {
      return null;
    }
  }

  @Override
  public void apply(@NotNull PsiFile file, @Nullable List<CompactCompilerDiagnostic> diagnostics, @NotNull AnnotationHolder holder) {
    if (diagnostics == null || diagnostics.isEmpty()) {
      return;
    }

    Document document = PsiDocumentManager.getInstance(file.getProject()).getDocument(file);
    if (document == null) {
      return;
    }

    for (CompactCompilerDiagnostic diagnostic : diagnostics) {
      TextRange range = getRange(diagnostic, document);
      HighlightSeverity severity = diagnostic.isError() ? HighlightSeverity.ERROR : HighlightSeverity.WARNING;

      holder.newAnnotation(severity, diagnostic.message())
          .range(range)
          .create();
    }
  }

  private static @NonNull TextRange getRange(CompactCompilerDiagnostic diagnostic, Document document) {
    int lineIndex = Math.clamp(diagnostic.line() - 1, 0, document.getLineCount() - 1);
    int lineStart = document.getLineStartOffset(lineIndex);
    int lineEnd = document.getLineEndOffset(lineIndex);

    int colOffset = Math.min(lineStart + Math.max(0, diagnostic.column() - 1), lineEnd);
    int endOffset = Math.min(colOffset + 1, lineEnd);
    if (colOffset >= endOffset && lineStart < lineEnd) {
      endOffset = Math.min(colOffset + 1, lineEnd);
    }

    return new TextRange(Math.min(colOffset, endOffset), Math.max(colOffset, endOffset));
  }
}
