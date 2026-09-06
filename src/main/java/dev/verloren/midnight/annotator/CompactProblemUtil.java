package dev.verloren.midnight.annotator;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utility to immediately restart code analysis and inspections
 * when compiler versions or pragmas change.
 */
public final class CompactProblemUtil {
  private CompactProblemUtil() {}

  @SuppressWarnings("deprecation")
  public static void clearProblemsAndRestart(@NotNull Project project, @Nullable VirtualFile targetFile) {
    if (project.isDisposed()) return;

    ApplicationManager.getApplication().invokeLater(() -> {
      if (project.isDisposed()) return;

      if (targetFile != null && targetFile.isValid()) {
        PsiFile psi = PsiManager.getInstance(project).findFile(targetFile);
        if (psi != null) {
          DaemonCodeAnalyzer.getInstance(project).restart(psi);
        }
      }

      // Restart daemon for all open .compact files
      for (VirtualFile openFile : FileEditorManager.getInstance(project).getOpenFiles()) {
        if (openFile.isValid() && "compact".equalsIgnoreCase(openFile.getExtension())) {
          PsiFile psi = PsiManager.getInstance(project).findFile(openFile);
          if (psi != null) {
            DaemonCodeAnalyzer.getInstance(project).restart(psi);
          }
        }
      }

      DaemonCodeAnalyzer.getInstance(project).restart();
    }, ModalityState.any());
  }
}
