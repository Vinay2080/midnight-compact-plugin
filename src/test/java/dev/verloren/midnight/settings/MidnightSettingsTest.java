package dev.verloren.midnight.settings;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class MidnightSettingsTest extends BasePlatformTestCase {

  public void testDefaultSettingsStateValues() {
    MidnightSettingsState state = new MidnightSettingsState();
    assertEquals("", state.compilerPath);
    assertEquals("gen", state.defaultOutputDir);
    assertTrue(state.skipZkDefault);
    assertEquals("http://localhost:9944", state.devnetRpcUrl);
  }

  public void testStateMutationAndLoadState() {
    MidnightSettingsState original = new MidnightSettingsState();
    original.compilerPath = "/usr/local/bin/compactc";
    original.defaultOutputDir = "dist/gen";
    original.skipZkDefault = false;
    original.devnetRpcUrl = "https://rpc.testnet.midnight.network";

    MidnightSettingsState copy = new MidnightSettingsState();
    copy.loadState(original);

    assertEquals("/usr/local/bin/compactc", copy.compilerPath);
    assertEquals("dist/gen", copy.defaultOutputDir);
    assertFalse(copy.skipZkDefault);
    assertEquals("https://rpc.testnet.midnight.network", copy.devnetRpcUrl);
  }

  public void testConfigurableLifecycle() {
    MidnightSettingsConfigurable configurable = new MidnightSettingsConfigurable();
    assertEquals("dev.verloren.midnight.settings.MidnightSettingsConfigurable", configurable.getId());
    assertEquals("Midnight Compact", configurable.getDisplayName());
    assertNotNull(configurable.createComponent());

    configurable.reset();
    assertFalse("Immediately after reset, configurable should not be modified", configurable.isModified());

    configurable.disposeUIResources();
  }
}
