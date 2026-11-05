package dev.verloren.midnight.toolwindow;

import com.intellij.icons.AllIcons;
import com.intellij.ide.HelpTooltipKt;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.util.text.HtmlChunk;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.ui.SimpleListCellRenderer;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import dev.verloren.midnight.run.CompactToolchainUtil;
import dev.verloren.midnight.settings.MidnightProjectSettings;
import dev.verloren.midnight.version.CompactSemVerUtil;
import dev.verloren.midnight.version.CompactVersionManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Modern, responsive Compact Compiler management and execution tool window panel.
 */
public class CompactCompilerPanel extends JPanel implements Disposable {
  private final Project project;
  private final ComboBox<ToolchainItem> versionComboBox = new ComboBox<>();
  private final CompactAdvancedOptionsPanel advancedOptionsPanel = new CompactAdvancedOptionsPanel();
  @SuppressWarnings("DialogTitleCapitalization")
  private final JBLabel advancedToggleLabel = new JBLabel("▶ Advanced Options");
  private final JButton compileButton = createCompileButton();
  private boolean isAdvancedExpanded = false;

  /**
   * Represents an item in the toolchain version dropdown.
   */
  public record ToolchainItem(@NotNull String version, @NotNull String display) {
    @Override
    public @NotNull String toString() {
      return display;
    }
  }

  /**
   * Initializes the compiler panel, constructs layout sections, and loads available versions.
   */
  @SuppressWarnings("this-escape")
  public CompactCompilerPanel(@NotNull Project project) {
    super(new GridBagLayout());
    this.project = project;

    setBorder(JBUI.Borders.empty(12));
    initLayout();
    initListeners();
    refreshVersions();
  }

  private void initLayout() {
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;

    addVersionHeaderSection(gbc);
    addAdvancedOptionsSection(gbc);
    addRunButtonSection(gbc);

    gbc.gridy++;
    gbc.weighty = 1.0;
    gbc.fill = GridBagConstraints.BOTH;
    add(new JPanel() {{ setOpaque(false); }}, gbc);
  }

  @SuppressWarnings("DialogTitleCapitalization")
  private void addVersionHeaderSection(@NotNull GridBagConstraints gbc) {
    JBLabel versionHeader = new JBLabel("Compiler Toolchain:");
    versionHeader.setFont(JBFont.label().asBold());
    add(versionHeader, gbc);

    gbc.gridy++;
    gbc.insets = JBUI.insetsTop(4);
    versionComboBox.setRenderer(new SimpleListCellRenderer<>() {
      @Override
      public void customize(
          @NotNull JList<? extends ToolchainItem> list,
          @Nullable ToolchainItem value,
          int index,
          boolean selected,
          boolean hasFocus
      ) {
        if (value != null) {
          setText(value.display());
        }
      }
    });
    add(versionComboBox, gbc);
  }

  private void addAdvancedOptionsSection(@NotNull GridBagConstraints gbc) {
    gbc.gridy++;
    gbc.insets = JBUI.insetsTop(12);
    advancedToggleLabel.setFont(JBFont.small().asBold());
    advancedToggleLabel.setForeground(JBColor.namedColor("Link.activeForeground", new JBColor(0x2470B8, 0x589DF6)));
    advancedToggleLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    add(advancedToggleLabel, gbc);

    gbc.gridy++;
    gbc.insets = JBUI.insetsTop(6);
    advancedOptionsPanel.setVisible(false);
    add(advancedOptionsPanel, gbc);
  }

  private void addRunButtonSection(@NotNull GridBagConstraints gbc) {
    gbc.gridy++;
    gbc.insets = JBUI.insetsTop(16);
    add(compileButton, gbc);
  }

