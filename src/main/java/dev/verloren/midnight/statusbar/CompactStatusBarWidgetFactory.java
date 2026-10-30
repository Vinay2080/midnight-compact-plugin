package dev.verloren.midnight.statusbar;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.StatusBarWidgetFactory;
import com.intellij.openapi.wm.impl.status.widget.StatusBarWidgetsManager;
import dev.verloren.midnight.CompactBundle;
import kotlinx.coroutines.CoroutineScope;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

/**
 * Factory for creating and managing the Compact compiler status bar widget.
 *
 * <p><h3>Architectural Role &amp; Extension Point</h3>
 * Registered as an extension to {@code com.intellij.statusBarWidgetFactory}.
 * It controls the lifecycle, availability, and instantiation of the status bar indicator
 * {@link CompactStatusBarWidget} located at the bottom-right of the IntelliJ IDEA IDE frame.
 *
 * <p><h3>Lifecycle &amp; Updates</h3>
 * <pre>
 *   IDE Startup / Project Open
 *              │
 *              ▼
 *   createWidget(Project, CoroutineScope)
 *              │
 *              ▼
 *   Instantiate CompactStatusBarWidget(project, scope)
 *              │
 *              ▼ (On compiler switch / version download)
 *   CompactStatusBarWidgetFactory.updateWidget(project)
 *              │
 *              ▼
 *   StatusBarWidgetsManager.updateWidget(CompactStatusBarWidgetFactory.class)
 * </pre>
 *
 * <p><h3>Threading Contract</h3>
 * Invoked by IntelliJ platform widget management infrastructure on the Event Dispatch Thread (EDT).
 * Provides the static utility {@link #updateWidget(Project)} which safely triggers an asynchronous
 * recalculation of the widget state across all open windows.
 *
 * @see CompactStatusBarWidget
 * @see CompactStatusBarPopup
 */
public class CompactStatusBarWidgetFactory implements StatusBarWidgetFactory {

  /**
   * Returns the unique string identifier of this widget, matching {@link CompactStatusBarWidget#WIDGET_ID}.
   */
  @Override
  public @NonNls @NotNull String getId() {
    return CompactStatusBarWidget.WIDGET_ID;
  }

  /**
   * Returns the user-facing display name shown in IDE status bar settings and context menus.
   */
  @Override
  public @Nls @NotNull String getDisplayName() {
    return CompactBundle.message("status.bar.compact.display.name");
  }

  /**
   * Evaluates whether the widget should be available for the given project.
   * Always returns {@code true} so Compact toolchain details are accessible whenever the plugin is active.
   *
   * @param project the current project
   * @return true
   */
  @Override
  public boolean isAvailable(@NotNull Project project) {
    return true;
  }

  /**
   * Determines whether the widget can be placed on the specified status bar instance.
   *
   * @param statusBar the target status bar
   * @return true
   */
  @Override
  public boolean canBeEnabledOn(@NotNull StatusBar statusBar) {
    return true;
  }

  /**
   * Instantiates a new {@link CompactStatusBarWidget} instance bound to the project's coroutine scope.
   *
   * @param project the active project
   * @param scope   lifecycle coroutine scope supplied by the status bar manager
   * @return new widget instance
   */
  @Override
  public @NotNull StatusBarWidget createWidget(@NotNull Project project, @NotNull CoroutineScope scope) {
    return new CompactStatusBarWidget(project, scope);
  }

  /**
   * Disposes the given widget instance when removed from the status bar.
   *
   * @param widget the widget to dispose
   */
  @Override
  public void disposeWidget(@NotNull StatusBarWidget widget) {
    widget.dispose();
  }

  /**
   * Indicates whether the user can toggle widget visibility through IDE status bar settings.
   *
   * @return true
   */
  @Override
  public boolean isConfigurable() {
    return true;
  }

  /**
   * Triggers an asynchronous update of the widget across open status bars via {@link StatusBarWidgetsManager}.
   *
   * @param project the target project whose status bar widget should refresh
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
