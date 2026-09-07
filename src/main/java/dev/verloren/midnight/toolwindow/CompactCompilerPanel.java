package dev.verloren.midnight.toolwindow;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.ProgramRunnerUtil;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.icons.AllIcons;
import com.intellij.ide.HelpTooltipKt;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
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
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import dev.verloren.midnight.CompactFileType;
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
 */
public class CompactCompilerPanel extends JPanel {
  private static final Logger LOG = Logger.getInstance(CompactCompilerPanel.class);

  private final Project project;
  private final JPanel cardsPanel;
  private final JBLabel activeFileNameLabel = new JBLabel();
  private final JBLabel pragmaBadge = new JBLabel();
  private final JBLabel compilerStatusLabel = new JBLabel();

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

  private final JBCheckBox skipZkCheckBox = new JBCheckBox("Skip ZK proof generation (--skip-zk)");

  private final Set<String> downloadingVersions = ConcurrentHashMap.newKeySet();
  private String currentPragmaConstraint = null;

  @SuppressWarnings("this-escape")
  public CompactCompilerPanel(@NotNull Project project) {
    super(new BorderLayout());
    this.project = project;

    setBorder(JBUI.Borders.empty(8));

    // Top: Active Contract Status & Compilation Card
    JPanel topPanel = createTopPanel();
    add(topPanel, BorderLayout.NORTH);

    // Center: Compact Compiler Versions Manager
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

    // Initial population of cards and status
    refreshCards();
    updateActiveFileInfo();

    // Listen to active editor changes to keep pragma and file info in sync
    project.getMessageBus().connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerListener() {
      @Override
      public void selectionChanged(@NotNull FileEditorManagerEvent event) {
        updateActiveFileInfo();
      }

      @Override
      public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        updateActiveFileInfo();
      }

      @Override
      public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        updateActiveFileInfo();
      }
    });
  }

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

  public void refreshCards() {
    ApplicationManager.getApplication().invokeLater(() -> {
      if (project.isDisposed()) return;

      cardsPanel.removeAll();

      MidnightProjectSettings settings = MidnightProjectSettings.getInstance(project);
      String selectedVer = settings.selectedCompilerVersion != null ? settings.selectedCompilerVersion.trim() : "";
      String activeCompilerVer = CompactToolchainUtil.getActiveCompilerVersion(project);
      String effectiveActive = !selectedVer.isEmpty() ? selectedVer : (activeCompilerVer != null ? activeCompilerVer : "");

      // 1. Collect all known and locally installed compiler versions
      Set<String> allVersionKeys = new LinkedHashSet<>(CompactVersionManager.KNOWN_VERSIONS);
      Map<String, String> installedMap = CompactVersionManager.getInstalledVersions();
      allVersionKeys.addAll(installedMap.keySet());

      // Sort descending by SemVer
      List<String> sortedVersions = new ArrayList<>(allVersionKeys);
      sortedVersions.sort((v1, v2) -> {
        CompactSemVerUtil.SemVer s1 = CompactSemVerUtil.parse(v1);
        CompactSemVerUtil.SemVer s2 = CompactSemVerUtil.parse(v2);
        if (s1 != null && s2 != null) {
          return s2.compareTo(s1);
        }
        return v2.compareTo(v1);
      });

      // 2. Render Minimal Classic Version Cards
      for (String v : sortedVersions) {
        boolean isInstalled = CompactVersionManager.isVersionInstalled(v);
        boolean isActive = v.equals(effectiveActive);
        boolean isDownloading = downloadingVersions.contains(v);
        String installedPath = installedMap.get(v);

        String langVer = CompactVersionManager.getLanguageVersionForToolchain(v);
        String cardTitle = v.equals(langVer) ? "Compact v" + v : "Compact v" + v + " (Language v" + langVer + ")";

        boolean isPragmaMatch = currentPragmaConstraint != null &&
            CompactSemVerUtil.satisfiesConstraint(langVer, currentPragmaConstraint);

        CompactVersionCard card = new CompactVersionCard(
            v,
            cardTitle,
            isInstalled,
            isActive,
            isPragmaMatch,
            isDownloading,
            installedPath,
            () -> {
              if (isInstalled) {
                selectVersionImmediately(v);
              } else {
                downloadAndActivateVersion(v);
              }
            },
            () -> onRemoveVersion(v, installedPath)
        );

        cardsPanel.add(card);
        cardsPanel.add(Box.createVerticalStrut(JBUI.scale(4)));
      }

      cardsPanel.revalidate();
      cardsPanel.repaint();
    }, ModalityState.any());
  }

  /**
   * Instantly updates UI selection on EDT with zero perceived latency,
   * then updates settings and triggers background reparsing.
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
    for (Component c : cardsPanel.getComponents()) {
      if (c instanceof CompactVersionCard card) {
        card.setActive(version.equals(card.getVersion()));
      }
    }

    updateActiveFileInfo();
    restartDaemon();
    notifyUser("Switched project compiler to Compact v" + version + ".", NotificationType.INFORMATION);
  }

  private void downloadAndActivateVersion(@NotNull String version) {
    if (downloadingVersions.contains(version)) {
      return;
    }

    // Confirmation dialog: do not start downloading on a single accidental click
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
   * Intelligently resolves the target .compact file:
   * 1. Currently, focused editor if it's a .compact file.
   * 2. Any currently open editor tabs containing a .compact file.
   * 3. Any indexed .compact file within the project directory.
   */
  public @Nullable VirtualFile findTargetCompactFile() {
    // 1. Focused editor file
    VirtualFile[] selected = FileEditorManager.getInstance(project).getSelectedFiles();
    for (VirtualFile f : selected) {
      if (f != null && "compact".equalsIgnoreCase(f.getExtension())) {
        return f;
      }
    }

    // 2. Any open editor file
    VirtualFile[] open = FileEditorManager.getInstance(project).getOpenFiles();
    for (VirtualFile f : open) {
      if (f != null && "compact".equalsIgnoreCase(f.getExtension())) {
        return f;
      }
    }

    // 3. Project index search
    try {
      Collection<VirtualFile> indexed = FileTypeIndex.getFiles(CompactFileType.INSTANCE, GlobalSearchScope.projectScope(project));
      if (!indexed.isEmpty()) {
        return indexed.iterator().next();
      }
    } catch (Exception _) {
    }

    return null;
  }

  public void updateActiveFileInfo() {
    ApplicationManager.getApplication().invokeLater(() -> {
      if (project.isDisposed()) return;

      VirtualFile file = findTargetCompactFile();
      if (file == null) {
        activeFileNameLabel.setText("No Compact contract");
        pragmaBadge.setText("Open or create .compact");
        compilerStatusLabel.setText("Active Compiler: " + getActiveCompilerDisplay());
        compilerStatusLabel.setForeground(JBColor.GRAY);
        currentPragmaConstraint = null;
        compileButton.setEnabled(true);
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

      if (pragma != null) {
        String constraint = pragma.getConstraintText();
        if (constraint == null || constraint.isEmpty()) {
          constraint = pragma.getRequiredVersion();
        }
        if (constraint != null) {
          currentPragmaConstraint = constraint;
          pragmaBadge.setText("pragma " + constraint);

          String activeVer = CompactToolchainUtil.getActiveCompilerVersion(project);
          if (activeVer != null) {
            String langVer = CompactVersionManager.getLanguageVersionForToolchain(activeVer);
            boolean satisfies = CompactSemVerUtil.satisfiesConstraint(langVer, constraint);

            if (satisfies) {
              compilerStatusLabel.setText("Compiler v" + activeVer + " (Language v" + langVer + ") satisfies pragma (" + constraint + ")");
              compilerStatusLabel.setForeground(new JBColor(new Color(36, 138, 61), new Color(63, 185, 80)));
            } else {
              compilerStatusLabel.setText("Compiler v" + activeVer + " (Language v" + langVer + ") does not satisfy pragma (" + constraint + ")");
              compilerStatusLabel.setForeground(JBColor.RED);
            }
          } else {
            compilerStatusLabel.setText("Active Compiler: " + getActiveCompilerDisplay());
            compilerStatusLabel.setForeground(JBColor.GRAY);
          }
        } else {
          currentPragmaConstraint = null;
          pragmaBadge.setText("No pragma declared");
          compilerStatusLabel.setText("Active Compiler: " + getActiveCompilerDisplay());
          compilerStatusLabel.setForeground(JBColor.GRAY);
        }
      } else {
        currentPragmaConstraint = null;
        pragmaBadge.setText("No pragma declared");
        compilerStatusLabel.setText("Active Compiler: " + getActiveCompilerDisplay());
        compilerStatusLabel.setForeground(JBColor.GRAY);
      }
    }, ModalityState.any());
  }

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
          handler.startNotify();
        } catch (ExecutionException ex) {
          notifyUser("Compilation failed: " + ex.getMessage(), NotificationType.ERROR);
        }
      });
    } catch (ExecutionException e) {
      notifyUser("Failed to run Compact compiler: " + e.getMessage(), NotificationType.ERROR);
    }
  }

  private void restartDaemon() {
    CompactProblemUtil.clearProblemsAndRestart(project, findTargetCompactFile());
  }

  private void notifyUser(@NotNull String content, @NotNull NotificationType type) {
    NotificationGroup group = NotificationGroupManager.getInstance().getNotificationGroup("Midnight Notifications");
    if (group != null) {
      group.createNotification(content, type).notify(project);
    }
  }
}
