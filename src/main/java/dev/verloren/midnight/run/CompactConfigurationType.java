package dev.verloren.midnight.run;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.ConfigurationTypeUtil;
import dev.verloren.midnight.icons.MidnightIcons;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * Defines the run configuration type for compiling Midnight Compact smart contracts.
 *
 * <h3>Architectural Role &amp; Extension Point</h3>
 * <p>Registered as a {@code configurationType} extension in {@code plugin.xml}. Implements
 * {@link ConfigurationType} to register "Compact Smart Contract" into IntelliJ's Run/Debug Configurations
 * dialogue (Edit Configurations, Run gutter icons, context menu Run actions). Serves as the factory provider
 * for {@link CompactConfigurationFactory} and the root descriptor for all Compact compilation profiles.</p>
 *
 * <h3>Configuration Infrastructure Hierarchy</h3>
 * <pre>
 *   [ConfigurationType: CompactConfigurationType]
 *                      │
 *                      ▼
 *   [ConfigurationFactory: CompactConfigurationFactory]
 *                      │
 *                      ▼
 *   [RunConfiguration: CompactRunConfiguration]
 *                      │
 *         ┌────────────┴────────────┐
 *         │                         │
 *         ▼                         ▼
 *   [SettingsEditor]        [RunProfileState]
 *   CompactRunConfiguration CompactRunProfileState
 *   Editor (Swing UI)       (Execution &amp; Process Handler)
 * </pre>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Thread Safety:</b> Stateless and thread-safe. Methods are invoked on EDT when opening configuration dialogues
 *       or populating action groups.</li>
 * </ul>
 *
 * @see CompactConfigurationFactory
 * @see CompactRunConfiguration
 * @see CompactRunConfigurationProducer
 */
public class CompactConfigurationType implements ConfigurationType {

  /**
   * Unique identifier for the Compact run configuration type across the IDE.
   */
  public static final String ID = "COMPACT_RUN_CONFIGURATION";

  /**
   * Returns the user-facing display name of this configuration type in run dialogues and menus.
   *
   * @return {@code "Compact Smart Contract"}
   */
  @Override
  public @NotNull @Nls(capitalization = Nls.Capitalization.Title) String getDisplayName() {
    return "Compact Smart Contract";
  }

  /**
   * Returns a concise description of what this configuration type executes.
   *
   * @return descriptive summary of the Compact smart contract compiler configuration
   */
  @Override
  public @Nls(capitalization = Nls.Capitalization.Sentence) String getConfigurationTypeDescription() {
    return "Compiles Compact smart contracts and generates TypeScript and ZKIR artifacts";
  }

  /**
   * Returns the icon associated with Compact run configurations in toolbars and dialogues.
   *
   * @return {@link MidnightIcons#FILE}
   */
  @Override
  public Icon getIcon() {
    return MidnightIcons.FILE;
  }

  /**
   * Returns the stable programmatic ID of this configuration type.
   *
   * @return {@link #ID}
   */
  @Override
  public @NotNull @NonNls String getId() {
    return ID;
  }

  /**
   * Returns the array of configuration factories associated with this type.
   *
   * @return array containing a single {@link CompactConfigurationFactory}
   */
  @Override
  public ConfigurationFactory[] getConfigurationFactories() {
    return new ConfigurationFactory[]{new CompactConfigurationFactory(this)};
  }

  /**
   * Retrieves the singleton instance of {@link CompactConfigurationType} registered in the platform container.
   *
   * @return the active configuration type instance
   */
  public static CompactConfigurationType getInstance() {
    return ConfigurationTypeUtil.findConfigurationType(CompactConfigurationType.class);
  }
}
