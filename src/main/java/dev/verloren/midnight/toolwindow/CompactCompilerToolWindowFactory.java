package dev.verloren.midnight.toolwindow;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

/**
 * Factory for creating and registering the Remix IDE-style "Compact Compiler" tool window on the IDE sidebar.
 *
 * <p><h3>Architectural Role &amp; Extension Point</h3>
 * Registered as an extension to {@code com.intellij.toolWindow} with {@code id="Compact Compiler"} and
 * {@code anchor="right"}. This tool window serves as the primary visual hub for Midnight Compact developers,
 * inspired by the Ethereum Remix IDE Solidity compiler panel. It integrates:
 * <ul>
 *   <li><b>Live Contract Context:</b> Displays the currently focused or active {@code .compact} file and its pragma version declaration.</li>
 *   <li><b>Compilation Execution:</b> Provides single-click contract compilation with customizable flags (e.g., {@code --skip-zk}).</li>
 *   <li><b>Compiler Toolchain Manager:</b> Displays installed and available Compact compiler releases, allowing seamless download, activation, and removal.</li>
 * </ul>
 *
 * <p><h3>Lifecycle &amp; Resource Management</h3>
 * Implements {@link ToolWindowFactory} and {@link DumbAware}. Because it is marked as dumb-aware, the tool window
 * is available immediately upon project initialization even while indexes are being built.
 * <pre>
 *   IDE Startup / Window Open
 *              │
 *              ▼
 *   createToolWindowContent(Project, ToolWindow)
 *              │
 *              ├─► Instantiate CompactCompilerPanel(project)
 *              ├─► Create Content wrapper via ContentFactory.getInstance()
 *              ├─► Register Disposable hierarchy: Disposer.register(content, panel)
 *              └─► Register Content with ToolWindow ContentManager
 * </pre>
 *
 * <p><h3>Threading Contract</h3>
 * Invoked by the IntelliJ Platform on the Event Dispatch Thread (EDT) when the tool window is first displayed.
 * Disposer hierarchy guarantees that UI resources and timers (such as {@link com.intellij.util.Alarm} debouncers)
 * inside {@link CompactCompilerPanel} are cleanly disposed when the tool window content is closed.
 *
 * @see CompactCompilerPanel
 * @see CompactCompilerEventListener
 * @see CompactVersionCard
 */
public class CompactCompilerToolWindowFactory implements ToolWindowFactory, DumbAware {

  /**
   * Initializes and populates the contents of the "Compact Compiler" tool window.
   *
   * <p>Instantiates a new {@link CompactCompilerPanel}, wraps it in an IntelliJ {@link Content} instance,
   * ties the panel's lifecycle to the content via {@link Disposer#register(Disposable, Disposable)},
   * and registers the content with the tool window's {@link com.intellij.ui.content.ContentManager}.
   *
   * @param project    the active IntelliJ project
   * @param toolWindow the tool window instance managed by the IDE platform
   */
  @Override
  public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
    CompactCompilerPanel panel = new CompactCompilerPanel(project);
    Content content = ContentFactory.getInstance().createContent(panel, "", false);
    Disposer.register(content, panel);
    toolWindow.getContentManager().addContent(content);
  }
}
