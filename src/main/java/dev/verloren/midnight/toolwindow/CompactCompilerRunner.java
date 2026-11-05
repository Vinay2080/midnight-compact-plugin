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
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import dev.verloren.midnight.annotator.CompactProblemUtil;
import dev.verloren.midnight.run.CompactConfigurationType;
import dev.verloren.midnight.run.CompactRunConfiguration;
import dev.verloren.midnight.run.CompactToolchainUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Executes contract compilation via temporary run configuration or direct command-line fallback.
 */
public final class CompactCompilerRunner {
  private static final Logger LOG = Logger.getInstance(CompactCompilerRunner.class);

  private CompactCompilerRunner() {}

  /**
   * Compiles the target Compact contract file with given flags.
   */
  public static void compileContract(
      @NotNull Project project,
      @NotNull VirtualFile file,
      boolean skipZk,
      @NotNull String outputDir,
      @NotNull String customFlags
  ) {
    try {
      RunManager runManager = RunManager.getInstance(project);
      ConfigurationFactory factory = CompactConfigurationType.getInstance().getConfigurationFactories()[0];
      RunnerAndConfigurationSettings settings = runManager.createConfiguration("Compile " + file.getName(), factory);
      CompactRunConfiguration config = (CompactRunConfiguration) settings.getConfiguration();
      config.setCompactFilePath(file.getPath());
      config.setSkipZk(skipZk);
      config.setOutputDirectory(outputDir);
      config.setCustomCompilerFlags(customFlags);
      runManager.setTemporaryConfiguration(settings);

      ProgramRunnerUtil.executeConfiguration(settings, DefaultRunExecutor.getRunExecutorInstance());
    } catch (Exception e) {
      LOG.warn("Failed to launch run configuration, executing directly via command line", e);
      executeDirectly(project, file, skipZk, outputDir, customFlags);
    }
  }

  private static void executeDirectly(
      @NotNull Project project,
      @NotNull VirtualFile file,
      boolean skipZk,
      @NotNull String outputDir,
      @NotNull String customFlags
  ) {
    List<String> args = new ArrayList<>();
    if (skipZk) {
      args.add("--skip-zk");
    }
    if (!customFlags.isEmpty()) {
      for (String f : customFlags.split("\\s+")) {
        if (!f.isEmpty()) args.add(f);
      }
    }
    args.add(file.getPath());
    if (!outputDir.isEmpty()) {
      args.add(outputDir);
    }

    try {
      GeneralCommandLine cmd = CompactToolchainUtil.createCommandLine(project, args, project.getBasePath());
      notifyUser(project, "Compiling " + file.getName() + "...", NotificationType.INFORMATION);

      ApplicationManager.getApplication().executeOnPooledThread(() -> {
        try {
          OSProcessHandler handler = new OSProcessHandler(cmd);
          handler.addProcessListener(new ProcessListener() {
            @Override
            public void processTerminated(@NotNull ProcessEvent event) {
              CompactProblemUtil.clearProblemsAndRestart(project, file);
            }
          });
          handler.startNotify();
        } catch (ExecutionException ex) {
          notifyUser(project, "Compilation failed: " + ex.getMessage(), NotificationType.ERROR);
        }
      });
    } catch (ExecutionException e) {
      notifyUser(project, "Failed to run compiler: " + e.getMessage(), NotificationType.ERROR);
    }
  }

  /**
   * Posts an IDE notification under the Midnight Notifications group.
   */
  public static void notifyUser(@NotNull Project project, @NotNull String content, @NotNull NotificationType type) {
    NotificationGroup group = NotificationGroupManager.getInstance().getNotificationGroup("Midnight Notifications");
    if (group != null) {
      group.createNotification(content, type).notify(project);
    }
  }
}
