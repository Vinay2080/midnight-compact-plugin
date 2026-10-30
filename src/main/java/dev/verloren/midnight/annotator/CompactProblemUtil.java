package dev.verloren.midnight.annotator;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.codeInsight.intention.preview.IntentionPreviewUtils;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import dev.verloren.midnight.toolwindow.CompactCompilerEventListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utility to safely restart code analysis, daemon highlighting, and inspections
 * when compiler versions, pragma directives, or toolchains change.
 *
 * <h3>IntelliJ Platform Subsystem Role & Calling Pipeline</h3>
 * <p>
 * Invoked by quick-fixes and settings listeners whenever the compiler configuration changes:
 * <ul>
 *   <li><b>Message Bus Notification:</b> Publishes events across {@link CompactCompilerEventListener#TOPIC}
 *       to update status bars and compiler toolwindows.</li>
 *   <li><b>Daemon Analyser Restart:</b> Dispatches {@link DaemonCodeAnalyzer#restart(PsiFile, Object)} or
 *       {@link DaemonCodeAnalyzer#restart(Object)} to wipe stale inspection/external annotator errors
 *       and immediately trigger fresh analysis.</li>
 *   <li><b>Intention Preview Guard:</b> Checks {@link IntentionPreviewUtils#isIntentionPreviewActive()}
 *       to prevent triggering full background daemon restarts when IntelliJ is merely generating
 *       an Alt+Enter intention preview popup.</li>
 * </ul>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Asynchronous Dispatch:</b> Calls are queued to the EDT via {@link ApplicationManager#getApplication()#invokeLater}
 *       with {@link ModalityState#any()}.</li>
 *   <li><b>Read Action Guard:</b> Resolving PSI files from {@link VirtualFile} references is wrapped inside
 *       {@link ReadAction#computeBlocking}.</li>
 * </ul>
 *
 * @see dev.verloren.midnight.annotator.CompactSwitchCompilerQuickFix
 * @see dev.verloren.midnight.annotator.CompactUpdatePragmaQuickFix
 * @see com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
 */
public final class CompactProblemUtil {

  /**
   * Diagnostic reason logged when restarting the daemon due to a pragma update.
   */
  private static final String REASON_FILE_UPDATE = "Compact pragma or file updated";

  /**
   * Diagnostic reason logged when restarting the daemon due to a global compiler switch.
   */
  private static final String REASON_COMPILER_CHANGED = "Compact compiler version changed";

  /**
   * Private constructor preventing external instantiation of this stateless utility class.
   */
  private CompactProblemUtil() {
  }

  /**
   * Clears cached diagnostics and restarts background code analysis for a target file or project-wide.
   *
   * @param project    current active project
   * @param targetFile specific file modified (e.g. pragma update), or {@code null} if global compiler changed
   */
  public static void clearProblemsAndRestart(@NotNull Project project, @Nullable VirtualFile targetFile) {
    // Step 1: Suppress restarts during intention preview generation or if project is already closing
    if (project.isDisposed() || IntentionPreviewUtils.isIntentionPreviewActive()) {
      return;
    }

    // Step 2: Queue daemon restart onto the Event Dispatch Thread
    ApplicationManager.getApplication().invokeLater(() -> {
      if (project.isDisposed()) return;

      try {
        project.getMessageBus().syncPublisher(CompactCompilerEventListener.TOPIC).onCompilerStateChanged();
      } catch (Exception _) {
      }

      DaemonCodeAnalyzer daemon = DaemonCodeAnalyzer.getInstance(project);

      // Step 3: If a specific target file is modified (e.g. pragma updated), restart that file
      if (targetFile != null && targetFile.isValid()) {
        PsiFile psi = ReadAction.computeBlocking(() -> targetFile.isValid() ? PsiManager.getInstance(project).findFile(targetFile) : null);
        if (psi != null && psi.isValid()) {
          daemon.restart(psi, REASON_FILE_UPDATE);
        }
        return;
      }

      // Step 4: If targetFile is null (global compiler changed), restart all currently open .compact files
      boolean restartedOpenCompactFile = false;
      for (VirtualFile openFile : FileEditorManager.getInstance(project).getOpenFiles()) {
        if (openFile.isValid() && "compact".equalsIgnoreCase(openFile.getExtension())) {
          PsiFile psi = ReadAction.computeBlocking(() -> openFile.isValid() ? PsiManager.getInstance(project).findFile(openFile) : null);
          if (psi != null && psi.isValid()) {
            daemon.restart(psi, REASON_COMPILER_CHANGED);
            restartedOpenCompactFile = true;
          }
        }
      }

      // Step 5: Fallback project-wide daemon restart if no .compact files were currently active
      if (!restartedOpenCompactFile) {
        daemon.restart(REASON_COMPILER_CHANGED);
      }
    }, ModalityState.any());
  }
}
