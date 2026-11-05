package dev.verloren.midnight.toolwindow;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.util.TimeoutUtil;
import dev.verloren.midnight.settings.MidnightProjectSettings;

public class CompactCompilerPanelTest extends BasePlatformTestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    dev.verloren.midnight.version.CompactVersionManager.setInstalledVersionsForTesting(null);
  }

  @Override
  protected void tearDown() throws Exception {
    dev.verloren.midnight.version.CompactVersionManager.setInstalledVersionsForTesting(null);
    super.tearDown();
  }

  public void testFindTargetCompactFileOnEdt() {
    CompactCompilerPanel panel = new CompactCompilerPanel(getProject());
    Disposer.register(getTestRootDisposable(), panel);
    VirtualFile file = panel.findTargetCompactFile();
    assertNull(file);
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

  public void testDropdownPopulatesKnownVersionsAndSyncsSelection() {
    MidnightProjectSettings settings = MidnightProjectSettings.getInstance(getProject());
    settings.selectedCompilerVersion = "0.35.0";

    CompactCompilerPanel panel = new CompactCompilerPanel(getProject());
    Disposer.register(getTestRootDisposable(), panel);

    panel.refreshVersions();
    waitForVersions(panel, "0.35.0");

    ComboBox<CompactCompilerPanel.ToolchainItem> combo = panel.getVersionComboBox();
    assertNotNull("ComboBox should not be null", combo);
    assertTrue("ComboBox should contain items", combo.getItemCount() > 0);

    CompactCompilerPanel.ToolchainItem selected = (CompactCompilerPanel.ToolchainItem) combo.getSelectedItem();
    assertNotNull("Selected item should not be null", selected);
    assertEquals("0.35.0", selected.version());
    assertTrue("Display string should mention Ledger 9", selected.display().contains("Midnight Ledger 9"));
  }

  public void testAdvancedOptionsToggleExpandsAndCollapses() {
    CompactCompilerPanel panel = new CompactCompilerPanel(getProject());
    Disposer.register(getTestRootDisposable(), panel);

    assertFalse("Advanced options should be collapsed initially", panel.isAdvancedExpanded());
    panel.setAdvancedExpanded(true);
    assertTrue("Advanced options should expand", panel.isAdvancedExpanded());
    assertTrue("Skip ZK should be enabled by default", panel.isSkipZkSelected());
    assertEquals("gen", panel.getOutputDirectory());

    panel.setAdvancedExpanded(false);
    assertFalse("Advanced options should collapse", panel.isAdvancedExpanded());
  }

  private void waitForVersions(CompactCompilerPanel panel, String expectedVersion) {
    long deadline = System.currentTimeMillis() + 5000;
    while (System.currentTimeMillis() < deadline) {
      PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
      ComboBox<CompactCompilerPanel.ToolchainItem> combo = panel.getVersionComboBox();
      for (int i = 0; i < combo.getItemCount(); i++) {
        CompactCompilerPanel.ToolchainItem item = combo.getItemAt(i);
        if (item != null && expectedVersion.equals(item.version())) {
          return;
        }
      }
      TimeoutUtil.sleep(50);
    }
  }
}
