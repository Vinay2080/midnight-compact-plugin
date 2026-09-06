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
 * Utility to safely restart code analysis and inspections
 * when compiler versions or pragmas change.
 */
public final class CompactProblemUtil {
  private static final String REASON_FILE_UPDATE = "Compact pragma or file updated";
  private static final String REASON_COMPILER_CHANGED = "Compact compiler version changed";

  private CompactProblemUtil() {}

  public static void clearProblemsAndRestart(@NotNull Project project, @Nullable VirtualFile targetFile) {
    if (project.isDisposed()) return;

    ApplicationManager.getApplication().invokeLater(() -> {
      if (project.isDisposed()) return;

      DaemonCodeAnalyzer daemon = DaemonCodeAnalyzer.getInstance(project);

      // 1. If a specific target file is modified (e.g., pragma updated), only restart that file
      if (targetFile != null && targetFile.isValid()) {
        PsiFile psi = PsiManager.getInstance(project).findFile(targetFile);
        if (psi != null && psi.isValid()) {
          daemon.restart(psi, REASON_FILE_UPDATE);
        }
        return;
      }

      // 2. If the targetFile is null (a global compiler version changed), restart open .compact files
      boolean restartedOpenCompactFile = false;
      for (VirtualFile openFile : FileEditorManager.getInstance(project).getOpenFiles()) {
        if (openFile.isValid() && "compact".equalsIgnoreCase(openFile.getExtension())) {
          PsiFile psi = PsiManager.getInstance(project).findFile(openFile);
          if (psi != null && psi.isValid()) {
            daemon.restart(psi, REASON_COMPILER_CHANGED);
            restartedOpenCompactFile = true;
          }
        }
      }

      // 3. If no open .compact files were restarted, notify the daemon with a project-wide restart reason
      if (!restartedOpenCompactFile) {
        daemon.restart(REASON_COMPILER_CHANGED);
      }
    }, ModalityState.any());
  }
}
