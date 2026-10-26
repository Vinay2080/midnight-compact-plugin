package dev.verloren.midnight.toolwindow;

import com.intellij.icons.AllIcons;
import com.intellij.ide.HelpTooltipKt;
import com.intellij.openapi.util.text.HtmlChunk;
import com.intellij.util.ui.JBUI;
import dev.verloren.midnight.version.CompactVersionManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * UI building helper for {@link CompactVersionCard}.
 */
public final class CompactVersionCardUiBuilder {

  private CompactVersionCardUiBuilder() {}

  /**
   * Constructs the action icon component (download label, delete button, or progress spinner).
   */
  public static @NotNull JComponent createActionIconComponent(
      @NotNull String version,
      boolean isDownloading,
      boolean isInstalled,
      @Nullable Runnable onRemove
  ) {
    Dimension iconBoxSize = new Dimension(JBUI.scale(22), JBUI.scale(22));
    if (isDownloading) {
      com.intellij.ui.components.JBLabel spinLabel = new com.intellij.ui.components.JBLabel(AllIcons.Process.Step_1);
      spinLabel.setPreferredSize(iconBoxSize);
      HelpTooltipKt.setToolTipText(spinLabel, HtmlChunk.text("Downloading compiler..."));
      return spinLabel;
    }
    if (isInstalled) {
      JButton deleteBtn = new JButton(AllIcons.Actions.GC);
      deleteBtn.setPreferredSize(iconBoxSize);
      deleteBtn.setMinimumSize(iconBoxSize);
      deleteBtn.setMaximumSize(iconBoxSize);
      deleteBtn.setBorder(BorderFactory.createEmptyBorder());
      deleteBtn.setContentAreaFilled(false);
      deleteBtn.setFocusPainted(false);
      deleteBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      HelpTooltipKt.setToolTipText(deleteBtn, HtmlChunk.text("Remove Compact v" + version + " from local storage"));
      deleteBtn.addActionListener(_ -> {
        if (onRemove != null) {
          onRemove.run();
        }
      });
      return deleteBtn;
    }
    com.intellij.ui.components.JBLabel dlLabel = new com.intellij.ui.components.JBLabel(AllIcons.Actions.Download);
    dlLabel.setPreferredSize(iconBoxSize);
    String assetName = CompactVersionManager.getReleaseAssetName(version);
    HelpTooltipKt.setToolTipText(dlLabel, HtmlChunk.text("Download Compact v" + version + " (" + assetName + ")"));
    return dlLabel;
  }
}
