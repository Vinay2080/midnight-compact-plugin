package dev.verloren.midnight.statusbar;

import com.intellij.icons.AllIcons;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
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
import dev.verloren.midnight.CompactBundle;
import dev.verloren.midnight.annotator.CompactProblemUtil;
import dev.verloren.midnight.run.CompactToolchainUtil;
import dev.verloren.midnight.settings.MidnightProjectSettings;
import dev.verloren.midnight.settings.MidnightSettingsConfigurable;
import dev.verloren.midnight.settings.MidnightSettingsState;
import dev.verloren.midnight.toolwindow.CompactCompilerRunner;
import dev.verloren.midnight.version.CompactSemVerUtil;
import dev.verloren.midnight.version.CompactVersionManager;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Creates the curated speed-search action popup for the Compact status bar widget.
 */
public final class CompactStatusBarPopup {

  private CompactStatusBarPopup() {}

  /**
   * Constructs the action group popup containing active, top 5 installed, and top 5 available versions.
   */
  public static @NotNull ListPopup createPopup(@NotNull Project project, @NotNull DataContext context) {
    DefaultActionGroup group = new DefaultActionGroup();

    String effectiveActive = resolveEffectiveActiveVersion(project);
    Map<String, String> installedMap = CompactVersionManager.getInstalledVersions();

    addInstalledSection(group, project, installedMap, effectiveActive);
    addDownloadSection(group, project, installedMap);
    addShortcutsSection(group, project);

    return JBPopupFactory.getInstance().createActionGroupPopup(
        CompactBundle.message("status.bar.compact.popup.title"),
        group,
        context,
        JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
        true
    );
  }

  private static @NotNull String resolveEffectiveActiveVersion(@NotNull Project project) {
    MidnightProjectSettings settings = MidnightProjectSettings.getInstance(project);
    String selected = settings != null && settings.selectedCompilerVersion != null
        ? settings.selectedCompilerVersion.trim()
        : "";
    String active = CompactToolchainUtil.getActiveCompilerVersion(project);
    return !selected.isEmpty() ? selected : (active != null ? active : "");
  }

  private static void addInstalledSection(
      @NotNull DefaultActionGroup group,
      @NotNull Project project,
      @NotNull Map<String, String> installedMap,
      @NotNull String effectiveActive
  ) {
    List<String> installedSorted = new ArrayList<>(installedMap.keySet());
    installedSorted.sort(CompactSemVerUtil.DESCENDING_COMPARATOR);

    group.addSeparator("Installed Compilers (Top 5)");
    int count = 0;
    for (String ver : installedSorted) {
      if (count++ >= 5) break;
      boolean isCurrent = ver.equals(effectiveActive);
      group.add(new SwitchCompilerVersionAction(project, ver, isCurrent));
      if (!isCurrent) {
        group.add(new UninstallCompilerVersionAction(project, ver));
      }
    }
  }

  private static void addDownloadSection(
      @NotNull DefaultActionGroup group,
      @NotNull Project project,
      @NotNull Map<String, String> installedMap
  ) {
    List<String> uninstalled = new ArrayList<>();
    for (String known : CompactVersionManager.KNOWN_VERSIONS) {
      if (!installedMap.containsKey(known)) {
        uninstalled.add(known);
      }
    }
    uninstalled.sort(CompactSemVerUtil.DESCENDING_COMPARATOR);

    if (!uninstalled.isEmpty()) {
      group.addSeparator("Available Cloud Releases (Top 5)");
      int count = 0;
      for (String ver : uninstalled) {
        if (count++ >= 5) break;
        group.add(new DownloadCompilerVersionAction(project, ver));
      }
    }
  }

  private static void addShortcutsSection(@NotNull DefaultActionGroup group, @NotNull Project project) {
    group.addSeparator();
    group.add(new OpenCompilerToolWindowAction(project));
    group.add(new OpenMidnightSettingsAction(project));
  }

