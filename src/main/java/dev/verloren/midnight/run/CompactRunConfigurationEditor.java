package dev.verloren.midnight.run;

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.ui.TextBrowseFolderListener;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * Swing-based configuration editor providing the visual settings form for {@link CompactRunConfiguration}.
 *
 * <h3>Architectural Role</h3>
 * <p>Extends {@link SettingsEditor} to present IntelliJ's "Edit Configurations" dialog panel for Compact
 * run configurations. Provides file browsing for {@code .compact} contracts, directory selection for compiler
 * output, a toggle for bypassing Zero-Knowledge proof generation ({@code --skip-zk}), and a freeform field
 * for additional compiler options.</p>
 *
 * <h3>UI Layout &amp; Dynamic State Synchronization</h3>
 * <pre>
 *   ┌────────────────────────────────────────────────────────────────────────┐
 *   │ Compact file:               [ /path/to/contract.compact ] [Browse...] │
 *   │ Output directory:           [ /path/to/project/gen      ] [Browse...] │
 *   │ [X] Skip ZK proving key generation (--skip-zk)                        │
 *   │ Additional compiler flags:  [ --verbose                               ] │
 *   └────────────────────────────────────────────────────────────────────────┘
 * </pre>
 * <p>A document listener on {@code compactFilePathField} monitors text changes: whenever a new contract
 * file path is typed or selected and the output directory is either unconfigured or set to the default
 * {@code "gen"}, the output directory is automatically recalculated via
 * {@link CompactToolchainUtil#deriveOutputDirectory(com.intellij.openapi.project.Project, String)}.</p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>EDT (Event Dispatch Thread):</b> Instantiation, UI binding, and listener execution are strictly
 *       confined to the Swing EDT.</li>
 * </ul>
 *
 * @see CompactRunConfiguration
 * @see SettingsEditor
 */
public class CompactRunConfigurationEditor extends SettingsEditor<CompactRunConfiguration> {

  /**
   * File chooser text field for selecting the target {@code .compact} contract source file.
   */
  private final TextFieldWithBrowseButton compactFilePathField = new TextFieldWithBrowseButton();

  /**
   * Directory chooser text field for selecting where compiler artifacts are generated.
   */
  private final TextFieldWithBrowseButton outputDirectoryField = new TextFieldWithBrowseButton();

  /**
   * Checkbox controlling whether to pass {@code --skip-zk} for rapid developer iterations.
   */
  private final JBCheckBox skipZkCheckBox = new JBCheckBox("Skip ZK proving key generation (--skip-zk, recommended for fast dev builds)", true);

  /**
   * Text field for passing custom arbitrary compiler arguments.
   */
  private final JBTextField customFlagsField = new JBTextField();

  /**
   * Initializes the editor form, configuring file chooser descriptors and real-time document listeners.
   */
  public CompactRunConfigurationEditor() {
    // 1. Configure file chooser descriptor restricted to .compact source files
    compactFilePathField.addBrowseFolderListener(
        new TextBrowseFolderListener(
            FileChooserDescriptorFactory.createSingleFileDescriptor("compact")
                .withTitle("Select Compact Contract File")
                .withDescription("Choose the .compact smart contract source file to compile")
        )
    );

    // 2. Configure directory chooser descriptor for the destination output folder
    outputDirectoryField.addBrowseFolderListener(
        new TextBrowseFolderListener(
            FileChooserDescriptorFactory.createSingleFolderDescriptor()
                .withTitle("Select Output Directory")
                .withDescription("Choose the target directory for generated TypeScript and ZKIR artifacts")
        )
    );

    // 3. Register document listener to dynamically derive output folder when file changes
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

  /**
   * Copies configuration values from the run configuration model into the UI fields.
   *
   * @param config the configuration being edited
   */
  @Override
  protected void resetEditorFrom(@NotNull CompactRunConfiguration config) {
    compactFilePathField.setText(config.getCompactFilePath());
    outputDirectoryField.setText(config.getOutputDirectory());
    skipZkCheckBox.setSelected(config.isSkipZk());
    customFlagsField.setText(config.getCustomCompilerFlags());
  }

  /**
   * Writes values from UI fields back into the run configuration model upon save or apply.
   *
   * @param config the configuration to update
   */
  @Override
  protected void applyEditorTo(@NotNull CompactRunConfiguration config) {
    config.setCompactFilePath(compactFilePathField.getText());
    config.setOutputDirectory(outputDirectoryField.getText());
    config.setSkipZk(skipZkCheckBox.isSelected());
    config.setCustomCompilerFlags(customFlagsField.getText());
  }

  /**
   * Assembles and returns the Swing UI panel using {@link FormBuilder}.
   *
   * @return the assembled configuration editor component
   */
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
