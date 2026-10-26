package dev.verloren.midnight.toolwindow;

import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * Custom painter rendering background fills and rounded borders for {@link CompactVersionCard}.
 */
public final class CompactVersionCardPainter {

  private CompactVersionCardPainter() {}

  /**
   * Paints the version card's rounded background and border according to active and hovered states.
   */
  public static void paintCard(
      @NotNull Graphics g,
      @NotNull JComponent component,
      boolean isActive,
      boolean isHovered
  ) {
    Graphics2D g2 = (Graphics2D) g.create();
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    int arc = JBUI.scale(8);
    int width = component.getWidth() - 1;
    int height = component.getHeight() - 1;

    Color bgColor;
    if (isActive) {
      bgColor = JBColor.namedColor("VersionCard.activeBackground", new JBColor(new Color(230, 242, 255), new Color(38, 55, 75)));
    } else if (isHovered) {
      bgColor = JBColor.namedColor("VersionCard.hoverBackground", new JBColor(new Color(245, 247, 250), new Color(48, 50, 52)));
    } else {
      bgColor = JBColor.namedColor("VersionCard.background", new JBColor(com.intellij.ui.Gray._250, new Color(40, 42, 44)));
    }

    g2.setColor(bgColor);
    g2.fillRoundRect(0, 0, width, height, arc, arc);

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
