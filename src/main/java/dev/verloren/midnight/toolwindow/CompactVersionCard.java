package dev.verloren.midnight.toolwindow;

import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Minimal, classic version card displaying a single action icon (download or delete),
 * version title, and pragma compatibility badge with fixed uniform dimensions.
 *
 * <p><h3>Visual Design &amp; Layout</h3>
 * Built with a fixed vertical dimension (44 scaled pixels) ensuring identical height across all cards
 * regardless of whether badges or secondary text are present. Uses anti-aliased rounded rectangle
 * painting with theme-aware background and border palettes:
 * <pre>
 *   ┌────────────────────────────────────────────────────────────────────────┐
 *   │ [Action Icon]   Compact v0.24.0   ✔ pragma match                       │
 *   │                 Midnight compact compiler 0.24.0                       │
 *   └────────────────────────────────────────────────────────────────────────┘
 * </pre>
 * <ul>
 *   <li><b>Action Icon:</b> Left-aligned action trigger showing either a download icon (for uninstalled releases),
 *       a trash/GC button (for locally cached releases), or an animated progress spinner (during download).</li>
 *   <li><b>Title &amp; Match Badge:</b> Bold version title (rendered in hyperlink color when active), accompanied
 *       by a green checkmark badge when the toolchain satisfies the active file's {@code pragma} constraint.</li>
 *   <li><b>Description:</b> Optional secondary subtitle displaying language version compatibility details.</li>
 * </ul>
 *
 * <p><h3>User Interactions &amp; Context Menu</h3>
 * Supports both mouse clicks and contextual popups:
 * <ul>
 *   <li><b>Left Click:</b> If installed, immediately switches the active compiler. If uninstalled, triggers confirmation and background download.</li>
 *   <li><b>Context Menu (Right Click):</b> Offers shortcut actions including "Remove Version", "Copy Executable Path", "Download &amp; Switch",
 *       and "Copy Asset Download URL".</li>
 * </ul>
 *
 * <p><h3>Threading Contract</h3>
 * As a Swing component, all lifecycle modifications, listener callbacks, and paint operations
 * MUST execute exclusively on the Swing Event Dispatch Thread (EDT).
 *
 * @see CompactCompilerPanel
 * @see dev.verloren.midnight.version.CompactVersionManager
 */
public class CompactVersionCard extends JPanel {

  /** Semantic version string represented by this card (e.g., {@code "0.24.0"}). */
  private final String version;

  /** Optional descriptive subtitle or language compatibility note. */
  private final String description;

  /** Whether the compiler binary for this version is already downloaded and present in local storage. */
  private final boolean isInstalled;

  /** Whether this version is currently selected as the active project compiler. */
  private boolean isActive;

  /** Absolute file system path to the local compiler executable, or {@code null} if not installed. */
  private final String installedPath;

  /** Callback invoked when the user selects this card to make it active or initiate download. */
  private final Runnable onSelect;

  /** Callback invoked when the user requests removal/uninstall of this version. */
  private final Runnable onRemove;

  /** Label displaying the primary card title (e.g., {@code "Compact v0.24.0"}). */
  private final JBLabel titleLabel;

  /** Secondary label displaying the compiler description, or {@code null} if no description was supplied. */
  private final JBLabel descriptionLabel;

  /** Left-hand action component (download label, delete button, or progress spinner). */
  private final JComponent actionIconComponent;

  /** Tracks mouse hover state to trigger dynamic background highlights. */
  private boolean isHovered = false;

  /** Base card height in unscaled pixels before DPI scaling. */
  private static final int CARD_HEIGHT = 44;

