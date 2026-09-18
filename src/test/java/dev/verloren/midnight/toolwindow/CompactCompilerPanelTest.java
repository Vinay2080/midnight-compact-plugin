package dev.verloren.midnight.toolwindow;

import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.SlowOperations;
import com.intellij.util.TimeoutUtil;
import dev.verloren.midnight.annotator.CompactSwitchCompilerQuickFix;
import dev.verloren.midnight.settings.MidnightProjectSettings;

import javax.swing.JPanel;
import java.awt.Component;

public class CompactCompilerPanelTest extends BasePlatformTestCase {

  @SuppressWarnings("UnstableApiUsage")

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

  public void testQuickFixCompilerSwitchReflectedInPanel() {
    myFixture.configureByText("test.compact", "pragma language_version >= 0.18.0;\n");
    MidnightProjectSettings settings = MidnightProjectSettings.getInstance(getProject());
    settings.selectedCompilerVersion = "0.25.0";

    CompactCompilerPanel panel = new CompactCompilerPanel(getProject());
    Disposer.register(getTestRootDisposable(), panel);

    panel.refreshCards();
    waitForCards(panel);

    CompactVersionCard card25 = findCard(panel, "0.25.0");
    CompactVersionCard card26 = findCard(panel, "0.26.0");
    assertNotNull("Card 0.25.0 should be present", card25);
    assertNotNull("Card 0.26.0 should be present", card26);
    assertTrue("Initial selected version 0.25.0 should be active", card25.isActive());
    assertFalse("Version 0.26.0 should not be active initially", card26.isActive());

    // User applies quick-fix suggesting compiler switch to 0.18.0 (toolchain 0.26.0)
    CompactSwitchCompilerQuickFix fix = new CompactSwitchCompilerQuickFix("0.18.0", false);
    fix.invoke(getProject(), myFixture.getEditor(), myFixture.getFile());

    PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
    waitForCards(panel);

    assertEquals("0.26.0", settings.selectedCompilerVersion);

    CompactVersionCard updatedCard26 = findCard(panel, "0.26.0");
    assertNotNull("Card 0.26.0 should exist", updatedCard26);
    assertTrue("Card for switched compiler version 0.26.0 should be active in the tool window panel", updatedCard26.isActive());

    CompactVersionCard updatedCard25 = findCard(panel, "0.25.0");
    assertNotNull("Card 0.25.0 should still exist", updatedCard25);
    assertFalse("Card for old compiler version 0.25.0 should no longer be active", updatedCard25.isActive());
  }

  private void waitForCards(CompactCompilerPanel panel) {
    long deadline = System.currentTimeMillis() + 5000;
    while (System.currentTimeMillis() < deadline) {
      PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
      if (findCard(panel, "0.26.0") != null) {
        return;
      }
      TimeoutUtil.sleep(50);
    }
  }

  private JPanel getCardsPanel(CompactCompilerPanel panel) {
    JPanel versionsContainer = (JPanel) panel.getComponent(1);
    JBScrollPane scrollPane = (JBScrollPane) versionsContainer.getComponent(1);
    return (JPanel) scrollPane.getViewport().getView();
  }

  private CompactVersionCard findCard(CompactCompilerPanel panel, String version) {
    JPanel cardsPanel = getCardsPanel(panel);
    for (Component c : cardsPanel.getComponents()) {
      if (c instanceof CompactVersionCard card && version.equals(card.getVersion())) {
        return card;
      }
    }
    return null;
  }
}
