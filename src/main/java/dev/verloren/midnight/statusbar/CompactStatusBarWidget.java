package dev.verloren.midnight.statusbar;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.impl.status.EditorBasedStatusBarPopup;
import dev.verloren.midnight.CompactBundle;
import dev.verloren.midnight.CompactFileType;
import dev.verloren.midnight.run.CompactToolchainUtil;
import dev.verloren.midnight.settings.MidnightProjectSettings;
import dev.verloren.midnight.toolwindow.CompactCompilerEventListener;
import dev.verloren.midnight.version.CompactVersionManager;
import kotlinx.coroutines.CoroutineScope;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Lightweight, non-blocking status bar popup widget displaying the current Compact compiler toolchain.
 *
 * <p><h3>Architectural Overview</h3>
 * Extends IntelliJ's modern {@link EditorBasedStatusBarPopup} to provide an ambient, low-profile indicator
 * on the IDE status bar. It displays:
 * <ul>
 *   <li>The active compiler release version (e.g., {@code "Compact: v0.24.0 (0.2.0)"}) or an unconfigured warning state.</li>
 *   <li>An interactive popup menu (via {@link CompactStatusBarPopup}) on mouse click allowing instant toolchain switching,
 *       downloading missing releases, opening the Remix-style tool window, or jumping directly to plugin settings.</li>
 * </ul>
 *
 * <p><h3>Event Flow &amp; Lifecycle</h3>
 * <pre>
 *   Compiler Switched (ToolWindow / Settings / Popup)
 *                         │
 *                         ▼
 *   MessageBus: CompactCompilerEventListener.TOPIC
 *                         │
 *                         ▼
 *         CompactStatusBarWidget.update()
 *                         │
 *                         ▼
 *             computeWidgetInfo()
 *        (Reads MidnightProjectSettings &amp; CompactToolchainUtil)
 *                         │
 *                         ▼
 *        Returns updated WidgetState (text &amp; tooltip)
 * </pre>
 *
 * <p><h3>File Association &amp; Visibility</h3>
 * Implements {@link #isEnabledForFile(VirtualFile)}, ensuring the widget activates whenever the user
 * edits a {@code .compact} file or when editor context transitions occur.
 *
 * <p><h3>Threading Contract</h3>
 * Extends {@link EditorBasedStatusBarPopup} which operates in conjunction with Kotlin coroutines
 * and executes UI updates on the Event Dispatch Thread (EDT) without blocking syntax analysis or indexing.
 *
 * @see CompactStatusBarWidgetFactory
 * @see CompactStatusBarPopup
 * @see CompactCompilerEventListener
 */
public class CompactStatusBarWidget extends EditorBasedStatusBarPopup {

  /** Unique identifier registered with the IntelliJ status bar widget registry. */
  public static final String WIDGET_ID = "CompactStatusBarWidget";

  /**
   * Immutable data carrier containing computed status bar label text and detailed hover tooltip.
   *
   * @param text    status bar label string (e.g., {@code "Compact: v0.24.0 (0.2.0)"})
   * @param tooltip multi-line descriptive tooltip explaining toolchain and click actions
   */
  public record CompactWidgetInfo(@NotNull String text, @NotNull String tooltip) {}

  /**
   * Constructs the status bar widget, binding to the project message bus to receive toolchain events.
   *
   * @param project active project
   * @param scope   coroutine lifecycle scope supplied by {@link CompactStatusBarWidgetFactory}
   */
  @SuppressWarnings("this-escape")
  public CompactStatusBarWidget(@NotNull Project project, @NotNull CoroutineScope scope) {
    super(project, false, scope);
    project.getMessageBus().connect(this).subscribe(CompactCompilerEventListener.TOPIC, (CompactCompilerEventListener) this::update);
  }

  /**
   * Returns the unique widget ID.
   */
  @Override
  public @NonNls @NotNull String ID() {
    return WIDGET_ID;
  }

  /**
   * Computes the display text and tooltip based on currently configured and active compiler versions.
   *
   * @return populated {@link CompactWidgetInfo}
   */
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
      tooltip = CompactBundle.message("status.bar.compact.tooltip", versionDisplay) + "\nClick to switch version or configure";
    } else {
      text = "Compact: Not Configured";
      tooltip = "Compact compiler is not configured.\nClick to select or download";
    }

    return new CompactWidgetInfo(text, tooltip);
  }

  /**
   * Returns the current widget presentation state (tooltip, label text, and clickability) for the active file.
   *
   * @param file current editor file, or {@code null}
   * @return fresh {@link WidgetState}
   */
  @Override
  protected @NotNull WidgetState getWidgetState(@Nullable VirtualFile file) {
    CompactWidgetInfo info = computeWidgetInfo();
    return new WidgetState(info.tooltip(), info.text(), true);
  }

  /**
   * Creates the dropdown popup menu when the user clicks on the status bar widget.
   *
   * @param context action data context
   * @return interactive {@link ListPopup} built by {@link CompactStatusBarPopup}
   */
  @Override
  protected @Nullable ListPopup createPopup(@NotNull DataContext context) {
    return CompactStatusBarPopup.createPopup(getProject(), context);
  }

  /**
   * Factory method to duplicate the widget for secondary windows or split status bars.
   *
   * @param project the project instance
   * @return new widget instance
   */
  @Override
  protected @NotNull StatusBarWidget createInstance(@NotNull Project project) {
    return new CompactStatusBarWidget(project, getScope());
  }

  /**
   * Returns whether the widget should be active and responsive given the currently focused file.
   *
   * @param file target file in editor, or {@code null}
   * @return true if file is null or has extension {@code .compact}
   */
  @Override
  public boolean isEnabledForFile(@Nullable VirtualFile file) {
    if (file == null) {
      return true;
    }
    return CompactFileType.INSTANCE.getDefaultExtension().equalsIgnoreCase(file.getExtension());
  }
}