  private void initListeners() {
    advancedToggleLabel.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        toggleAdvancedOptions();
      }
    });

    versionComboBox.addActionListener(_ -> {
      ToolchainItem selected = (ToolchainItem) versionComboBox.getSelectedItem();
      if (selected != null) {
        MidnightProjectSettings settings = MidnightProjectSettings.getInstance(project);
        if (settings != null) {
          settings.selectedCompilerVersion = selected.version();
        }
      }
    });

    compileButton.addActionListener(_ -> onCompileContract());
  }

  @SuppressWarnings("DialogTitleCapitalization")
  private void toggleAdvancedOptions() {
    isAdvancedExpanded = !isAdvancedExpanded;
    advancedToggleLabel.setText(isAdvancedExpanded ? "▼ Advanced Options" : "▶ Advanced Options");
    advancedOptionsPanel.setVisible(isAdvancedExpanded);
    revalidate();
    repaint();
  }

  /**
   * Refreshes the version list asynchronously and updates dropdown items on EDT.
   */
  public void refreshVersions() {
    ApplicationManager.getApplication().executeOnPooledThread(() -> {
      if (project.isDisposed()) return;
      List<ToolchainItem> items = collectSortedToolchainItems();
      String effective = resolveEffectiveVersion();

      ApplicationManager.getApplication().invokeLater(() -> {
        if (project.isDisposed()) return;
        updateComboBoxItems(items, effective);
      });
    });
  }

  private @NotNull List<ToolchainItem> collectSortedToolchainItems() {
    Set<String> allVersions = new LinkedHashSet<>(CompactVersionManager.KNOWN_VERSIONS);
    Map<String, String> installed = CompactVersionManager.getInstalledVersions();
    allVersions.addAll(installed.keySet());

    List<String> sorted = new ArrayList<>(allVersions);
    sorted.sort(CompactSemVerUtil.DESCENDING_COMPARATOR);

    List<ToolchainItem> items = new ArrayList<>();
    for (String v : sorted) {
      String langVer = CompactVersionManager.getLanguageVersionForToolchain(v);
      String ledger = CompactVersionManager.getLedgerVersionForToolchain(v);
      String display = "Compact v" + v + " (Lang " + langVer + (ledger != null ? " • " + ledger : "") + ")";
      items.add(new ToolchainItem(v, display));
    }
    return items;
  }

  private @NotNull String resolveEffectiveVersion() {
    MidnightProjectSettings settings = MidnightProjectSettings.getInstance(project);
    String selectedVer = settings != null && settings.selectedCompilerVersion != null
        ? settings.selectedCompilerVersion.trim()
        : "";
    String activeVer = CompactToolchainUtil.getActiveCompilerVersion(project);
    return !selectedVer.isEmpty() ? selectedVer : (activeVer != null ? activeVer : "");
  }

  private void updateComboBoxItems(@NotNull List<ToolchainItem> items, @NotNull String effective) {
    versionComboBox.removeAllItems();
    ToolchainItem selectedItem = null;
    for (ToolchainItem item : items) {
      versionComboBox.addItem(item);
      if (item.version().equals(effective)) {
        selectedItem = item;
      }
    }
    if (selectedItem != null) {
      versionComboBox.setSelectedItem(selectedItem);
    } else if (!items.isEmpty()) {
      versionComboBox.setSelectedIndex(0);
    }
  }

  /**
   * Resolves the active .compact contract file from currently open editor tabs.
   */
  public @Nullable VirtualFile findTargetCompactFile() {
    VirtualFile[] selected = FileEditorManager.getInstance(project).getSelectedFiles();
    for (VirtualFile f : selected) {
      if (f != null && f.isValid() && "compact".equalsIgnoreCase(f.getExtension())) {
        return f;
      }
    }
    VirtualFile[] open = FileEditorManager.getInstance(project).getOpenFiles();
    for (VirtualFile f : open) {
      if (f != null && f.isValid() && "compact".equalsIgnoreCase(f.getExtension())) {
        return f;
      }
    }
    return null;
  }

  private void onCompileContract() {
    VirtualFile file = findTargetCompactFile();
    if (file == null) {
      CompactCompilerRunner.notifyUser(
          project,
          "No active Compact (.compact) contract found. Please open or create a .compact file to compile.",
          NotificationType.WARNING
      );
      return;
    }

    CompactCompilerRunner.compileContract(
        project,
        file,
        advancedOptionsPanel.isSkipZkSelected(),
        advancedOptionsPanel.getOutputDirectory(),
        advancedOptionsPanel.getCustomFlags()
    );
  }

  private JButton createCompileButton() {
    JButton btn = new JButton("Run Contract", AllIcons.Actions.Execute) {
      @Override
      protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
          g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
          int arc = JBUI.scale(8);
          int w = getWidth();
          int h = getHeight();

          if (getModel().isPressed()) {
            g2.setColor(new JBColor(new Color(46, 160, 67, 50), new Color(63, 185, 80, 60)));
          } else if (getModel().isRollover()) {
            g2.setColor(new JBColor(new Color(46, 160, 67, 26), new Color(63, 185, 80, 32)));
          } else {
            g2.setColor(new JBColor(new Color(46, 160, 67, 12), new Color(63, 185, 80, 18)));
          }
          g2.fillRoundRect(1, 1, w - 2, h - 2, arc, arc);

          g2.setColor(new JBColor(new Color(46, 160, 67), new Color(63, 185, 80)));
          g2.setStroke(new BasicStroke(JBUIScale.scale(1.5f)));
          g2.drawRoundRect(1, 1, w - 3, h - 3, arc, arc);
        } finally {
          g2.dispose();
        }
        super.paintComponent(g);
      }
    };
    btn.setFont(JBFont.label().asBold());
    btn.setForeground(new JBColor(new Color(36, 138, 61), new Color(63, 185, 80)));
    btn.setContentAreaFilled(false);
    btn.setFocusPainted(false);
    btn.setBorder(JBUI.Borders.empty(8, 14));
    btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    btn.setPreferredSize(new Dimension(0, JBUI.scale(36)));
    HelpTooltipKt.setToolTipText(btn, HtmlChunk.text("Compile active Compact contract"));
    return btn;
  }

  public @NotNull ComboBox<ToolchainItem> getVersionComboBox() {
    return versionComboBox;
  }

  public boolean isAdvancedExpanded() {
    return isAdvancedExpanded;
  }

  public void setAdvancedExpanded(boolean expanded) {
    if (this.isAdvancedExpanded != expanded) {
      toggleAdvancedOptions();
    }
  }

  public boolean isSkipZkSelected() {
    return advancedOptionsPanel.isSkipZkSelected();
  }

  public @NotNull String getOutputDirectory() {
    return advancedOptionsPanel.getOutputDirectory();
  }

  public @NotNull String getCustomFlags() {
    return advancedOptionsPanel.getCustomFlags();
  }

  @Override
  public void dispose() {
    // Disposal hook for subcomponents if needed
  }
}
