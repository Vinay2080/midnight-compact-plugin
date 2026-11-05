package dev.verloren.midnight.toolwindow;

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.ui.TextBrowseFolderListener;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.text.HtmlChunk;
import com.intellij.ide.HelpTooltipKt;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.JPanel;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

/**
 * Collapsible panel containing optional and advanced compiler flags.
 */
public class CompactAdvancedOptionsPanel extends JPanel {
  private final JBCheckBox skipZkCheckBox = new JBCheckBox("Skip ZK proof generation (--skip-zk)", true);
  private final TextFieldWithBrowseButton outputDirField = new TextFieldWithBrowseButton();
  private final JBTextField customFlagsField = new JBTextField();

  /**
   * Constructs the advanced options panel.
   */
  @SuppressWarnings("this-escape")
  public CompactAdvancedOptionsPanel() {
    super(new GridBagLayout());
    setOpaque(false);
    initLayout();
  }

  private void initLayout() {
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;

    skipZkCheckBox.setOpaque(false);
    HelpTooltipKt.setToolTipText(skipZkCheckBox, HtmlChunk.text("Bypasses expensive ZK proving key generation for fast local iteration"));
    add(skipZkCheckBox, gbc);

    addOutputDirSection(gbc);
    addCustomFlagsSection(gbc);
  }

  @SuppressWarnings("DialogTitleCapitalization")
  private void addOutputDirSection(@NotNull GridBagConstraints gbc) {
    gbc.gridy++;
    gbc.insets = JBUI.insetsTop(8);
    JBLabel outDirLabel = new JBLabel("Output Directory:");
    outDirLabel.setFont(JBFont.small());
    add(outDirLabel, gbc);

    gbc.gridy++;
    gbc.insets = JBUI.insetsTop(2);
    outputDirField.setText("gen");
    outputDirField.addBrowseFolderListener(
        new TextBrowseFolderListener(
            FileChooserDescriptorFactory.createSingleFolderDescriptor()
                .withTitle("Select Output Directory")
                .withDescription("Target folder for generated TypeScript and ZKIR artifacts")
        )
    );
    add(outputDirField, gbc);
  }

  @SuppressWarnings("DialogTitleCapitalization")
  private void addCustomFlagsSection(@NotNull GridBagConstraints gbc) {
    gbc.gridy++;
    gbc.insets = JBUI.insetsTop(8);
    JBLabel flagsLabel = new JBLabel("Additional Compiler Flags:");
    flagsLabel.setFont(JBFont.small());
    add(flagsLabel, gbc);

    gbc.gridy++;
    gbc.insets = JBUI.insetsTop(2);
    HelpTooltipKt.setToolTipText(customFlagsField, HtmlChunk.text("Custom flags passed directly to compactc (e.g. --verbose)"));
    add(customFlagsField, gbc);
  }

  public boolean isSkipZkSelected() {
    return skipZkCheckBox.isSelected();
  }

  public @NotNull String getOutputDirectory() {
    return outputDirField.getText().trim();
  }

  public @NotNull String getCustomFlags() {
    return customFlagsField.getText().trim();
  }
}
