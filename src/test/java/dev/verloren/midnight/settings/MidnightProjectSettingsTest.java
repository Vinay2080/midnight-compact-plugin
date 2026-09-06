package dev.verloren.midnight.settings;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class MidnightProjectSettingsTest extends BasePlatformTestCase {

  public void testDefaultValues() {
    MidnightProjectSettings settings = new MidnightProjectSettings();
    assertEquals("", settings.selectedCompilerVersion);
    assertEquals("", settings.customCompilerPath);
  }

  public void testStateMutationAndLoad() {
    MidnightProjectSettings original = new MidnightProjectSettings();
    original.selectedCompilerVersion = "0.26.0";
    original.customCompilerPath = "/opt/compactc/bin/compactc";

    MidnightProjectSettings copy = new MidnightProjectSettings();
    copy.loadState(original);

    assertEquals("0.26.0", copy.selectedCompilerVersion);
    assertEquals("/opt/compactc/bin/compactc", copy.customCompilerPath);
  }

  public void testProjectServiceRetrieval() {
    MidnightProjectSettings settings = MidnightProjectSettings.getInstance(getProject());
    assertNotNull(settings);
    assertEquals("", settings.selectedCompilerVersion);
  }
}
