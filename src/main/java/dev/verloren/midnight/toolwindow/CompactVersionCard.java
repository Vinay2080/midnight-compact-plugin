package dev.verloren.midnight.toolwindow;

import com.intellij.icons.AllIcons;
import com.intellij.ide.HelpTooltipKt;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.ui.JBPopupMenu;
import com.intellij.openapi.util.text.HtmlChunk;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Minimal, classic version card displaying a single action icon (download or delete),
 * version title, and pragma compatibility badge with fixed uniform dimensions.
 */
public class CompactVersionCard extends JPanel {
  private final String version;
  private final boolean isInstalled;
  private boolean isActive;
  private final String installedPath;
  private final Runnable onSelect;
  private final Runnable onRemove;

  private final JBLabel titleLabel;
  private final JComponent actionIconComponent;
  private boolean isHovered = false;

  private static final int CARD_HEIGHT = 42;

  @SuppressWarnings("this-escape")
  public CompactVersionCard(
      @NotNull String version,
      @NotNull String title,
      boolean isInstalled,
      boolean isActive,
      boolean isPragmaMatch,
      boolean isDownloading,
      @Nullable String installedPath,
      @Nullable Runnable onSelect,
      @Nullable Runnable onRemove
  ) {
    super(new BorderLayout(JBUI.scale(8), 0));
    this.version = version;
    this.isInstalled = isInstalled;
    this.isActive = isActive;
    this.installedPath = installedPath;
    this.onSelect = onSelect;
    this.onRemove = onRemove;

    setOpaque(false);
    setBorder(JBUI.Borders.empty(4, 10));

    // Fixed uniform dimensions across all cards so sizing never jumps
    int height = JBUI.scale(CARD_HEIGHT);
    setPreferredSize(new Dimension(0, height));
    setMinimumSize(new Dimension(0, height));
    setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

    // Single Left Panel: Single Action Icon + Version Title + Pragma Match
    JPanel contentPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, JBUI.scale(8), JBUI.scale(4)));
    contentPanel.setOpaque(false);

    // Single Action Icon (either download, delete, or progress)
    Dimension iconBoxSize = new Dimension(JBUI.scale(22), JBUI.scale(22));
    if (isDownloading) {
      JBLabel spinLabel = new JBLabel(AllIcons.Process.Step_1);
      spinLabel.setPreferredSize(iconBoxSize);
      HelpTooltipKt.setToolTipText(spinLabel, HtmlChunk.text("Downloading compiler..."));
      actionIconComponent = spinLabel;
    } else if (isInstalled) {
      JButton deleteBtn = new JButton(AllIcons.Actions.GC);
      deleteBtn.setPreferredSize(iconBoxSize);
      deleteBtn.setMinimumSize(iconBoxSize);
      deleteBtn.setMaximumSize(iconBoxSize);
      deleteBtn.setBorder(BorderFactory.createEmptyBorder());
      deleteBtn.setContentAreaFilled(false);
      deleteBtn.setFocusPainted(false);
      deleteBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      HelpTooltipKt.setToolTipText(deleteBtn, HtmlChunk.text("Remove Compact v" + version + " from local storage"));
      deleteBtn.addActionListener(e -> {
        if (onRemove != null) {
          onRemove.run();
        }
      });
      actionIconComponent = deleteBtn;
    } else {
      JBLabel dlLabel = new JBLabel(AllIcons.Actions.Download);
      dlLabel.setPreferredSize(iconBoxSize);
      HelpTooltipKt.setToolTipText(dlLabel, HtmlChunk.text("Download Compact v" + version));
      actionIconComponent = dlLabel;
    }
    contentPanel.add(actionIconComponent);

    // Title
    titleLabel = new JBLabel(title);
    titleLabel.setFont(JBFont.label().asBold());
    if (isActive) {
      titleLabel.setForeground(JBUI.CurrentTheme.Link.Foreground.ENABLED);
    }
    contentPanel.add(titleLabel);

    // Pragma Match Badge
    if (isPragmaMatch) {
      JBLabel matchLabel = new JBLabel("✔ pragma match");
      matchLabel.setFont(JBFont.small());
      matchLabel.setForeground(new JBColor(new Color(36, 138, 61), new Color(63, 185, 80)));
      contentPanel.add(matchLabel);
    }

    add(contentPanel, BorderLayout.CENTER);

    // Mouse interactions
    MouseAdapter adapter = new MouseAdapter() {
      @Override
      public void mouseEntered(MouseEvent e) {
        if (!isHovered) {
          isHovered = true;
          repaint();
        }
      }

      @Override
      public void mouseExited(MouseEvent e) {
        Point p = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), CompactVersionCard.this);
        if (!contains(p)) {
          isHovered = false;
          repaint();
        }
      }

      @Override
      public void mousePressed(MouseEvent e) {
        if (e.isPopupTrigger()) {
          showContextMenu(e);
        }
      }

      @Override
      public void mouseReleased(MouseEvent e) {
        if (e.isPopupTrigger()) {
          showContextMenu(e);
        }
      }

      @Override
      public void mouseClicked(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e) && !e.isPopupTrigger()) {
          // If clicked on delete button, its own action listener handles it
          if (e.getSource() == actionIconComponent && isInstalled) {
            return;
          }
          if (!isDownloading && onSelect != null) {
            if (isInstalled) {
              setActive(true);
            }
            onSelect.run();
          }
        }
      }
    };

    attachMouseListener(contentPanel, adapter);
    addMouseListener(adapter);
  }

  public void setActive(boolean active) {
    this.isActive = active;
    if (active) {
      titleLabel.setForeground(JBUI.CurrentTheme.Link.Foreground.ENABLED);
    } else {
      titleLabel.setForeground(JBColor.foreground());
    }
    repaint();
  }

  private void attachMouseListener(@NotNull Component component, @NotNull MouseAdapter adapter) {
    if (component == actionIconComponent && isInstalled) {
      return;
    }
    component.addMouseListener(adapter);
    if (component instanceof Container container) {
      for (Component child : container.getComponents()) {
        attachMouseListener(child, adapter);
      }
    }
  }

  private void showContextMenu(MouseEvent e) {
    JBPopupMenu menu = new JBPopupMenu();
    if (isInstalled) {
      JMenuItem removeItem = new JMenuItem("Remove Version v" + version, AllIcons.Actions.GC);
      removeItem.addActionListener(ev -> {
        if (onRemove != null) {
          onRemove.run();
        }
      });
      menu.add(removeItem);

      if (installedPath != null && !installedPath.isEmpty()) {
        JMenuItem copyItem = new JMenuItem("Copy Executable Path", AllIcons.Actions.Copy);
        copyItem.addActionListener(ev -> CopyPasteManager.getInstance().setContents(new StringSelection(installedPath)));
        menu.add(copyItem);
      }
    } else {
      JMenuItem downloadItem = new JMenuItem("Download & Switch to v" + version, AllIcons.Actions.Download);
      downloadItem.addActionListener(ev -> {
        if (onSelect != null) {
          onSelect.run();
        }
      });
      menu.add(downloadItem);
    }

    menu.show(this, e.getX(), e.getY());
  }

  public String getVersion() {
    return version;
  }

  public boolean isInstalled() {
    return isInstalled;
  }

  public boolean isActive() {
    return isActive;
  }

  public @Nullable String getInstalledPath() {
    return installedPath;
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);

    Graphics2D g2 = (Graphics2D) g.create();
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    int arc = JBUI.scale(8);
    int width = getWidth() - 1;
    int height = getHeight() - 1;

    // Background color: active > hovered > default
    Color bgColor;
    if (isActive) {
      bgColor = JBColor.namedColor("VersionCard.activeBackground", new JBColor(new Color(230, 242, 255), new Color(38, 55, 75)));
    } else if (isHovered) {
      bgColor = JBColor.namedColor("VersionCard.hoverBackground", new JBColor(new Color(245, 247, 250), new Color(48, 50, 52)));
    } else {
      bgColor = JBColor.namedColor("VersionCard.background", new JBColor(new Color(250, 250, 250), new Color(40, 42, 44)));
    }

    g2.setColor(bgColor);
    g2.fillRoundRect(0, 0, width, height, arc, arc);

    // Border color: active > default subtle
    Color borderColor;
    if (isActive) {
      borderColor = JBUI.CurrentTheme.Link.Foreground.ENABLED;
    } else {
      borderColor = JBColor.namedColor("VersionCard.borderColor", new JBColor(new Color(220, 224, 230), new Color(55, 57, 60)));
    }

    g2.setColor(borderColor);
    g2.drawRoundRect(0, 0, width, height, arc, arc);

    g2.dispose();
  }
}
