package dev.verloren.midnight.run;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.ConfigurationTypeUtil;
import dev.verloren.midnight.icons.MidnightIcons;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class CompactConfigurationType implements ConfigurationType {
  public static final String ID = "COMPACT_RUN_CONFIGURATION";

  @Override
  public @NotNull @Nls(capitalization = Nls.Capitalization.Title) String getDisplayName() {
    return "Compact Smart Contract";
  }

  @Override
  public @Nls(capitalization = Nls.Capitalization.Sentence) String getConfigurationTypeDescription() {
    return "Compiles Compact smart contracts and generates TypeScript and ZKIR artifacts";
  }

  @Override
  public Icon getIcon() {
    return MidnightIcons.FILE;
  }

  @Override
  public @NotNull @NonNls String getId() {
    return ID;
  }

  @Override
  public ConfigurationFactory[] getConfigurationFactories() {
    return new ConfigurationFactory[]{new CompactConfigurationFactory(this)};
  }

  public static CompactConfigurationType getInstance() {
    return ConfigurationTypeUtil.findConfigurationType(CompactConfigurationType.class);
  }
}
