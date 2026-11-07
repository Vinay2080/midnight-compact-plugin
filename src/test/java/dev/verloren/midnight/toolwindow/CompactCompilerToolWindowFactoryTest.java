package dev.verloren.midnight.toolwindow;

import com.intellij.openapi.wm.RegisterToolWindowTask;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.content.Content;

public class CompactCompilerToolWindowFactoryTest extends BasePlatformTestCase {

  @SuppressWarnings("OverrideOnly")
  public void testCreateToolWindowContentWrapsInJBScrollPane() {
    ToolWindowManager manager = ToolWindowManager.getInstance(getProject());
    ToolWindow toolWindow = manager.registerToolWindow(
        RegisterToolWindowTask.closable("Compact Compiler Test", dev.verloren.midnight.icons.MidnightIcons.FILE)
    );

    CompactCompilerToolWindowFactory factory = new CompactCompilerToolWindowFactory();
    factory.createToolWindowContent(getProject(), toolWindow);

    assertEquals(1, toolWindow.getContentManager().getContentCount());
    Content content = toolWindow.getContentManager().getContent(0);
    assertNotNull(content);

    assertTrue("Tool window content component must be JBScrollPane for 2-axis fluidity",
        content.getComponent() instanceof JBScrollPane);

    JBScrollPane scrollPane = (JBScrollPane) content.getComponent();
    Object view = scrollPane.getViewport().getView();
    assertTrue("Viewport view must be CompactCompilerPanel",
        view instanceof CompactCompilerPanel);

    CompactCompilerPanel panel = (CompactCompilerPanel) view;
    assertTrue("Panel must track viewport width for horizontal responsiveness",
        panel.getScrollableTracksViewportWidth());
    assertFalse("Panel must not track viewport height to allow vertical scrolling",
        panel.getScrollableTracksViewportHeight());
  }
}
