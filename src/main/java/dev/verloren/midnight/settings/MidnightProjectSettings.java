package dev.verloren.midnight.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

/**
 * Project-level persistent settings for Midnight Compact.
 * Allows per-project selection of Compact compiler versions and path overrides.
 */
@Service(Service.Level.PROJECT)
@State(
    name = "dev.verloren.midnight.settings.MidnightProjectSettings",
    storages = @Storage("midnight.xml")
)
public final class MidnightProjectSettings implements PersistentStateComponent<MidnightProjectSettings> {
  /**
   * Specifically selected Compact compiler version for this project (e.g. "0.26.0", "0.23.0"),
   * or empty string to inherit the global/auto-detected default compiler.
   */
  public String selectedCompilerVersion = "";

  /**
   * Optional custom executable path override specific to this project.
   */
  public String customCompilerPath = "";

  public static MidnightProjectSettings getInstance(@NotNull Project project) {
    return project.getService(MidnightProjectSettings.class);
  }

  @Override
  public @NonNull MidnightProjectSettings getState() {
    return this;
  }

  @Override
  public void loadState(@NotNull MidnightProjectSettings state) {
    XmlSerializerUtil.copyBean(state, this);
  }
}
