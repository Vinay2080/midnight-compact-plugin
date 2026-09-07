package dev.verloren.midnight.statusbar;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.CustomStatusBarWidget;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.impl.status.EditorBasedStatusBarPopup;
import dev.verloren.midnight.CompactFileType;
import dev.verloren.midnight.run.CompactToolchainUtil;
import dev.verloren.midnight.settings.MidnightProjectSettings;
import dev.verloren.midnight.version.CompactVersionManager;
import kotlinx.coroutines.CoroutineScope;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Lightweight, non-blocking status bar popup widget displaying the current Compact compiler toolchain.
 */
public class CompactStatusBarWidget extends EditorBasedStatusBarPopup {
  public static final String WIDGET_ID = "CompactStatusBarWidget";

  public record CompactWidgetInfo(@NotNull String text, @NotNull String tooltip) {}

  public CompactStatusBarWidget(@NotNull Project project, @NotNull CoroutineScope scope) {
    super(project, false, scope);
  }

  @Override
  public @NonNls @NotNull String ID() {
    return WIDGET_ID;
  }

  public @NotNull CompactWidgetInfo computeWidgetInfo() {
    MidnightProjectSettings settings = MidnightProjectSettings.getInstance(getProject());
    String selectedVer = settings != null && settings.selectedCompilerVersion != null
        ? settings.selectedCompilerVersion.trim()
        : "";

    String activeVer = CompactToolchainUtil.getActiveCompilerVersion(getProject());
    String effective = !selectedVer.isEmpty() ? selectedVer : (activeVer != null ? activeVer : "");

    String text;
    String tooltip;

    if (!effective.isEmpty()) {
      String langVer = CompactVersionManager.getLanguageVersionForToolchain(effective);
      String versionDisplay = effective.equals(langVer) ? "v" + effective : "v" + effective + " (" + langVer + ")";
      text = "Compact: " + versionDisplay;
      tooltip = "Current Compact compiler: " + versionDisplay + "\nClick to switch version or configure";
    } else {
      text = "Compact: Not Configured";
      tooltip = "Compact compiler is not configured.\nClick to select or download";
    }

    return new CompactWidgetInfo(text, tooltip);
  }

  @Override
  protected @NotNull WidgetState getWidgetState(@Nullable VirtualFile file) {
    CompactWidgetInfo info = computeWidgetInfo();
    return new WidgetState(info.tooltip(), info.text(), true);
  }

  @Override
  protected @NotNull ListPopup createPopup(@NotNull DataContext context) {
    return CompactStatusBarPopup.createPopup(getProject(), context);
  }

  @Override
  protected @NotNull StatusBarWidget createInstance(@NotNull Project project) {
    return new CompactStatusBarWidget(project, getScope());
  }

  @Override
  public boolean isEnabledForFile(@Nullable VirtualFile file) {
    if (file == null) {
      return true;
    }
    return CompactFileType.INSTANCE.getDefaultExtension().equalsIgnoreCase(file.getExtension());
  }
}
