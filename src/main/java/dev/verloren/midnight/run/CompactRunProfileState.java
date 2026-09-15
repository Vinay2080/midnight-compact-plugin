package dev.verloren.midnight.run;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.CommandLineState;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessListener;
import com.intellij.execution.process.ProcessTerminatedListener;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import dev.verloren.midnight.annotator.CompactProblemUtil;
import org.jetbrains.annotations.NotNull;

public class CompactRunProfileState extends CommandLineState {
  private final CompactRunConfiguration configuration;

  @SuppressWarnings("this-escape")
  public CompactRunProfileState(@NotNull ExecutionEnvironment environment, @NotNull CompactRunConfiguration configuration) {
    super(environment);
    this.configuration = configuration;
    addConsoleFilters(new CompactConsoleFilter(environment.getProject()));
  }

  @Override
  protected @NotNull ProcessHandler startProcess() throws ExecutionException {
    GeneralCommandLine commandLine = CompactToolchainUtil.createCommandLine(
        getEnvironment().getProject(),
        configuration.buildCommandLineArgs(),
        getEnvironment().getProject().getBasePath()
    );

    OSProcessHandler handler = new OSProcessHandler(commandLine);
    ProcessTerminatedListener.attach(handler);
    handler.addProcessListener(new ProcessListener() {
      @Override
      public void processTerminated(@NotNull ProcessEvent event) {
        Project project = getEnvironment().getProject();
        String path = configuration.getCompactFilePath();
        VirtualFile targetFile = (path != null && !path.isEmpty())
            ? LocalFileSystem.getInstance().findFileByPath(path)
            : null;
        CompactProblemUtil.clearProblemsAndRestart(project, targetFile);
      }
    });
    return handler;
  }
}