  /**
   * Action to switch compiler version for the project.
   */
  public static class SwitchCompilerVersionAction extends DumbAwareAction {
    private final Project project;
    private final String version;
    private final boolean isCurrent;

    public SwitchCompilerVersionAction(@NotNull Project project, @NotNull String version, boolean isCurrent) {
      super(
          "Compact v" + version + (isCurrent ? " (Active)" : ""),
          "Switch project compiler to Compact v" + version,
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

      String installedExe = CompactVersionManager.getInstalledExecutable(version);
      if (installedExe != null) {
        MidnightSettingsState state = MidnightSettingsState.getInstance();
        if (state != null) {
          state.compilerPath = installedExe;
        }
      }

      CompactProblemUtil.clearProblemsAndRestart(project, null);
      CompactStatusBarWidgetFactory.updateWidget(project);
      CompactCompilerRunner.notifyUser(project, "Switched project compiler to Compact v" + version + ".", NotificationType.INFORMATION);
    }
  }

  /**
   * Action to uninstall and remove an unselected compiler version from local storage.
   */
  public static class UninstallCompilerVersionAction extends DumbAwareAction {
    private final Project project;
    private final String version;

    public UninstallCompilerVersionAction(@NotNull Project project, @NotNull String version) {
      super("  Uninstall v" + version + "...", "Uninstall and remove Compact v" + version, AllIcons.Actions.GC);
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
          "Are you sure you want to uninstall Compact compiler v" + version + "?",
          "Uninstall Compact Compiler",
          "Uninstall",
          "Cancel",
          Messages.getWarningIcon()
      );
      if (confirm != Messages.YES) {
        return;
      }

      CompactVersionManager.uninstallVersion(version);
      CompactStatusBarWidgetFactory.updateWidget(project);
      CompactCompilerRunner.notifyUser(project, "Compact Compiler v" + version + " has been uninstalled.", NotificationType.INFORMATION);
    }
  }

  /**
   * Action to download and activate a compiler version.
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
          "Compact compiler v" + version + " is not installed locally. Download now?",
          "Download Compact Compiler",
          "Download",
          "Cancel",
          AllIcons.Actions.Download
      );
      if (confirm != Messages.YES) {
        return;
      }

      ProgressManager.getInstance().run(new Task.Backgroundable(project, "Downloading Compact Compiler v" + version, true) {
        @Override
        public void run(@NotNull ProgressIndicator indicator) {
          boolean ok = CompactVersionManager.installVersion(version, project.getBasePath(), indicator);
          ApplicationManager.getApplication().invokeLater(() -> {
            if (project.isDisposed()) return;
            if (ok) {
              MidnightProjectSettings settings = MidnightProjectSettings.getInstance(project);
              if (settings != null) {
                settings.selectedCompilerVersion = version;
              }
              CompactStatusBarWidgetFactory.updateWidget(project);
              CompactProblemUtil.clearProblemsAndRestart(project, null);
              CompactCompilerRunner.notifyUser(project, "Compact Compiler v" + version + " installed & activated.", NotificationType.INFORMATION);
            } else {
              CompactCompilerRunner.notifyUser(project, "Failed to download Compact Compiler v" + version + ".", NotificationType.ERROR);
            }
          });
        }
      });
    }
  }

  /**
   * Action to show the Compact Compiler tool window.
   */
  public static class OpenCompilerToolWindowAction extends DumbAwareAction {
    private final Project project;

    public OpenCompilerToolWindowAction(@NotNull Project project) {
      super("Open Compact Compiler Tool Window", "Show compact compiler panel", AllIcons.Toolwindows.ToolWindowRun);
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
   * Action to open the Midnight settings dialog.
   */
  public static class OpenMidnightSettingsAction extends DumbAwareAction {
    private final Project project;

    public OpenMidnightSettingsAction(@NotNull Project project) {
      super("Configure All Toolchains in Settings...", "Open Midnight settings", AllIcons.General.GearPlain);
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
}
