package dev.verloren.midnight.run;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.CommandLineState;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessListener;
import com.intellij.execution.process.ProcessTerminatedListener;
import com.intellij.execution.filters.OpenFileHyperlinkInfo;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import dev.verloren.midnight.annotator.CompactProblemUtil;
import org.jetbrains.annotations.NotNull;

/**
 * Execution profile state that constructs and manages the OS process for compiling a Compact contract.
 *
 * <h3>Architectural Role</h3>
 * <p>Extends {@link CommandLineState} to bridge IntelliJ's execution engine with the external Compact compiler
 * CLI (either native binary {@code compactc} or {@code npx compactc}). Configures interactive console filters
 * via {@link CompactConsoleFilter}, launches the process as an {@link OSProcessHandler}, and registers a termination
 * listener that flushes stale compiler diagnostics and triggers re-analysis across the editor via
 * {@link CompactProblemUtil#clearProblemsAndRestart(Project, VirtualFile)}.</p>
 *
 * <h3>Execution Lifecycle</h3>
 * <pre>
 *   [User triggers Run Action]
 *                 │
 *                 ▼
 *   [CompactRunConfiguration.getState()] ──► Instantiates CompactRunProfileState
 *                 │
 *                 ▼
 *   [CompactRunProfileState.startProcess()]
 *                 │
 *                 ├─► CompactToolchainUtil.createCommandLine() (resolves binary &amp; args)
 *                 ├─► Spawns OSProcessHandler
 *                 ├─► Attaches ProcessTerminatedListener (notifies exit code)
 *                 │
 *                 ▼
 *   [Process Execution &amp; Streaming Console Output]
 *                 │
 *                 ├─► Filtered by CompactConsoleFilter (clickable error hyperlinks)
 *                 │
 *                 ▼
 *   [Process Termination Callback]
 *                 │
 *                 └─► CompactProblemUtil.clearProblemsAndRestart()
 *                       (Synchronizes IDE editor diagnostics with fresh compiler build)
 * </pre>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Process Pool:</b> {@link #startProcess()} runs on an asynchronous background execution thread.</li>
 *   <li><b>Process Listeners:</b> {@code processTerminated} runs on the process reader thread.</li>
 * </ul>
 *
 * @see CompactRunConfiguration
 * @see CompactToolchainUtil
 * @see CompactConsoleFilter
 * @see CompactProblemUtil
 */
public class CompactRunProfileState extends CommandLineState {

  /**
   * Reference to the parent run configuration holding compiler parameters.
   */
  private final CompactRunConfiguration configuration;

  /**
   * Console filter that parses compiler diagnostics into navigable hyperlinks and tracks the primary error.
   */
  private final CompactConsoleFilter consoleFilter;

  /**
   * Initializes the execution profile state and registers the console error filter.
   *
   * @param environment   current execution environment
   * @param configuration the Compact run configuration being executed
   */
  @SuppressWarnings("this-escape")
  public CompactRunProfileState(@NotNull ExecutionEnvironment environment, @NotNull CompactRunConfiguration configuration) {
    super(environment);
    this.configuration = configuration;
    this.consoleFilter = new CompactConsoleFilter(environment.getProject(), configuration.getCompactFilePath());
    // Attach console filter to convert error messages into navigable hyperlinks
    addConsoleFilters(consoleFilter);
  }

  /**
   * Constructs the OS process command line and launches the Compact compiler process handler.
   *
   * @return active {@link ProcessHandler} tracking the compiler process
   * @throws ExecutionException if command resolution or process spawning fails
   */
  @Override
  protected @NotNull ProcessHandler startProcess() throws ExecutionException {
    // 0. Synchronize open in-memory editor documents to disk so compactc compiles the exact editor buffer
    ApplicationManager.getApplication().invokeAndWait(() -> FileDocumentManager.getInstance().saveAllDocuments());

    // 1. Resolve the executable path and assemble the full command line with flags and targets
    GeneralCommandLine commandLine = CompactToolchainUtil.createCommandLine(
            getEnvironment().getProject(),
            configuration.buildCommandLineArgs(),
            getEnvironment().getProject().getBasePath()
    );

    // 2. Wrap the command line in an OS process handler
    OSProcessHandler handler = new OSProcessHandler(commandLine);
    ProcessTerminatedListener.attach(handler);

    // 3. Register a process listener to refresh IDE problem markers and auto-navigate to first error on failure
    handler.addProcessListener(createProcessListener());
    return handler;
  }

  /**
   * Constructs the process listener handling IDE problem cache refreshing and auto-navigation on build exit.
   *
   * @return active {@link ProcessListener} instance
   */
  private @NotNull ProcessListener createProcessListener() {
    return new ProcessListener() {
      @Override
      public void processTerminated(@NotNull ProcessEvent event) {
        Project project = getEnvironment().getProject();
        String path = configuration.getCompactFilePath();
        VirtualFile targetFile = (path != null && !path.isEmpty())
                ? LocalFileSystem.getInstance().findFileByPath(path)
                : null;
        CompactProblemUtil.clearProblemsAndRestart(project, targetFile);

        if (event.getExitCode() != 0) {
          OpenFileHyperlinkInfo firstError = consoleFilter.getFirstErrorHyperlink();
          if (firstError != null) {
            ApplicationManager.getApplication().invokeLater(() -> {
              if (!project.isDisposed()) {
                firstError.navigate(project);
              }
            }, ModalityState.any());
          }
        }
      }
    };
  }
}
