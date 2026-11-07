package dev.verloren.midnight.toolwindow;

import com.intellij.icons.AllIcons;
import com.intellij.ide.HelpTooltipKt;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.text.HtmlChunk;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import dev.verloren.midnight.ui.CompactNotificationUtil;
import dev.verloren.midnight.version.CompactVersionManager;
import org.jetbrains.annotations.NotNull;

import javax.swing.JButton;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;

/**
 * Minimalist card representing a single downloaded/installed Compact compiler.
 *
 * <p>Displays compiler version, language version, and ledger information, along with
 * an "Active" badge or an uninstall button (with confirmation) for inactive versions.
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li>Constructed and mutated strictly on the Event Dispatch Thread (EDT).</li>
 *   <li>Uninstall triggers file system operations through {@link CompactVersionManager}.</li>
 * </ul>
 */
public class CompactInstalledVersionCard extends JBPanel<CompactInstalledVersionCard> {
  private final Project project;
  private final String version;
  private final boolean isActive;
  private final Runnable onVersionDeleted;

  public CompactInstalledVersionCard(
          @NotNull Project project,
          @NotNull String version,
          boolean isActive,
          @NotNull Runnable onVersionDeleted
  ) {
    super(new BorderLayout());
    this.project = project;
    this.version = version;
    this.isActive = isActive;
    this.onVersionDeleted = onVersionDeleted;

    setOpaque(true);
    setBackground(JBColor.namedColor("Panel.background", new JBColor(0xF2F2F2, 0x2B2D30)));
    setBorder(JBUI.Borders.compound(
            JBUI.Borders.customLine(JBColor.border(), 1),
            JBUI.Borders.empty(8, 12)
    ));

    initCardUI();
  }

  private void initCardUI() {
    add(createTextPanel(), BorderLayout.CENTER);
    add(createActionPanel(), BorderLayout.EAST);
  }

  private @NotNull JBPanel<?> createTextPanel() {
    String langVer = CompactVersionManager.getLanguageVersionForToolchain(version);
    String ledger = CompactVersionManager.getLedgerVersionForToolchain(version);

    JBLabel titleLabel = new JBLabel("v" + version, AllIcons.Actions.Checked, JBLabel.LEFT);
    titleLabel.setFont(JBFont.label().asBold());

    String desc = "Lang " + langVer + (ledger != null ? " • " + ledger : "");
    JBLabel descLabel = new JBLabel(desc);
    descLabel.setFont(JBFont.small());
    descLabel.setForeground(JBColor.namedColor("Label.infoForeground", JBColor.GRAY));

    JBPanel<?> textPanel = new JBPanel<>(new BorderLayout(0, JBUI.scale(2)));
    textPanel.setOpaque(false);
    textPanel.add(titleLabel, BorderLayout.NORTH);
    textPanel.add(descLabel, BorderLayout.SOUTH);
    return textPanel;
  }

  private @NotNull JBPanel<?> createActionPanel() {
    JBPanel<?> actionPanel = new JBPanel<>(new FlowLayout(FlowLayout.RIGHT, 0, 0));
    actionPanel.setOpaque(false);

    if (isActive) {
      JBLabel activeBadge = new JBLabel("Active");
      activeBadge.setFont(JBFont.small().asBold());
      activeBadge.setForeground(JBColor.namedColor("Label.infoForeground", new JBColor(0x2E7D32, 0x4CAF50)));
      activeBadge.setBorder(JBUI.Borders.empty(2, 6));
      HelpTooltipKt.setToolTipText(activeBadge, HtmlChunk.text("Currently active project toolchain (protected against deletion)"));
      actionPanel.add(activeBadge);
    } else {
      JButton deleteButton = new JButton(AllIcons.Actions.GC);
      deleteButton.setPreferredSize(new Dimension(JBUI.scale(28), JBUI.scale(28)));
      deleteButton.setContentAreaFilled(false);
      deleteButton.setBorderPainted(false);
      deleteButton.setFocusPainted(false);
      deleteButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      HelpTooltipKt.setToolTipText(deleteButton, HtmlChunk.text("Uninstall Compact compiler v" + version));
      deleteButton.addActionListener(_ -> onDeleteClicked());
      actionPanel.add(deleteButton);
    }
    return actionPanel;
  }

  private void onDeleteClicked() {
    int confirm = Messages.showYesNoDialog(
            project,
            "Are you sure you want to uninstall Compact compiler v" + version + "?",
            "Uninstall Compact Compiler",
            "Uninstall",
            "Cancel",
            Messages.getWarningIcon()
    );
    if (confirm != Messages.YES) {
      return;
    }

    CompactVersionManager.uninstallVersion(version);
    CompactNotificationUtil.notifyUser(
            project,
            "Compact Compiler v" + version + " has been uninstalled.",
            NotificationType.INFORMATION
    );
    onVersionDeleted.run();
  }

  public @NotNull String getVersion() {
    return version;
  }

  public boolean isActive() {
    return isActive;
  }
}
