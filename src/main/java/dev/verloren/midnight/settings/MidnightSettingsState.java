package dev.verloren.midnight.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@State(
    name = "dev.verloren.midnight.settings.MidnightSettingsState",
    storages = @Storage("MidnightCompactPlugin.xml")
)
public class MidnightSettingsState implements PersistentStateComponent<MidnightSettingsState> {
  public String compilerPath = "";
  public String defaultOutputDir = "gen";
  public boolean skipZkDefault = true;
  public String devnetRpcUrl = "http://localhost:9944";

  public static MidnightSettingsState getInstance() {
    return ApplicationManager.getApplication().getService(MidnightSettingsState.class);
  }

  @Override
  public @Nullable MidnightSettingsState getState() {
    return this;
  }

  @Override
  public void loadState(@NotNull MidnightSettingsState state) {
    XmlSerializerUtil.copyBean(state, this);
  }
}
