package dev.verloren.midnight.statusbar;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import dev.verloren.midnight.settings.MidnightProjectSettings;
import kotlinx.coroutines.CoroutineScopeKt;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;

public class CompactStatusBarWidgetTest extends BasePlatformTestCase {

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

  public void testFactoryMetadata() {
    CompactStatusBarWidgetFactory factory = new CompactStatusBarWidgetFactory();
    assertEquals("CompactStatusBarWidget", factory.getId());
    assertNotNull(factory.getDisplayName());
    assertTrue(factory.isAvailable(getProject()));
    assertTrue(factory.isConfigurable());
  }

  public void testWidgetInfoUnconfigured() {
    MidnightProjectSettings settings = MidnightProjectSettings.getInstance(getProject());
    settings.selectedCompilerVersion = "";

    CompactStatusBarWidget widget = createTestWidget();
    CompactStatusBarWidget.CompactWidgetInfo info = widget.computeWidgetInfo();

    assertNotNull(info);
    assertTrue(info.text().contains("Not Configured") || info.text().contains("Compact: v"));
  }

  public void testWidgetInfoConfigured() {
    MidnightProjectSettings settings = MidnightProjectSettings.getInstance(getProject());
    settings.selectedCompilerVersion = "0.35.0";

    CompactStatusBarWidget widget = createTestWidget();
    CompactStatusBarWidget.CompactWidgetInfo info = widget.computeWidgetInfo();

    assertNotNull(info);
    assertTrue(info.text().contains("0.35.0"));
    assertTrue(info.tooltip().contains("0.35.0"));
  }

  public void testIsEnabledForFile() {
    CompactStatusBarWidget widget = createTestWidget();
    assertTrue(widget.isEnabledForFile(null));

    VirtualFile compactFile = myFixture.configureByText("test.compact", "pragma language_version >= 0.23;\n").getVirtualFile();
    assertTrue(widget.isEnabledForFile(compactFile));

    VirtualFile txtFile = myFixture.configureByText("test.txt", "hello").getVirtualFile();
    assertFalse(widget.isEnabledForFile(txtFile));
  }

  public void testPopupCreationWithTop5InstalledAndAvailable() {
    dev.verloren.midnight.version.CompactVersionManager.setInstalledVersionsForTesting(
        new LinkedHashMap<>(Map.of("0.35.0", "/dummy/path/compactc", "0.34.0", "/dummy/path/compactc"))
    );

    MidnightProjectSettings settings = MidnightProjectSettings.getInstance(getProject());
    settings.selectedCompilerVersion = "0.35.0";

    ListPopup popup = CompactStatusBarPopup.createPopup(getProject(), DataContext.EMPTY_CONTEXT);
    assertNotNull("Popup must not be null", popup);
  }

  private @NotNull CompactStatusBarWidget createTestWidget() {
    CompactStatusBarWidget widget = new CompactStatusBarWidget(getProject(), CoroutineScopeKt.MainScope());
    Disposer.register(getTestRootDisposable(), widget);
    return widget;
  }
}
