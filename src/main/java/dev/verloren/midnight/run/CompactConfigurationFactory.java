package dev.verloren.midnight.run;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * Factory responsible for creating default templates and instances of {@link CompactRunConfiguration}.
 *
 * <h3>Architectural Role</h3>
 * <p>Extends {@link ConfigurationFactory} to supply new {@link CompactRunConfiguration} instances
 * whenever an existing run configuration is duplicated, loaded from project files, or newly created
 * from template settings in IntelliJ IDEA.</p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Thread Safety:</b> Stateless factory. Invoked on EDT or background threads by the execution framework.</li>
 * </ul>
 *
 * @see CompactConfigurationType
 * @see CompactRunConfiguration
 */
public class CompactConfigurationFactory extends ConfigurationFactory {

  /**
   * Constructs a new factory tied to the specified Compact configuration type.
   *
   * @param type the parent {@link ConfigurationType}
   */
  public CompactConfigurationFactory(@NotNull ConfigurationType type) {
    super(type);
  }

  /**
   * Returns the stable programmatic identifier of this factory.
   *
   * @return {@link CompactConfigurationType#ID}
   */
  @Override
  public @NotNull String getId() {
    return CompactConfigurationType.ID;
  }

  /**
   * Creates a template run configuration for the given project, pre-populated with default settings.
   *
   * @param project the project in which the configuration will reside
   * @return a newly initialized {@link CompactRunConfiguration}
   */
  @Override
  public @NotNull RunConfiguration createTemplateConfiguration(@NotNull Project project) {
    return new CompactRunConfiguration(project, this, "Compact");
  }
}
