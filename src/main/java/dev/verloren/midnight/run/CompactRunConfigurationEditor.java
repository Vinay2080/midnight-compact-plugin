package dev.verloren.midnight.run;

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class CompactRunConfigurationEditor extends SettingsEditor<CompactRunConfiguration> {
  private final TextFieldWithBrowseButton compactFilePathField = new TextFieldWithBrowseButton();
  private final TextFieldWithBrowseButton outputDirectoryField = new TextFieldWithBrowseButton();
  private final JBCheckBox skipZkCheckBox = new JBCheckBox("Skip ZK proving key generation (--skip-zk, recommended for fast dev builds)", true);
  private final JBTextField customFlagsField = new JBTextField();

  public CompactRunConfigurationEditor() {
    compactFilePathField.addBrowseFolderListener(
        null,
        FileChooserDescriptorFactory.createSingleFileDescriptor("compact")
            .withTitle("Select Compact Contract File")
            .withDescription("Choose the .compact smart contract source file to compile")
    );
    outputDirectoryField.addBrowseFolderListener(
        null,
        FileChooserDescriptorFactory.createSingleFolderDescriptor()
            .withTitle("Select Output Directory")
            .withDescription("Choose the target directory for generated TypeScript and ZKIR artifacts")
    );

    compactFilePathField.getTextField().getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
      private void updateOutput() {
        String currentOut = outputDirectoryField.getText().trim();
        String currentFile = compactFilePathField.getText().trim();
        if ((currentOut.isEmpty() || "gen".equals(currentOut)) && !currentFile.isEmpty()) {
          outputDirectoryField.setText(CompactToolchainUtil.deriveOutputDirectory(null, currentFile));
        }
      }

      @Override
      public void insertUpdate(javax.swing.event.DocumentEvent e) {
        updateOutput();
      }

      @Override
      public void removeUpdate(javax.swing.event.DocumentEvent e) {
        updateOutput();
      }

      @Override
      public void changedUpdate(javax.swing.event.DocumentEvent e) {
        updateOutput();
      }
    });
  }


  @Override
  protected void resetEditorFrom(@NotNull CompactRunConfiguration config) {
    compactFilePathField.setText(config.getCompactFilePath());
    outputDirectoryField.setText(config.getOutputDirectory());
    skipZkCheckBox.setSelected(config.isSkipZk());
    customFlagsField.setText(config.getCustomCompilerFlags());
  }

  @Override
  protected void applyEditorTo(@NotNull CompactRunConfiguration config) {
    config.setCompactFilePath(compactFilePathField.getText());
    config.setOutputDirectory(outputDirectoryField.getText());
    config.setSkipZk(skipZkCheckBox.isSelected());
    config.setCustomCompilerFlags(customFlagsField.getText());
  }

  @Override
  protected @NotNull JComponent createEditor() {
    return FormBuilder.createFormBuilder()
        .addLabeledComponent("Compact file:", compactFilePathField)
        .addLabeledComponent("Output directory:", outputDirectoryField)
        .addComponent(skipZkCheckBox)
        .addLabeledComponent("Additional compiler flags:", customFlagsField)
        .getPanel();
  }
}
