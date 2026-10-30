package dev.verloren.midnight.toolwindow;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.ProgramRunnerUtil;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessListener;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.icons.AllIcons;
import com.intellij.ide.HelpTooltipKt;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.text.HtmlChunk;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.util.Alarm;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import dev.verloren.midnight.annotator.CompactProblemUtil;
import dev.verloren.midnight.psi.CompactFile;
import dev.verloren.midnight.psi.CompactPragmaForm;
import dev.verloren.midnight.run.CompactConfigurationType;
import dev.verloren.midnight.run.CompactRunConfiguration;
import dev.verloren.midnight.run.CompactToolchainUtil;
import dev.verloren.midnight.settings.MidnightProjectSettings;
import dev.verloren.midnight.settings.MidnightSettingsState;
import dev.verloren.midnight.version.CompactSemVerUtil;
import dev.verloren.midnight.version.CompactVersionManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Modern Compact Compiler management and execution tool window panel.
 *
 * <p><h3>Architectural Overview</h3>
 * Inspired by the Ethereum Remix IDE Solidity compiler sidebar, this panel acts as the central
 * developer dashboard for Midnight Compact smart contracts within IntelliJ IDEA. It unifies:
 * <ul>
 *   <li><b>Active Contract Status:</b> Tracks the currently focused {@code .compact} file, extracts its
 *       {@code pragma language_version} or {@code pragma compiler_version} constraint via live AST inspection,
 *       and validates whether the active compiler toolchain satisfies the contract requirements.</li>
 *   <li><b>Compilation Execution:</b> Provides an instant "Run Contract" action that generates an ephemeral
 *       {@link CompactRunConfiguration} or falls back to direct execution via
 *       {@link CompactToolchainUtil#createCommandLine(Project, List, String)}, supporting flags such as {@code --skip-zk}.</li>
 *   <li><b>Toolchain Version Manager:</b> Displays a real-time list of installed and publicly available Compact
 *       compiler versions via {@link CompactVersionCard} widgets, supporting one-click downloading, version switching,
 *       and uninstallation with zero UI latency.</li>
 * </ul>
 *
 * <p><h3>Visual Structure &amp; Layout</h3>
 * <pre>
 *   ┌────────────────────────────────────────────────────────────────────────┐
 *   │ [TOP PANEL]                                                            │
 *   │   Contract.compact                                  pragma ^0.24.0     │
 *   │   Compiler v0.24.0 (Language v0.2.0) satisfies pragma (^0.24.0)        │
 *   │   [ ] Skip ZK proof generation (--skip-zk)         [▶ Run Contract]    │
 *   ├────────────────────────────────────────────────────────────────────────┤
 *   │ COMPACT COMPILER VERSIONS                                              │
 *   │ ┌────────────────────────────────────────────────────────────────────┐ │
 *   │ │ [🗑] Compact v0.24.0 (Language v0.2.0)            ✔ pragma match    │ │
 *   │ └────────────────────────────────────────────────────────────────────┘ │
 *   │ ┌────────────────────────────────────────────────────────────────────┐ │
 *   │ │ [⬇] Compact v0.23.0                                                │ │
 *   │ └────────────────────────────────────────────────────────────────────┘ │
 *   └────────────────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * <p><h3>Event Subscriptions &amp; Calling Pipelines</h3>
 * The panel listens to multiple IDE events to maintain live synchronization:
 * <ol>
 *   <li><b>Active File Changes:</b> Subscribes to {@link FileEditorManagerListener#FILE_EDITOR_MANAGER}
 *       to automatically track the newly focused {@code .compact} document.</li>
 *   <li><b>Live Document Typing:</b> Registers an {@link EditorFactory} document listener with a 300ms
 *       debounced {@link Alarm}. Edits past character 250 that do not modify pragma declarations are skipped
 *       to ensure zero EDT typing latency.</li>
 *   <li><b>Toolchain Version Switching:</b> Subscribes to {@link CompactCompilerEventListener#TOPIC}
 *       to synchronize card selection and pragma badges when compiler changes occur from the status bar or settings.</li>
 * </ol>
 *
 * <p><h3>Threading Contract &amp; Concurrency</h3>
 * <ul>
 *   <li><b>EDT (Event Dispatch Thread):</b> All Swing layout, card addition, active file labels, and button actions.</li>
 *   <li><b>Pooled Background Threads:</b> {@link #refreshCards()} collects known releases, scans local disk directories,
 *       and executes SemVer sorting off the UI thread, dispatching results back to EDT via {@code invokeLater}.</li>
 *   <li><b>Read Action:</b> AST inspection of {@link CompactPragmaForm} is safely wrapped inside
 *       {@link ReadAction#computeBlocking(com.intellij.openapi.util.ThrowableComputable)}.</li>
 *   <li><b>Background Progress Tasks:</b> Compiler downloads execute inside {@link Task.Backgroundable}
 *       with cancellation support and indeterminate progress indicators.</li>
 * </ul>
 *
 * @see CompactCompilerToolWindowFactory
 * @see CompactVersionCard
 * @see CompactCompilerEventListener
 * @see dev.verloren.midnight.version.CompactVersionManager
 */
public class CompactCompilerPanel extends JPanel implements Disposable {
  private static final Logger LOG = Logger.getInstance(CompactCompilerPanel.class);

  /** Active project reference. */
  private final Project project;

  /** Container panel hosting the vertically stacked {@link CompactVersionCard} items. */
  private final JPanel cardsPanel;

  /** Header label displaying the active {@code .compact} file name. */
  private final JBLabel activeFileNameLabel = new JBLabel();

  /** Badge label displaying the active pragma constraint string (e.g. {@code "pragma ^0.2.0"}). */
  private final JBLabel pragmaBadge = new JBLabel();

  /** Status label displaying whether the active compiler satisfies the file's pragma requirement. */
  private final JBLabel compilerStatusLabel = new JBLabel();

  /** Debounce alarm used to throttle pragma reparsing during rapid document editing. */
  private final Alarm pragmaUpdateAlarm;

  /**
   * Run the action button with a green outline and icon. Always interactive and never disabled.
   */
  private final JButton compileButton = new JButton("Run Contract") {
    @Override
    protected void paintComponent(Graphics g) {
      Graphics2D g2 = (Graphics2D) g.create();
      try {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int arc = JBUI.scale(8);
        int w = getWidth();
        int h = getHeight();

        // Subtle background tint on hover / press
        if (getModel().isPressed()) {
          g2.setColor(new JBColor(new Color(46, 160, 67, 45), new Color(63, 185, 80, 55)));
        } else if (getModel().isRollover()) {
          g2.setColor(new JBColor(new Color(46, 160, 67, 26), new Color(63, 185, 80, 32)));
        } else {
          g2.setColor(new JBColor(new Color(46, 160, 67, 12), new Color(63, 185, 80, 18)));
        }
        g2.fillRoundRect(1, 1, w - 2, h - 2, arc, arc);

        // Prominent Green Outline
        g2.setColor(new JBColor(new Color(46, 160, 67), new Color(63, 185, 80)));
        g2.setStroke(new BasicStroke(JBUIScale.scale(1.5f)));
        g2.drawRoundRect(1, 1, w - 3, h - 3, arc, arc);
      } finally {
        g2.dispose();
      }
      super.paintComponent(g);
    }
  };

  /** Checkbox toggling whether to pass {@code --skip-zk} during compilation to bypass zero-knowledge proving key generation. */
  private final JBCheckBox skipZkCheckBox = new JBCheckBox("Skip ZK proof generation (--skip-zk)");

  /** Concurrent set tracking version strings currently undergoing background download. */
  private final Set<String> downloadingVersions = ConcurrentHashMap.newKeySet();

  /** Cached pragma constraint text extracted from the active file (e.g., {@code "^0.2.0"}). */
  private String currentPragmaConstraint = null;

  /** True if the active pragma constraint targets {@code compiler_version} rather than {@code language_version}. */
  private boolean currentPragmaIsCompilerVersion = false;

  /**
   * Initializes the compiler panel, constructs layout sections, attaches editor event listeners,
   * and triggers the initial file and compiler version discovery.
   *
   * @param project an active IntelliJ project
   */
  @SuppressWarnings("this-escape")
  public CompactCompilerPanel(@NotNull Project project) {
    super(new BorderLayout());
    this.project = project;
    this.pragmaUpdateAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD, this);

    setBorder(JBUI.Borders.empty(8));

    // Top: Active Contract Status & Compilation Card
    JPanel topPanel = createTopPanel();
    add(topPanel, BorderLayout.NORTH);

    // Centre: Compact Compiler Versions Manager
    JPanel versionsContainer = new JPanel(new BorderLayout());
    versionsContainer.setBorder(JBUI.Borders.emptyTop(10));

    JBLabel sectionHeader = new JBLabel("COMPACT COMPILER VERSIONS");
    sectionHeader.setFont(JBFont.small().asBold());
    sectionHeader.setForeground(JBColor.GRAY);
    sectionHeader.setBorder(JBUI.Borders.empty(4, 2, 8, 2));
    versionsContainer.add(sectionHeader, BorderLayout.NORTH);

    cardsPanel = new JPanel();
    cardsPanel.setLayout(new BoxLayout(cardsPanel, BoxLayout.Y_AXIS));
    cardsPanel.setOpaque(false);

    JBScrollPane scrollPane = new JBScrollPane(cardsPanel);
    scrollPane.setBorder(JBUI.Borders.empty());
    scrollPane.setOpaque(false);
    scrollPane.getViewport().setOpaque(false);
    versionsContainer.add(scrollPane, BorderLayout.CENTER);

    add(versionsContainer, BorderLayout.CENTER);

    // Initial update of active file and compiler cards
    updateActiveFileInfo();

    // Listen to active editor changes to keep pragma and file info in sync immediately
    project.getMessageBus().connect(this).subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerListener() {
      @Override
      public void selectionChanged(@NotNull FileEditorManagerEvent event) {
        updateActiveFileInfo(event.getNewFile());
      }

      @Override
      public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        updateActiveFileInfo(file);
      }

      @Override
      public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        updateActiveFileInfo(null);
      }
    });

    // Listen to compiler version changes or project-level switches
    project.getMessageBus().connect(this).subscribe(CompactCompilerEventListener.TOPIC, (CompactCompilerEventListener) () -> {
      updateCardSelection();
      updateActiveFileInfo(null);
      refreshCards();
    });

    // Listen to document edits in open .compact files so pragma changes update live
    // Debounced and filtered so general typing never blocks EDT or triggers card churn
    EditorFactory.getInstance().getEventMulticaster().addDocumentListener(new DocumentListener() {
      @Override
      public void documentChanged(@NotNull DocumentEvent event) {
        VirtualFile file = FileDocumentManager.getInstance().getFile(event.getDocument());
        if (file == null || !file.isValid() || !"compact".equalsIgnoreCase(file.getExtension())) {
          return;
        }

        // Fast check: pragma declaration is always at the top of a Compact contract.
        // If the modification is past the first 250 characters and doesn't mention "pragma", ignore.
        CharSequence oldFrag = event.getOldFragment();
        CharSequence newFrag = event.getNewFragment();
        boolean mentionsPragma = oldFrag.toString().contains("pragma")
            || newFrag.toString().contains("pragma");
        boolean nearTop = event.getOffset() < 250;
        if (!mentionsPragma && !nearTop) {
          return;
        }

        pragmaUpdateAlarm.cancelAllRequests();
        pragmaUpdateAlarm.addRequest(() -> {
          if (!project.isDisposed()) {
            updateActiveFileInfo(file);
          }
        }, 300);
      }
    }, this);
  }

  /**
   * Disposes background resources, cancelling any pending pragma debouncing alarm requests.
   */
  @Override
  public void dispose() {
    pragmaUpdateAlarm.cancelAllRequests();
  }

  /**
   * Constructs the top status card panel displaying current contract details, pragma compatibility,
   * proof generation toggles, and the "Run Contract" button.
   *
   * @return the configured top panel component
   */
  private JPanel createTopPanel() {
    JPanel card = new JPanel(new BorderLayout(0, JBUI.scale(6))) {
      @Override
      protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
          g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
          g2.setColor(new JBColor(new Color(0, 0, 0, 10), new Color(255, 255, 255, 12)));
          g2.fillRoundRect(0, 0, getWidth(), getHeight(), JBUI.scale(8), JBUI.scale(8));
          g2.setColor(JBColor.border());
          g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, JBUI.scale(8), JBUI.scale(8));
        } finally {
          g2.dispose();
        }
        super.paintComponent(g);
      }
    };
    card.setOpaque(false);
    card.setBorder(JBUI.Borders.empty(10));

    // Active File Header row
    JPanel fileRow = new JPanel(new BorderLayout(JBUI.scale(8), 0));
    fileRow.setOpaque(false);

    activeFileNameLabel.setFont(JBFont.label().asBold());
    fileRow.add(activeFileNameLabel, BorderLayout.WEST);

    pragmaBadge.setFont(JBFont.small());
    pragmaBadge.setForeground(JBColor.GRAY);
    fileRow.add(pragmaBadge, BorderLayout.EAST);
    card.add(fileRow, BorderLayout.NORTH);

    // Compiler compatibility status line
    compilerStatusLabel.setFont(JBFont.small());
    card.add(compilerStatusLabel, BorderLayout.CENTER);

    // Compilation Action Controls
    JPanel actionRow = new JPanel(new BorderLayout(JBUI.scale(8), 0));
    actionRow.setOpaque(false);

    skipZkCheckBox.setOpaque(false);
    actionRow.add(skipZkCheckBox, BorderLayout.WEST);

    // Styled Run button with green outline and play icon
    compileButton.setFont(JBFont.label().asBold());
    compileButton.setIcon(AllIcons.Actions.Execute);
    compileButton.setForeground(new JBColor(new Color(36, 138, 61), new Color(63, 185, 80)));
    compileButton.setContentAreaFilled(false);
    compileButton.setFocusPainted(false);
    compileButton.setBorder(JBUI.Borders.empty(4, 12));
    compileButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    HelpTooltipKt.setToolTipText(compileButton, HtmlChunk.text("Run Compact compiler on contract"));
    compileButton.addActionListener(_ -> onCompileContract());
    actionRow.add(compileButton, BorderLayout.EAST);

    card.add(actionRow, BorderLayout.SOUTH);
    return card;
  }

  /**
   * Discovers and refreshes the list of compiler version cards asynchronously on a pooled thread,
   * sorting them descending by SemVer and rebuilding the card list on EDT.
   */
  public void refreshCards() {
    MidnightProjectSettings settings = MidnightProjectSettings.getInstance(project);
    String selectedVer = settings.selectedCompilerVersion != null ? settings.selectedCompilerVersion.trim() : "";
    String activeCompilerVer = CompactToolchainUtil.getActiveCompilerVersion(project);
    String effectiveActive = !selectedVer.isEmpty() ? selectedVer : (activeCompilerVer != null ? activeCompilerVer : "");
    String pragmaConstraint = currentPragmaConstraint;
    boolean pragmaIsCompiler = currentPragmaIsCompilerVersion;

    ApplicationManager.getApplication().executeOnPooledThread(() -> {
      if (project.isDisposed()) return;

      // 1. Collect all known and locally installed compiler versions of EDT
      Set<String> allVersionKeys = new LinkedHashSet<>(CompactVersionManager.KNOWN_VERSIONS);
      Map<String, String> installedMap = CompactVersionManager.getInstalledVersions();
      allVersionKeys.addAll(installedMap.keySet());

      // 2. Sort descending by SemVer
      List<String> sortedVersions = new ArrayList<>(allVersionKeys);
      sortedVersions.sort(CompactSemVerUtil.DESCENDING_COMPARATOR);

      /*
        Immutable value carrier holding pre-calculated card layout attributes off EDT.
       */
      record CardData(
          String version,
          String title,
          String description,
          boolean isInstalled,
          boolean isActive,
          boolean isPragmaMatch,
          boolean isDownloading,
          String installedPath
      ) {}

      // 3. Populate CardData models off EDT
      List<CardData> cardsData = new ArrayList<>();
      for (String v : sortedVersions) {
        boolean isInstalled = installedMap.containsKey(v) || CompactVersionManager.isVersionInstalled(v);
        boolean isActive = v.equals(effectiveActive);
        boolean isDownloading = downloadingVersions.contains(v);
        String installedPath = installedMap.get(v);

        String cardTitle = "Compact v" + v;
        String desc = CompactVersionManager.getVersionDescription(v);

        String langVer = CompactVersionManager.getLanguageVersionForToolchain(v);
        String versionToCheck = pragmaIsCompiler ? v : langVer;
        boolean isPragmaMatch = pragmaConstraint != null &&
            CompactSemVerUtil.satisfiesConstraint(versionToCheck, pragmaConstraint);

        cardsData.add(new CardData(v, cardTitle, desc, isInstalled, isActive, isPragmaMatch, isDownloading, installedPath));
      }

      // 4. Dispatch rebuilt card components onto Swing EDT
      ApplicationManager.getApplication().invokeLater(() -> {
        if (project.isDisposed()) return;

        cardsPanel.removeAll();
        for (CardData cd : cardsData) {
          CompactVersionCard card = new CompactVersionCard(
              cd.version(),
              cd.title(),
              cd.description(),
              cd.isInstalled(),
              cd.isActive(),
              cd.isPragmaMatch(),
              cd.isDownloading(),
              cd.installedPath(),
              () -> {
                if (cd.isInstalled()) {
                  selectVersionImmediately(cd.version());
                } else {
                  downloadAndActivateVersion(cd.version());
                }
              },
              () -> onRemoveVersion(cd.version(), cd.installedPath())
          );

          cardsPanel.add(card);
          cardsPanel.add(Box.createVerticalStrut(JBUI.scale(4)));
        }

        cardsPanel.revalidate();
        cardsPanel.repaint();
      }, ModalityState.any());
    });
  }

  /**
   * Instantly synchronizes the visual active state across all displayed version cards on EDT.
   */
  public void updateCardSelection() {
    MidnightProjectSettings settings = MidnightProjectSettings.getInstance(project);
    String selectedVer = (settings != null && settings.selectedCompilerVersion != null)
        ? settings.selectedCompilerVersion.trim()
        : "";
    String activeCompilerVer = CompactToolchainUtil.getActiveCompilerVersion(project);
    String effectiveActive = !selectedVer.isEmpty()
        ? selectedVer
        : (activeCompilerVer != null ? activeCompilerVer : "");

    for (Component c : cardsPanel.getComponents()) {
      if (c instanceof CompactVersionCard card) {
        card.setActive(card.getVersion().equals(effectiveActive));
      }
    }
  }

  /**
   * Instantly updates UI selection on EDT with zero perceived latency,
   * then updates settings, triggers background reparsing.
   *
   * @param version the semantic version to activate
   */
  private void selectVersionImmediately(@NotNull String version) {
    MidnightProjectSettings.getInstance(project).selectedCompilerVersion = version;

    String installedExe = CompactVersionManager.getInstalledExecutable(version);
    if (installedExe != null) {
      MidnightSettingsState state = MidnightSettingsState.getInstance();
      if (state != null) {
        state.compilerPath = installedExe;
      }
    }

    // Immediately update active status on all card components
    updateCardSelection();

    updateActiveFileInfo();
    restartDaemon();
    notifyUser("Switched project compiler to Compact v" + version + ".", NotificationType.INFORMATION);
  }

  /**
   * Prompts the user with a confirmation dialogue before downloading and activating a compiler version.
   *
   * @param version semantic version to download
   */
  private void downloadAndActivateVersion(@NotNull String version) {
    if (downloadingVersions.contains(version)) {
      return;
    }

    // Confirmation dialogue: do not start downloading on a single accidental click
    int confirm = Messages.showYesNoDialog(
        this,
        "Do you want to download and install Compact compiler v" + version + "?",
        "Download Compact Compiler",
        Messages.getQuestionIcon()
    );
    if (confirm != Messages.YES) {
      return;
    }

    downloadingVersions.add(version);
    refreshCards();

    ProgressManager.getInstance().run(new Task.Backgroundable(project, "Downloading Compact Compiler v" + version, true) {
      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        boolean ok = CompactVersionManager.installVersion(version, project.getBasePath(), indicator);
        ApplicationManager.getApplication().invokeLater(() -> {
          downloadingVersions.remove(version);
          if (ok) {
            selectVersionImmediately(version);
            refreshCards();
            notifyUser("Compact Compiler v" + version + " downloaded & activated.", NotificationType.INFORMATION);
          } else {
            refreshCards();
            notifyUser("Failed to download Compact Compiler v" + version + ". Please check your network connection.", NotificationType.ERROR);
          }
        }, ModalityState.any());
      }
    });
  }

  /**
   * Prompts the user with a confirmation dialogue before uninstalling and deleting a compiler binary.
   *
   * @param version semantic version to delete
   * @param path    local filesystem path of the binary
   */
  private void onRemoveVersion(@NotNull String version, @Nullable String path) {
    String msg = "Are you sure you want to remove Compact compiler v" + version + " from local storage?";
    if (path != null) {
      msg += "\nLocation: " + path;
    }
    int confirm = Messages.showYesNoDialog(
        this,
        msg,
        "Remove Compact Compiler",
        Messages.getQuestionIcon()
    );

    if (confirm == Messages.YES) {
      CompactVersionManager.uninstallVersion(version);
      MidnightProjectSettings settings = MidnightProjectSettings.getInstance(project);
      if (version.equals(settings.selectedCompilerVersion)) {
        settings.selectedCompilerVersion = "";
      }
      refreshCards();
      updateActiveFileInfo();
      restartDaemon();
      notifyUser("Removed Compact Compiler v" + version + " from disk.", NotificationType.INFORMATION);
    }
  }

  /**
   * Intelligently resolves the target {@code .compact} file:
   * <ol>
   *   <li>Currently focused editor if it represents a {@code .compact} file.</li>
   *   <li>Any currently open editor tabs containing a {@code .compact} file.</li>
   * </ol>
   *
   * @return the resolved {@link VirtualFile}, or {@code null} if no Compact contract is currently open
   */
  public @Nullable VirtualFile findTargetCompactFile() {
    // 1. Check the currently focused editor file
    VirtualFile[] selected = FileEditorManager.getInstance(project).getSelectedFiles();
    for (VirtualFile f : selected) {
      if (f != null && f.isValid() && "compact".equalsIgnoreCase(f.getExtension())) {
        return f;
      }
    }

    // 2. Fall back to any open editor file
    VirtualFile[] open = FileEditorManager.getInstance(project).getOpenFiles();
    for (VirtualFile f : open) {
      if (f != null && f.isValid() && "compact".equalsIgnoreCase(f.getExtension())) {
        return f;
      }
    }

    return null;
  }

  /**
   * Updates file details and pragma compatibility checks using the default resolved target file.
   */
  public void updateActiveFileInfo() {
    updateActiveFileInfo(null);
  }

  /**
   * Updates contract filename, extracts pragma version constraints via AST inspection,
   * evaluates active compiler compatibility, and updates UI status labels on EDT.
   *
   * @param preferredFile explicitly provided file, or {@code null} to resolve automatically
   */
  public void updateActiveFileInfo(@Nullable VirtualFile preferredFile) {
    ApplicationManager.getApplication().invokeLater(() -> {
      if (project.isDisposed()) return;

      VirtualFile file = (preferredFile != null && preferredFile.isValid() && "compact".equalsIgnoreCase(preferredFile.getExtension()))
          ? preferredFile
          : findTargetCompactFile();

      updateCardSelection();

      if (file == null) {
        activeFileNameLabel.setText("No Compact contract");
        pragmaBadge.setText("Open or create .compact");
        compilerStatusLabel.setText("Active Compiler: " + getActiveCompilerDisplay());
        compilerStatusLabel.setForeground(JBColor.GRAY);
        boolean pragmaChanged = (currentPragmaConstraint != null || currentPragmaIsCompilerVersion);
        currentPragmaConstraint = null;
        currentPragmaIsCompilerVersion = false;
        compileButton.setEnabled(true);
        if (pragmaChanged) {
          refreshCards();
        }
        return;
      }

      activeFileNameLabel.setText(file.getName());
      compileButton.setEnabled(true);

      // Safe ReadAction for PSI access
      CompactPragmaForm pragma = ReadAction.computeBlocking(() -> {
        PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
        return (psiFile instanceof CompactFile)
            ? PsiTreeUtil.findChildOfType(psiFile, CompactPragmaForm.class)
            : null;
      });

      boolean pragmaChanged;
      if (pragma != null) {
        String constraint = pragma.getConstraintText();
        if (constraint == null || constraint.isEmpty()) {
          constraint = pragma.getRequiredVersion();
        }
        if (constraint != null) {
          PsiElement id = pragma.getPragmaIdentifier();
          String idText = id != null ? id.getText() : "language_version";
          boolean isCompilerVer = "compiler_version".equals(idText);

          pragmaChanged = !Objects.equals(constraint, currentPragmaConstraint)
              || (isCompilerVer != currentPragmaIsCompilerVersion);

          currentPragmaConstraint = constraint;
          currentPragmaIsCompilerVersion = isCompilerVer;
          pragmaBadge.setText("pragma " + constraint);

          MidnightProjectSettings settings = MidnightProjectSettings.getInstance(project);
          String selectedVer = (settings != null && settings.selectedCompilerVersion != null)
              ? settings.selectedCompilerVersion.trim()
              : "";
          String activeVer = !selectedVer.isEmpty()
              ? selectedVer
              : CompactToolchainUtil.getActiveCompilerVersion(project);

          if (activeVer != null) {
            String targetVer = currentPragmaIsCompilerVersion
                ? activeVer
                : CompactVersionManager.getLanguageVersionForToolchain(activeVer);
            boolean satisfies = CompactSemVerUtil.satisfiesConstraint(targetVer, constraint);

            if (satisfies) {
              String labelText = currentPragmaIsCompilerVersion
                  ? "Compiler v" + activeVer + " satisfies pragma (" + constraint + ")"
                  : "Compiler v" + activeVer + " (Language v" + targetVer + ") satisfies pragma (" + constraint + ")";
              compilerStatusLabel.setText(labelText);
              compilerStatusLabel.setForeground(new JBColor(new Color(36, 138, 61), new Color(63, 185, 80)));
            } else {
              String labelText = currentPragmaIsCompilerVersion
                  ? "Compiler v" + activeVer + " does not satisfy pragma (" + constraint + ")"
                  : "Compiler v" + activeVer + " (Language v" + targetVer + ") does not satisfy pragma (" + constraint + ")";
              compilerStatusLabel.setText(labelText);
              compilerStatusLabel.setForeground(JBColor.RED);
            }
          } else {
            compilerStatusLabel.setText("Active Compiler: " + getActiveCompilerDisplay());
            compilerStatusLabel.setForeground(JBColor.GRAY);
          }
        } else {
          pragmaChanged = (currentPragmaConstraint != null || currentPragmaIsCompilerVersion);
          currentPragmaConstraint = null;
          currentPragmaIsCompilerVersion = false;
          pragmaBadge.setText("No pragma declared");
          compilerStatusLabel.setText("Active Compiler: " + getActiveCompilerDisplay());
          compilerStatusLabel.setForeground(JBColor.GRAY);
        }
      } else {
        pragmaChanged = (currentPragmaConstraint != null || currentPragmaIsCompilerVersion);
        currentPragmaConstraint = null;
        currentPragmaIsCompilerVersion = false;
        pragmaBadge.setText("No pragma declared");
        compilerStatusLabel.setText("Active Compiler: " + getActiveCompilerDisplay());
        compilerStatusLabel.setForeground(JBColor.GRAY);
      }

      // Refresh cards dynamically only when pragma constraint actually changed
      if (pragmaChanged) {
        refreshCards();
      }
    }, ModalityState.any());
  }

  /**
   * Computes human-readable display text for the active compiler and associated language version.
   *
   * @return formatted string (e.g., {@code "v0.24.0 (Language v0.2.0)"})
   */
  private String getActiveCompilerDisplay() {
    MidnightProjectSettings settings = MidnightProjectSettings.getInstance(project);
    String ver = (settings != null && settings.selectedCompilerVersion != null && !settings.selectedCompilerVersion.trim().isEmpty())
        ? settings.selectedCompilerVersion.trim()
        : CompactToolchainUtil.getActiveCompilerVersion(project);
    if (ver != null) {
      String langVer = CompactVersionManager.getLanguageVersionForToolchain(ver);
      return "v" + ver + " (Language v" + langVer + ")";
    }
    return "Not configured";
  }

  /**
   * Compiles the active contract file by generating a temporary run configuration,
   * falling back to direct process execution if configuration generation fails.
   */
  private void onCompileContract() {
    VirtualFile file = findTargetCompactFile();
    if (file == null) {
      notifyUser("No active Compact (.compact) contract file found in the project. Please open or create a .compact file to run.", NotificationType.WARNING);
      return;
    }

    try {
      RunManager runManager = RunManager.getInstance(project);
      ConfigurationFactory factory = CompactConfigurationType.getInstance().getConfigurationFactories()[0];
      RunnerAndConfigurationSettings settings = runManager.createConfiguration("Compile " + file.getName(), factory);
      CompactRunConfiguration configuration = (CompactRunConfiguration) settings.getConfiguration();
      configuration.setCompactFilePath(file.getPath());
      configuration.setSkipZk(skipZkCheckBox.isSelected());
      runManager.setTemporaryConfiguration(settings);

      ProgramRunnerUtil.executeConfiguration(settings, DefaultRunExecutor.getRunExecutorInstance());
    } catch (Exception e) {
      LOG.warn("Failed to launch run configuration, falling back to direct process execution", e);
      executeDirectly(file);
    }
  }

  /**
   * Executes the compiler process directly via command-line invocation on a pooled thread,
   * attaching a process listener to refresh IDE inspections upon completion.
   *
   * @param file the target {@code .compact} file to compile
   */
  private void executeDirectly(@NotNull VirtualFile file) {
    List<String> args = new ArrayList<>();
    if (skipZkCheckBox.isSelected()) {
      args.add("--skip-zk");
    }
    args.add(file.getPath());

    try {
      GeneralCommandLine cmd = CompactToolchainUtil.createCommandLine(project, args, project.getBasePath());
      notifyUser("Starting compilation of " + file.getName() + "...", NotificationType.INFORMATION);

      ApplicationManager.getApplication().executeOnPooledThread(() -> {
        try {
          ProcessHandler handler = new OSProcessHandler(cmd);
          handler.addProcessListener(new ProcessListener() {
            @Override
            public void processTerminated(@NotNull ProcessEvent event) {
              CompactProblemUtil.clearProblemsAndRestart(project, file);
            }
          });
          handler.startNotify();
        } catch (ExecutionException ex) {
          notifyUser("Compilation failed: " + ex.getMessage(), NotificationType.ERROR);
        }
      });
    } catch (ExecutionException e) {
      notifyUser("Failed to run Compact compiler: " + e.getMessage(), NotificationType.ERROR);
    }
  }

  /**
   * Clears inspection problems and restarts the daemon code analyser for the target contract.
   */
  private void restartDaemon() {
    CompactProblemUtil.clearProblemsAndRestart(project, findTargetCompactFile());
  }

  /**
   * Posts an IntelliJ balloon notification under the "Midnight Notifications" group.
   *
   * @param content notification message text
   * @param type    severity type (INFORMATION, WARNING, ERROR)
   */
  private void notifyUser(@NotNull String content, @NotNull NotificationType type) {
    NotificationGroup group = NotificationGroupManager.getInstance().getNotificationGroup("Midnight Notifications");
    if (group != null) {
      group.createNotification(content, type).notify(project);
    }
  }
}
