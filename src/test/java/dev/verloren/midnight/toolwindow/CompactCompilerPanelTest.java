package dev.verloren.midnight.toolwindow;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.util.SlowOperations;

public class CompactCompilerPanelTest extends BasePlatformTestCase {

  public void testFindTargetCompactFileProhibitsSlowOperationsOnEdt() {
    CompactCompilerPanel panel = new CompactCompilerPanel(getProject());
    try {
      try (var _ = SlowOperations.startSection(SlowOperations.ACTION_PERFORM)) {
        VirtualFile file = panel.findTargetCompactFile();
        assertNull(file);
      }
    } finally {
      panel.dispose();
    }
  }

  public void testFindTargetCompactFileFindsOpenEditor() {
    VirtualFile file = myFixture.configureByText("test.compact", "pragma language_version >= 0.23;\n").getVirtualFile();
    CompactCompilerPanel panel = new CompactCompilerPanel(getProject());
    try {
      VirtualFile target = panel.findTargetCompactFile();
      assertNotNull(target);
      assertEquals(file, target);
    } finally {
      panel.dispose();
    }
  }

  public void testTypingSpaceAfterExportDoesNotFreezeOrThrow() {
    myFixture.configureByText("test.compact", "pragma language_version >= 0.23;\n\nexport<caret>");
    CompactCompilerPanel panel = new CompactCompilerPanel(getProject());
    try {
      myFixture.type(' ');
      myFixture.checkResult("pragma language_version >= 0.23;\n\nexport <caret>");
    } finally {
      panel.dispose();
    }
  }
}
