package dev.verloren.midnight.toolwindow;

import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.util.SlowOperations;

public class CompactCompilerPanelTest extends BasePlatformTestCase {

  public void testFindTargetCompactFileProhibitsSlowOperationsOnEdt() {
    CompactCompilerPanel panel = new CompactCompilerPanel(getProject());
    Disposer.register(getTestRootDisposable(), panel);
    try (var _ = SlowOperations.startSection(SlowOperations.ACTION_PERFORM)) {
      VirtualFile file = panel.findTargetCompactFile();
      assertNull(file);
    }
  }

  public void testFindTargetCompactFileFindsOpenEditor() {
    VirtualFile file = myFixture.configureByText("test.compact", "pragma language_version >= 0.23;\n").getVirtualFile();
    CompactCompilerPanel panel = new CompactCompilerPanel(getProject());
    Disposer.register(getTestRootDisposable(), panel);
    VirtualFile target = panel.findTargetCompactFile();
    assertNotNull(target);
    assertEquals(file, target);
  }

  public void testTypingSpaceAfterExportDoesNotFreezeOrThrow() {
    myFixture.configureByText("test.compact", "pragma language_version >= 0.23;\n\nexport<caret>");
    CompactCompilerPanel panel = new CompactCompilerPanel(getProject());
    Disposer.register(getTestRootDisposable(), panel);
    myFixture.type(' ');
    myFixture.checkResult("pragma language_version >= 0.23;\n\nexport <caret>");
    PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
  }
}
