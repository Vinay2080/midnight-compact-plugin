package dev.verloren.midnight.settings;

import com.intellij.openapi.options.SearchableConfigurable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class MidnightSettingsConfigurable implements SearchableConfigurable {
  private MidnightSettingsComponent settingsComponent;

  @Override
  public @NotNull @NonNls String getId() {
    return "dev.verloren.midnight.settings.MidnightSettingsConfigurable";
  }

  @Override
  public @Nls(capitalization = Nls.Capitalization.Title) String getDisplayName() {
    return "Midnight Compact";
  }

  @Override
  public @Nullable JComponent createComponent() {
    settingsComponent = new MidnightSettingsComponent();
    return settingsComponent.getPanel();
  }

  @Override
  public boolean isModified() {
    MidnightSettingsState state = MidnightSettingsState.getInstance();
    if (state == null || settingsComponent == null) {
      return false;
    }
    return !settingsComponent.getCompilerPath().equals(state.compilerPath)
        || !settingsComponent.getDefaultOutputDir().equals(state.defaultOutputDir)
        || settingsComponent.isSkipZkDefault() != state.skipZkDefault
        || !settingsComponent.getDevnetRpcUrl().equals(state.devnetRpcUrl);
  }

  @Override
  public void apply() {
    MidnightSettingsState state = MidnightSettingsState.getInstance();
    if (state != null && settingsComponent != null) {
      state.compilerPath = settingsComponent.getCompilerPath();
      state.defaultOutputDir = settingsComponent.getDefaultOutputDir();
      state.skipZkDefault = settingsComponent.isSkipZkDefault();
      state.devnetRpcUrl = settingsComponent.getDevnetRpcUrl();
    }
  }

  @Override
  public void reset() {
    MidnightSettingsState state = MidnightSettingsState.getInstance();
    if (state != null && settingsComponent != null) {
      settingsComponent.setCompilerPath(state.compilerPath != null ? state.compilerPath : "");
      settingsComponent.setDefaultOutputDir(state.defaultOutputDir != null ? state.defaultOutputDir : "gen");
      settingsComponent.setSkipZkDefault(state.skipZkDefault);
      settingsComponent.setDevnetRpcUrl(state.devnetRpcUrl != null ? state.devnetRpcUrl : "http://localhost:9944");
    }
  }

  @Override
  public void disposeUIResources() {
    settingsComponent = null;
  }
}
