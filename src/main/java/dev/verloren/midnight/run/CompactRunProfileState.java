package dev.verloren.midnight.run;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.CommandLineState;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessTerminatedListener;
import com.intellij.execution.runners.ExecutionEnvironment;
import org.jetbrains.annotations.NotNull;


public class CompactRunProfileState extends CommandLineState {
  private final CompactRunConfiguration configuration;

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
    return handler;
  }
}
