package dev.verloren.midnight.settings;

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class MidnightSettingsComponent {
  private final JPanel mainPanel;
  private final TextFieldWithBrowseButton compilerPathField = new TextFieldWithBrowseButton();
  private final JBTextField defaultOutputDirField = new JBTextField();
  private final JBCheckBox skipZkDefaultCheckBox = new JBCheckBox("Skip ZK proving key generation by default (--skip-zk)", true);
  private final JBTextField devnetRpcUrlField = new JBTextField();

  public MidnightSettingsComponent() {
    compilerPathField.addBrowseFolderListener(
        null,
        FileChooserDescriptorFactory.createSingleFileOrExecutableAppDescriptor()
            .withTitle("Select Compact Compiler Executable")
            .withDescription("Path to compact / compactc binary or WSL wrapper (e.g. /home/<user>/.local/bin/compact or \\\\wsl$\\Ubuntu\\...)")
    );

    JButton autoDetectButton = new JButton("Auto-Detect");
    autoDetectButton.addActionListener(_ -> {
      String detected = dev.verloren.midnight.run.CompactToolchainUtil.getCompilerExecutablePath(null);
      if (detected != null) {
        compilerPathField.setText(detected);
      }
    });

    JPanel compilerPanel = new JPanel(new java.awt.BorderLayout(5, 0));
    compilerPanel.add(compilerPathField, java.awt.BorderLayout.CENTER);
    compilerPanel.add(autoDetectButton, java.awt.BorderLayout.EAST);

    mainPanel = FormBuilder.createFormBuilder()
        .addLabeledComponent(new JBLabel("Compiler executable path (compact / compactc):"), compilerPanel, 1, false)
        .addLabeledComponent(new JBLabel("Default output directory:"), defaultOutputDirField, 1, false)
        .addComponent(skipZkDefaultCheckBox, 1)
        .addLabeledComponent(new JBLabel("Devnet / Node RPC URL:"), devnetRpcUrlField, 1, false)
        .addComponentFillVertically(new JPanel(), 0)
        .getPanel();
  }

  public @NotNull JPanel getPanel() {
    return mainPanel;
  }

  public @NotNull String getCompilerPath() {
    return compilerPathField.getText().trim();
  }

  public void setCompilerPath(@NotNull String newPath) {
    compilerPathField.setText(newPath);
  }

  public @NotNull String getDefaultOutputDir() {
    return defaultOutputDirField.getText().trim();
  }

  public void setDefaultOutputDir(@NotNull String newDir) {
    defaultOutputDirField.setText(newDir);
  }

  public boolean isSkipZkDefault() {
    return skipZkDefaultCheckBox.isSelected();
  }

  public void setSkipZkDefault(boolean skipZk) {
    skipZkDefaultCheckBox.setSelected(skipZk);
  }

  public @NotNull String getDevnetRpcUrl() {
    return devnetRpcUrlField.getText().trim();
  }

  public void setDevnetRpcUrl(@NotNull String url) {
    devnetRpcUrlField.setText(url);
  }
}
