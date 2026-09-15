package dev.verloren.midnight.statusbar;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.StatusBarWidgetFactory;
import com.intellij.openapi.wm.impl.status.widget.StatusBarWidgetsManager;
import kotlinx.coroutines.CoroutineScope;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

/**
 * Factory for creating and managing the Compact compiler status bar widget.
 */
public class CompactStatusBarWidgetFactory implements StatusBarWidgetFactory {

  @Override
  public @NonNls @NotNull String getId() {
    return CompactStatusBarWidget.WIDGET_ID;
  }

  @Override
  public @Nls @NotNull String getDisplayName() {
    return "Midnight Compact Toolchain";
  }

  @Override
  @SuppressWarnings("RedundantMethodOverride")
  public boolean isAvailable(@NotNull Project project) {
    return true;
  }

  @Override
  @SuppressWarnings("RedundantMethodOverride")
  public boolean canBeEnabledOn(@NotNull StatusBar statusBar) {
    return true;
  }

  @Override
  public @NotNull StatusBarWidget createWidget(@NotNull Project project, @NotNull CoroutineScope scope) {
    return new CompactStatusBarWidget(project, scope);
  }

  @Override
  @SuppressWarnings("RedundantMethodOverride")
  public boolean isConfigurable() {
    return true;
  }

  /**
   * Triggers an update of the widget across open status bars.
   */
  @SuppressWarnings("IncorrectServiceRetrieving")
  public static void updateWidget(@NotNull Project project) {
    if (project.isDisposed()) return;
    StatusBarWidgetsManager manager = project.getService(StatusBarWidgetsManager.class);
    if (manager != null) {
      manager.updateWidget(CompactStatusBarWidgetFactory.class);
    }
  }
}
