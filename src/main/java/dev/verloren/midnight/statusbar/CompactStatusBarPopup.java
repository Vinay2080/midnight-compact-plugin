package dev.verloren.midnight.statusbar;

import com.intellij.icons.AllIcons;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import dev.verloren.midnight.icons.MidnightIcons;
import dev.verloren.midnight.annotator.CompactProblemUtil;
import dev.verloren.midnight.run.CompactToolchainUtil;
import dev.verloren.midnight.settings.MidnightProjectSettings;
import dev.verloren.midnight.settings.MidnightSettingsConfigurable;
import dev.verloren.midnight.settings.MidnightSettingsState;
import dev.verloren.midnight.version.CompactSemVerUtil;
import dev.verloren.midnight.version.CompactVersionManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Builds the lightweight, zero-latency popup menu for the Compact status bar widget.
 */
public final class CompactStatusBarPopup {

  private CompactStatusBarPopup() {
  }

  public static @NotNull ListPopup createPopup(@NotNull Project project, @NotNull DataContext context) {
    DefaultActionGroup group = new DefaultActionGroup();

    MidnightProjectSettings projectSettings = MidnightProjectSettings.getInstance(project);
    String selectedVersion = projectSettings != null && projectSettings.selectedCompilerVersion != null
        ? projectSettings.selectedCompilerVersion.trim()
        : "";

    String activeVersion = CompactToolchainUtil.getActiveCompilerVersion(project);
    String effectiveActive = !selectedVersion.isEmpty() ? selectedVersion : (activeVersion != null ? activeVersion : "");

    // 1. Header showing active toolchain info
    String headerText = !effectiveActive.isEmpty() ? "Active Compiler: v" + effectiveActive : "Active Compiler: Not Configured";
    group.addSeparator(headerText);

    // 2. Installed Compiler Versions Section
    Map<String, String> installedMap = CompactVersionManager.getInstalledVersions();
    List<String> installedSorted = new ArrayList<>(installedMap.keySet());
    installedSorted.sort((v1, v2) -> {
      CompactSemVerUtil.SemVer s1 = CompactSemVerUtil.parse(v1);
      CompactSemVerUtil.SemVer s2 = CompactSemVerUtil.parse(v2);
      if (s1 != null && s2 != null) {
        return s2.compareTo(s1);
      }
      return v2.compareTo(v1);
    });

    if (!installedSorted.isEmpty()) {
      for (String ver : installedSorted) {
        boolean isCurrent = ver.equals(effectiveActive);
        group.add(new SwitchCompilerVersionAction(project, ver, isCurrent));
      }
    }

    // Auto / Default option if a custom version was selected
    if (!selectedVersion.isEmpty()) {
      group.add(new SwitchCompilerVersionAction(project, "", false));
    }

    // 3. Download Available Versions Section
    List<String> uninstalledVersions = new ArrayList<>();
    for (String known : CompactVersionManager.KNOWN_VERSIONS) {
      if (!installedMap.containsKey(known)) {
        uninstalledVersions.add(known);
      }
    }

    if (!uninstalledVersions.isEmpty()) {
      group.addSeparator("Download Compact Compiler");
      for (String uninstalled : uninstalledVersions) {
        group.add(new DownloadCompilerVersionAction(project, uninstalled));
      }
    }

    // 4. Navigation & Configuration Actions
    group.addSeparator();
    group.add(new OpenCompilerToolWindowAction(project));
    group.add(new OpenMidnightSettingsAction(project));

    return JBPopupFactory.getInstance().createActionGroupPopup(
        "Compact Compiler Toolchain",
        group,
        context,
        JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
        true
    );
  }

  /**
   * Action to switch compiler version with zero perceived latency on EDT.
   */
  public static class SwitchCompilerVersionAction extends DumbAwareAction {
    private final Project project;
    private final String version;
    private final boolean isCurrent;

