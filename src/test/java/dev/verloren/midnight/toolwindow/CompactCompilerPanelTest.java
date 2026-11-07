package dev.verloren.midnight.toolwindow;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.util.TimeoutUtil;
import dev.verloren.midnight.settings.MidnightProjectSettings;

import javax.swing.JButton;
import javax.swing.JLabel;
import java.awt.Component;
import java.awt.Container;

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

  public void testDropdownPopulatesMultipleVersions() {
    CompactCompilerPanel panel = new CompactCompilerPanel(getProject());
    Disposer.register(getTestRootDisposable(), panel);

    panel.refreshVersions();
    waitForVersions(panel, "0.34.0");

    ComboBox<CompactCompilerPanel.ToolchainItem> combo = panel.getVersionComboBox();
    assertTrue("ComboBox should contain 0.34.0", combo.getItemCount() > 1);
  }

  public void testDropdownItemTracksInstallationStatus() {
    dev.verloren.midnight.version.CompactVersionManager.setInstalledVersionsForTesting(
            new java.util.LinkedHashMap<>(java.util.Map.of("0.35.0", "/dummy/path/compactc"))
    );

    CompactCompilerPanel panel = new CompactCompilerPanel(getProject());
    Disposer.register(getTestRootDisposable(), panel);

    panel.refreshVersions();
    waitForVersions(panel, "0.35.0");

    ComboBox<CompactCompilerPanel.ToolchainItem> combo = panel.getVersionComboBox();
    CompactCompilerPanel.ToolchainItem item35 = null;
    CompactCompilerPanel.ToolchainItem item34 = null;
    for (int i = 0; i < combo.getItemCount(); i++) {
      CompactCompilerPanel.ToolchainItem it = combo.getItemAt(i);
      if (it != null && "0.35.0".equals(it.version())) {
        item35 = it;
      }
      if (it != null && "0.34.0".equals(it.version())) {
        item34 = it;
      }
    }

    assertNotNull("0.35.0 must be in dropdown", item35);
    assertTrue("0.35.0 should be marked as installed", item35.isInstalled());

    assertNotNull("0.34.0 must be in dropdown", item34);
    assertFalse("0.34.0 should not be marked as installed", item34.isInstalled());
  }

  public void testRevertToInstalledVersionFallback() {
    dev.verloren.midnight.version.CompactVersionManager.setInstalledVersionsForTesting(
            new java.util.LinkedHashMap<>(java.util.Map.of("0.35.0", "/dummy/path/compactc"))
    );

    CompactCompilerPanel panel = new CompactCompilerPanel(getProject());
    Disposer.register(getTestRootDisposable(), panel);

    panel.refreshVersions();
    waitForVersions(panel, "0.35.0");

    CompactToolchainSelectionHandler handler = new CompactToolchainSelectionHandler(
            getProject(),
            panel.getVersionComboBox(),
            panel::refreshVersions
    );

    handler.revertToInstalledVersion("0.34.0", Runnable::run);

    CompactCompilerPanel.ToolchainItem selected =
            (CompactCompilerPanel.ToolchainItem) panel.getVersionComboBox().getSelectedItem();
    assertNotNull(selected);
    assertEquals("0.35.0", selected.version());
    assertEquals("0.35.0", MidnightProjectSettings.getInstance(getProject()).selectedCompilerVersion);
  }

  public void testPanelContainsNoRunButtonOrAdvancedOptions() {
    CompactCompilerPanel panel = new CompactCompilerPanel(getProject());
    Disposer.register(getTestRootDisposable(), panel);

    assertNoButtonOrAdvancedControls(panel);
  }

  private void assertNoButtonOrAdvancedControls(Container container) {
    for (Component comp : container.getComponents()) {
      // JComboBox contains an internal arrow button (BasicArrowButton extends JButton)
      if (comp instanceof JButton btn && !(container instanceof ComboBox<?>)) {
        String text = btn.getText();
        assertFalse("Tool window must not contain Run Contract button: " + text,
                text != null && text.contains("Run"));
        assertNull("Tool window must not contain an action button outside JComboBox", btn.getAction());
      }
      if (comp instanceof JLabel label) {
        String text = label.getText();
        if (text != null) {
          assertFalse("Tool window must not contain Advanced Options label",
                  text.contains("Advanced Options"));
          assertFalse("Tool window must not contain Run Contract label",
                  text.contains("Run Contract"));
        }
      }
      if (comp instanceof Container childContainer && !(childContainer instanceof ComboBox<?>)) {
        assertNoButtonOrAdvancedControls(childContainer);
      }
    }
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
