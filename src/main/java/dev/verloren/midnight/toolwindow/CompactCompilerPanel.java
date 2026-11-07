package dev.verloren.midnight.toolwindow;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.SimpleListCellRenderer;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import dev.verloren.midnight.run.CompactToolchainUtil;
import dev.verloren.midnight.settings.MidnightProjectSettings;
import dev.verloren.midnight.version.CompactSemVerUtil;
import dev.verloren.midnight.version.CompactVersionManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.Scrollable;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Modern, responsive Compact Compiler management tool window panel.
 *
 * <p>Serves as the dedicated toolchain selection panel in the IDE sidebar, providing
 * version discovery, installation status visualization, and fallback handling. Contract
 * compilation is delegated to standard IntelliJ Run Configurations.
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li>UI components, listeners, and model updates execute strictly on the Event Dispatch Thread (EDT).</li>
 *   <li>Toolchain version queries and file system lookups run on pooled background threads.</li>
 * </ul>
 */
public class CompactCompilerPanel extends JPanel implements Disposable, Scrollable {
  private final Project project;
  private final ComboBox<ToolchainItem> versionComboBox = new ComboBox<>();
  private final CompactToolchainSelectionHandler selectionHandler;
  private boolean isUpdatingComboBox = false;

  /**
   * Represents an item in the toolchain version dropdown.
   */
  public record ToolchainItem(
          @NotNull String version,
          @NotNull String display,
          boolean isInstalled
  ) {
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
    this.selectionHandler = new CompactToolchainSelectionHandler(project, versionComboBox, this::refreshVersions);

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

    gbc.gridy++;
    gbc.weighty = 1.0;
    gbc.fill = GridBagConstraints.BOTH;
    add(new JPanel() {{
      setOpaque(false);
    }}, gbc);
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
          setIcon(value.isInstalled() ? AllIcons.Actions.Checked : AllIcons.Actions.Download);
        }
      }
    });
    add(versionComboBox, gbc);
  }

  private void initListeners() {
    versionComboBox.addActionListener(_ -> {
      if (isUpdatingComboBox) {
        return;
      }
      ToolchainItem selected = (ToolchainItem) versionComboBox.getSelectedItem();
      if (selected != null) {
        selectionHandler.handleSelection(selected, this::runWithoutComboEvents);
      }
    });
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
      boolean isInstalled = installed.containsKey(v);
      String langVer = CompactVersionManager.getLanguageVersionForToolchain(v);
      String ledger = CompactVersionManager.getLedgerVersionForToolchain(v);
      String display = "Compact v" + v + " (Lang " + langVer + (ledger != null ? " • " + ledger : "") + ")";
      items.add(new ToolchainItem(v, display, isInstalled));
    }
    return items;
  }

  @SuppressWarnings("DuplicatedCode")
  private @NotNull String resolveEffectiveVersion() {
    MidnightProjectSettings settings = MidnightProjectSettings.getInstance(project);
    String selectedVer = settings != null && settings.selectedCompilerVersion != null
            ? settings.selectedCompilerVersion.trim()
            : "";
    String activeVer = CompactToolchainUtil.getActiveCompilerVersion(project);
    return !selectedVer.isEmpty() ? selectedVer : (activeVer != null ? activeVer : "");
  }

  private void updateComboBoxItems(@NotNull List<ToolchainItem> items, @NotNull String effective) {
    isUpdatingComboBox = true;
    try {
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
    } finally {
      isUpdatingComboBox = false;
    }
  }

  private void runWithoutComboEvents(@NotNull Runnable action) {
    isUpdatingComboBox = true;
    try {
      action.run();
    } finally {
      isUpdatingComboBox = false;
    }
  }

  public @NotNull ComboBox<ToolchainItem> getVersionComboBox() {
    return versionComboBox;
  }

  @Override
  public Dimension getPreferredScrollableViewportSize() {
    return getPreferredSize();
  }

  @Override
  public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
    return JBUI.scale(16);
  }

  @Override
  public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
    return JBUI.scale(48);
  }

  @Override
  public boolean getScrollableTracksViewportWidth() {
    return true;
  }

  @Override
  public boolean getScrollableTracksViewportHeight() {
    return false;
  }

  @Override
  public void dispose() {
    // Disposal hook for subcomponents if needed
  }
}
