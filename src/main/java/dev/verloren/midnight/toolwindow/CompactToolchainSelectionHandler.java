package dev.verloren.midnight.toolwindow;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.Messages;
import dev.verloren.midnight.settings.MidnightProjectSettings;
import dev.verloren.midnight.toolwindow.CompactCompilerPanel.ToolchainItem;
import dev.verloren.midnight.version.CompactVersionManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Handles download confirmation, background installation, and cancellation fallbacks
 * for toolchain items selected in {@link CompactCompilerPanel}.
 */
final class CompactToolchainSelectionHandler {
  private final Project project;
  private final ComboBox<ToolchainItem> versionComboBox;
  private final Runnable onVersionsRefreshed;

  CompactToolchainSelectionHandler(
      @NotNull Project project,
      @NotNull ComboBox<ToolchainItem> versionComboBox,
      @NotNull Runnable onVersionsRefreshed
  ) {
    this.project = project;
    this.versionComboBox = versionComboBox;
    this.onVersionsRefreshed = onVersionsRefreshed;
  }

  void handleSelection(@NotNull ToolchainItem selected, @NotNull SelectionGuard guard) {
    if (selected.isInstalled()) {
      applySelectedVersion(selected.version());
      return;
    }

    int choice = Messages.showYesNoDialog(
        project,
        "Compact compiler v" + selected.version() + " is not installed locally.\n"
            + "Would you like to download and install it now?",
        "Download Compact Compiler",
        "Download",
        "Cancel",
        AllIcons.Actions.Download
    );

    if (choice == Messages.YES) {
      triggerVersionDownload(selected.version(), guard);
    } else {
      revertToInstalledVersion(selected.version(), guard);
    }
  }

  void revertToInstalledVersion(@NotNull String fallbackVersion, @NotNull SelectionGuard guard) {
    String revertTarget = findFirstInstalledVersion();
    if (revertTarget != null) {
      guard.runWithoutEvents(() -> {
        for (int i = 0; i < versionComboBox.getItemCount(); i++) {
          ToolchainItem it = versionComboBox.getItemAt(i);
          if (it != null && it.version().equals(revertTarget)) {
            versionComboBox.setSelectedIndex(i);
            break;
          }
        }
      });
      applySelectedVersion(revertTarget);
    } else {
      applySelectedVersion(fallbackVersion);
    }
  }

  @Nullable String findFirstInstalledVersion() {
    for (int i = 0; i < versionComboBox.getItemCount(); i++) {
      ToolchainItem it = versionComboBox.getItemAt(i);
      if (it != null && it.isInstalled()) {
        return it.version();
      }
    }
    return null;
  }

  void applySelectedVersion(@NotNull String version) {
    MidnightProjectSettings settings = MidnightProjectSettings.getInstance(project);
    if (settings != null) {
      settings.selectedCompilerVersion = version;
    }
  }

  private void triggerVersionDownload(@NotNull String version, @NotNull SelectionGuard guard) {
    ProgressManager.getInstance().run(
        new Task.Backgroundable(project, "Downloading Compact Compiler v" + version, true) {
          @Override
          public void run(@NotNull ProgressIndicator indicator) {
            boolean success = CompactVersionManager.installVersion(version, project.getBasePath(), indicator);
            ApplicationManager.getApplication().invokeLater(() -> {
              if (project.isDisposed()) {
                return;
              }
              if (success) {
                applySelectedVersion(version);
                onVersionsRefreshed.run();
              } else {
                revertToInstalledVersion(version, guard);
              }
            });
          }
        }
    );
  }

  @FunctionalInterface
  interface SelectionGuard {
    void runWithoutEvents(Runnable action);
  }
}
