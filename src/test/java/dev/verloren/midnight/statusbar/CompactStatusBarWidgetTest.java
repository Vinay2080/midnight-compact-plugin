package dev.verloren.midnight.statusbar;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.testFramework.TestActionEvent;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import dev.verloren.midnight.settings.MidnightProjectSettings;
import kotlinx.coroutines.GlobalScope;

public class CompactStatusBarWidgetTest extends BasePlatformTestCase {

  public void testFactoryCreationAndAvailability() {
    CompactStatusBarWidgetFactory factory = new CompactStatusBarWidgetFactory();
    assertEquals("CompactStatusBarWidget", factory.getId());
    assertNotNull(factory.getDisplayName());
    assertTrue(factory.isAvailable(getProject()));
    StatusBar statusBar = WindowManager.getInstance().getStatusBar(getProject());
    if (statusBar != null) {
      assertTrue(factory.canBeEnabledOn(statusBar));
    }
    assertTrue(factory.isConfigurable());

    StatusBarWidget widget = factory.createWidget(getProject(), GlobalScope.INSTANCE);
    assertNotNull(widget);
    assertEquals("CompactStatusBarWidget", widget.ID());
  }

  public void testWidgetStatePresentation() {
    CompactStatusBarWidget widget = new CompactStatusBarWidget(getProject(), GlobalScope.INSTANCE);

    // Initial state without custom compiler selection
    MidnightProjectSettings settings = MidnightProjectSettings.getInstance(getProject());
    settings.selectedCompilerVersion = "";

    CompactStatusBarWidget.CompactWidgetInfo info = widget.computeWidgetInfo();
    assertNotNull(info);
    assertNotNull(info.text());
    assertTrue(info.text().startsWith("Compact:"));

    // Set custom selected compiler version
    settings.selectedCompilerVersion = "0.34.0";
    CompactStatusBarWidget.CompactWidgetInfo configuredInfo = widget.computeWidgetInfo();
    assertNotNull(configuredInfo);
    assertEquals("Compact: v0.34.0 (0.26.0)", configuredInfo.text());
    assertTrue(configuredInfo.tooltip().contains("0.34.0"));
  }

  public void testWidgetEnabledForFile() {
    CompactStatusBarWidget widget = new CompactStatusBarWidget(getProject(), GlobalScope.INSTANCE);

    // Null file (general state)
    assertTrue(widget.isEnabledForFile(null));

    // Compact file
    VirtualFile compactFile = myFixture.configureByText("Test.compact", "pragma language_version >= 0.26.0;").getVirtualFile();
    assertTrue(widget.isEnabledForFile(compactFile));

    // Non-compact file
    VirtualFile txtFile = myFixture.configureByText("Test.txt", "hello").getVirtualFile();
    assertFalse(widget.isEnabledForFile(txtFile));
  }

  public void testPopupCreation() {
    MidnightProjectSettings settings = MidnightProjectSettings.getInstance(getProject());
    settings.selectedCompilerVersion = "0.34.0";

    DataContext context = DataContext.EMPTY_CONTEXT;
    ListPopup popup = CompactStatusBarPopup.createPopup(getProject(), context);
    assertNotNull(popup);
    assertNotNull(popup.getListStep());
    assertEquals("Compact Compiler Toolchain", popup.getListStep().getTitle());
  }

  public void testSwitchCompilerVersionAction() {
    MidnightProjectSettings settings = MidnightProjectSettings.getInstance(getProject());
    settings.selectedCompilerVersion = "0.26.0";

    CompactStatusBarPopup.SwitchCompilerVersionAction action =
        new CompactStatusBarPopup.SwitchCompilerVersionAction(getProject(), "0.34.0", false);

    action.actionPerformed(TestActionEvent.createTestEvent(action));
    // Directly invoking action logic updates settings
    assertEquals("0.34.0", settings.selectedCompilerVersion);
  }
}