    public SwitchCompilerVersionAction(@NotNull Project project, @NotNull String version, boolean isCurrent) {
      super(
          version.isEmpty() ? "Use System / Auto-Detected Toolchain" : "Compact v" + version,
          version.isEmpty() ? "Reset to auto-detected toolchain" : "Switch project compiler to Compact v" + version,
          isCurrent ? AllIcons.Actions.Checked : null
      );
      this.project = project;
      this.version = version;
      this.isCurrent = isCurrent;
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
      return ActionUpdateThread.BGT;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      if (isCurrent) return;

      MidnightProjectSettings projectSettings = MidnightProjectSettings.getInstance(project);
      if (projectSettings != null) {
        projectSettings.selectedCompilerVersion = version;
      }

      if (!version.isEmpty()) {
        String installedExe = CompactVersionManager.getInstalledExecutable(version);
        if (installedExe != null) {
          MidnightSettingsState state = MidnightSettingsState.getInstance();
          if (state != null) {
            state.compilerPath = installedExe;
          }
        }
      }

      CompactProblemUtil.clearProblemsAndRestart(project, null);
      CompactStatusBarWidgetFactory.updateWidget(project);

      String msg = version.isEmpty()
          ? "Switched to auto-detected Compact toolchain."
          : "Switched project compiler to Compact v" + version + ".";
      notifyUser(project, msg, NotificationType.INFORMATION);
    }
  }

  /**
   * Action to download and install a known compiler version in a background task.
   */
  public static class DownloadCompilerVersionAction extends DumbAwareAction {
    private final Project project;
    private final String version;

    public DownloadCompilerVersionAction(@NotNull Project project, @NotNull String version) {
      super("Download v" + version + "...", "Download and activate Compact v" + version, AllIcons.Actions.Download);
      this.project = project;
      this.version = version;
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
      return ActionUpdateThread.BGT;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      int confirm = Messages.showYesNoDialog(
          project,
          "Do you want to download and install Compact compiler v" + version + "?",
          "Download Compact Compiler",
          Messages.getQuestionIcon()
      );
      if (confirm != Messages.YES) {
        return;
      }

      ProgressManager.getInstance().run(new Task.Backgroundable(project, "Downloading Compact Compiler v" + version, true) {
        @Override
        public void run(@NotNull ProgressIndicator indicator) {
          boolean ok = CompactVersionManager.installVersion(version, project.getBasePath(), indicator);
          ApplicationManager.getApplication().invokeLater(() -> {
            if (ok) {
              MidnightProjectSettings projectSettings = MidnightProjectSettings.getInstance(project);
              if (projectSettings != null) {
                projectSettings.selectedCompilerVersion = version;
              }
              CompactStatusBarWidgetFactory.updateWidget(project);
              CompactProblemUtil.clearProblemsAndRestart(project, null);
              notifyUser(project, "Compact Compiler v" + version + " installed & activated.", NotificationType.INFORMATION);
            } else {
              notifyUser(project, "Failed to download Compact Compiler v" + version + ". Please check network connection.", NotificationType.ERROR);
            }
          }, ModalityState.any());
        }
      });
    }
  }

  /**
   * Action to open the Remix-style Compact Compiler Tool Window.
   */
  public static class OpenCompilerToolWindowAction extends DumbAwareAction {
    private final Project project;

    public OpenCompilerToolWindowAction(@NotNull Project project) {
      super("Open Compact Compiler Panel", "Show the Compact Compiler sidebar panel", MidnightIcons.SIDEBAR_TOOLWINDOW);
      this.project = project;
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
      return ActionUpdateThread.BGT;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Compact Compiler");
      if (toolWindow != null) {
        toolWindow.show();
      }
    }
  }

  /**
   * Action to open the Midnight plugin settings dialog.
   */
  public static class OpenMidnightSettingsAction extends DumbAwareAction {
    private final Project project;

    public OpenMidnightSettingsAction(@NotNull Project project) {
      super("Configure Midnight Settings...", "Open Midnight Compact settings", AllIcons.General.GearPlain);
      this.project = project;
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
      return ActionUpdateThread.BGT;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      ShowSettingsUtil.getInstance().showSettingsDialog(project, MidnightSettingsConfigurable.class);
    }
  }

  private static void notifyUser(@NotNull Project project, @NotNull String content, @NotNull NotificationType type) {
    NotificationGroup group = NotificationGroupManager.getInstance().getNotificationGroup("Midnight Notifications");
    if (group != null) {
      group.createNotification(content, type).notify(project);
    }
  }
}
