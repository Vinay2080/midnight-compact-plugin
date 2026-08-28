package dev.verloren.midnight.run;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class CompactConfigurationFactory extends ConfigurationFactory {
  public CompactConfigurationFactory(@NotNull ConfigurationType type) {
    super(type);
  }

  @Override
  public @NotNull String getId() {
    return CompactConfigurationType.ID;
  }

  @Override
  public @NotNull RunConfiguration createTemplateConfiguration(@NotNull Project project) {
    return new CompactRunConfiguration(project, this, "Compact");
  }
}