  /**
   * Constructs a version card without an explicit description subtitle.
   *
   * @param version        semantic version string
   * @param title          display title for the header row
   * @param isInstalled    true if already present in local plugin storage
   * @param isActive       true if currently configured as the project compiler
   * @param isPragmaMatch  true if this version matches the contract's pragma constraint
   * @param isDownloading  true if a background download task is currently active for this version
   * @param installedPath  local executable path, or {@code null}
   * @param onSelect       callback triggered on card selection or download click
   * @param onRemove       callback triggered on removal action
   */
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
    this(version, title, null, isInstalled, isActive, isPragmaMatch, isDownloading, installedPath, onSelect, onRemove);
  }

  /**
   * Constructs a fully specified version card with an optional description subtitle.
   *
   * @param version        semantic version string
   * @param title          display title for the header row
   * @param description    optional subtitle description text
   * @param isInstalled    true if already present in local plugin storage
   * @param isActive       true if currently configured as the project compiler
   * @param isPragmaMatch  true if this version matches the contract's pragma constraint
   * @param isDownloading  true if a background download task is currently active for this version
   * @param installedPath  local executable path, or {@code null}
   * @param onSelect       callback triggered on card selection or download click
   * @param onRemove       callback triggered on removal action
   */
  @SuppressWarnings("this-escape")
  public CompactVersionCard(
      @NotNull String version,
      @NotNull String title,
      @Nullable String description,
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
    this.description = description;
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

    // Single Action Icon (either download, delete, or progress)
    actionIconComponent = CompactVersionCardUiBuilder.createActionIconComponent(version, isDownloading, isInstalled, onRemove);

    JPanel leftContainer = new JPanel(new BorderLayout(JBUI.scale(8), 0));
    leftContainer.setOpaque(false);

    JPanel iconBox = new JPanel(new GridBagLayout());
    iconBox.setOpaque(false);
    iconBox.add(actionIconComponent);
    leftContainer.add(iconBox, BorderLayout.WEST);

    JPanel textPanel = new JPanel();
    textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
    textPanel.setOpaque(false);

    JPanel headerRow = new JPanel(new FlowLayout(FlowLayout.LEFT, JBUI.scale(6), 0));
    headerRow.setOpaque(false);

    titleLabel = new JBLabel(title);
    titleLabel.setFont(JBFont.label().asBold());
    if (isActive) {
      titleLabel.setForeground(JBUI.CurrentTheme.Link.Foreground.ENABLED);
    }
    headerRow.add(titleLabel);

    if (isPragmaMatch) {
      JBLabel matchLabel = new JBLabel("✔ pragma match");
      matchLabel.setFont(JBFont.small());
      matchLabel.setForeground(new JBColor(new Color(36, 138, 61), new Color(63, 185, 80)));
      headerRow.add(matchLabel);
    }
    textPanel.add(headerRow);

    if (description != null && !description.isEmpty()) {
      JPanel descRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
      descRow.setOpaque(false);
      descriptionLabel = new JBLabel(description);
      descriptionLabel.setFont(JBFont.small());
      descriptionLabel.setForeground(JBColor.namedColor("VersionCard.descriptionForeground", new JBColor(new Color(110, 115, 122), new Color(150, 155, 162))));
      descRow.add(descriptionLabel);
      textPanel.add(descRow);
    } else {
      descriptionLabel = null;
    }

    leftContainer.add(textPanel, BorderLayout.CENTER);
    add(leftContainer, BorderLayout.CENTER);

    String tooltip = title + (description != null && !description.isEmpty() ? " - " + description : "");
    setToolTipText(tooltip);

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

    attachMouseListener(leftContainer, adapter);
    addMouseListener(adapter);
  }

  /**
   * Sets whether this version card represents the currently selected project compiler.
   *
   * <p>Updates title label foreground color to the active hyperlink accent and triggers a repaint.
   * Must be called on the Event Dispatch Thread (EDT).
   *
   * @param active true if this version is the project's active compiler
   */
  public void setActive(boolean active) {
    this.isActive = active;
    if (active) {
      titleLabel.setForeground(JBUI.CurrentTheme.Link.Foreground.ENABLED);
    } else {
      titleLabel.setForeground(JBColor.foreground());
    }
    repaint();
  }

  /**
   * Recursively attaches the mouse listener adapter to all child components except the delete button,
   * ensuring clicks anywhere on the card surface trigger card selection or popup display.
   *
   * @param component the root component to attach to
   * @param adapter   the unified mouse adapter handling hover and click events
   */
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

  /**
   * Constructs and displays the contextual popup menu at the mouse trigger coordinates.
   *
   * <p>If installed, provides options to remove the version or copy the local binary path.
   * If uninstalled, provides an option to download and switch to this version.
   * In both cases, provides an action to copy the release asset download URL to the clipboard.
   *
   * @param e mouse event triggering the popup (pressed or released)
   */
  private void showContextMenu(MouseEvent e) {
    CompactVersionCardContextMenu.showContextMenu(this, e, version, isInstalled, installedPath, onSelect, onRemove);
  }

  /**
   * Returns the semantic version string associated with this card (e.g. {@code "0.24.0"}).
   */
  public String getVersion() {
    return version;
  }

  /**
   * Returns the primary header title text displayed on this card.
   */
  public @NotNull String getTitle() {
    return titleLabel.getText();
  }

  /**
   * Returns the optional description subtitle text, or {@code null} if omitted.
   */
  public @Nullable String getDescription() {
    return description;
  }

  /**
   * Returns true if this compiler version binary is downloaded and present in local storage.
   */
  public boolean isInstalled() {
    return isInstalled;
  }

  /**
   * Returns true if this card represents the currently selected project compiler.
   */
  public boolean isActive() {
    return isActive;
  }

  /**
   * Returns the absolute path to the local compiler binary on disk, or {@code null} if not installed.
   */
  public @Nullable String getInstalledPath() {
    return installedPath;
  }

  /**
   * Custom painting routine rendering smooth rounded rectangle borders and background fills
   * responsive to active, hovered, and default states.
   *
   * @param g graphics context
   */
  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    CompactVersionCardPainter.paintCard(g, this, isActive, isHovered);
  }
}
