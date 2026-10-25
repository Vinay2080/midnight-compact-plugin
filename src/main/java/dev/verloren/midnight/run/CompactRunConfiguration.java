package dev.verloren.midnight.run;


import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.LocatableConfigurationBase;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Persistent run configuration model representing an invocation of the Midnight Compact smart contract compiler.
 *
 * <h3>Architectural Role</h3>
 * <p>Extends {@link LocatableConfigurationBase} with execution state type {@link CompactRunProfileState}.
 * Encapsulates compiler invocation options including the target {@code .compact} source file path,
 * destination artifact directory, ZK proving key generation bypass ({@code --skip-zk}), and optional
 * custom flags. Manages XML persistence to {@code .idea/runConfigurations} and coordinates with
 * {@link CompactRunConfigurationEditor} for UI presentation and {@link CompactRunProfileState} for process execution.</p>
 *
 * <h3>CLI Argument Assembly Pipeline</h3>
 * <pre>
 *   [CompactRunConfiguration.buildCommandLineArgs()]
 *                           │
 *                           ├─► "--vscode" (enables JSON diagnostics streaming)
 *                           │
 *                           ├─► isSkipZk()? ──► YES ──► "--skip-zk"
 *                           │
 *                           ├─► customCompilerFlags (whitespace-split token list)
 *                           │
 *                           ├─► compactFilePath (target contract source)
 *                           │
 *                           └─► outputDirectory (defaults to derived or "gen")
 * </pre>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Read / Write:</b> Getters and setters are accessed on EDT during UI interaction and on background
 *       execution threads during process launch. State reading and XML serialization are synchronized by IntelliJ's
 *       configuration infrastructure.</li>
 * </ul>
 *
 * @see CompactConfigurationType
 * @see CompactConfigurationFactory
 * @see CompactRunConfigurationEditor
 * @see CompactRunProfileState
 * @see CompactToolchainUtil
 */
public class CompactRunConfiguration extends LocatableConfigurationBase<CompactRunProfileState> {

  /**
   * File path to the Compact smart contract source file ({@code .compact}) to be compiled.
   */
  private String compactFilePath = "";

  /**
   * Target directory where generated TypeScript bindings, ZKIR, and proof keys are emitted.
   */
  private String outputDirectory = "";

  /**
   * Flag indicating whether to skip ZK proving key generation (passing {@code --skip-zk})
   * for faster build turnarounds during local development. Defaults to {@code true}.
   */
  private boolean skipZk = true;

  /**
   * Arbitrary user-supplied CLI flags appended to the compiler invocation.
   */
  private String customCompilerFlags = "";

  /**
   * Constructs a new Compact run configuration.
   *
   * @param project the project owning this configuration
   * @param factory the parent configuration factory
   * @param name configuration title
   */
  public CompactRunConfiguration(@NotNull Project project, @NotNull ConfigurationFactory factory, @Nullable String name) {
    super(project, factory, name);
  }

  /**
   * Returns the file path of the Compact contract to compile.
   *
   * @return contract file path string
   */
  public String getCompactFilePath() {
    return compactFilePath;
  }

  /**
   * Updates the target Compact contract file path, automatically re-deriving the default output directory
   * if the current output directory is blank, default {@code "gen"}, or matched the prior derived path.
   *
   * @param compactFilePath the new file path
   */
  public void setCompactFilePath(String compactFilePath) {
    String oldDerived = (this.compactFilePath != null && !this.compactFilePath.trim().isEmpty())
        ? CompactToolchainUtil.deriveOutputDirectory(getProject(), this.compactFilePath)
        : "";
    this.compactFilePath = compactFilePath != null ? compactFilePath : "";
    if (outputDirectory == null || outputDirectory.trim().isEmpty() || "gen".equals(outputDirectory.trim()) || outputDirectory.equals(oldDerived)) {
      if (!this.compactFilePath.trim().isEmpty()) {
        this.outputDirectory = CompactToolchainUtil.deriveOutputDirectory(getProject(), this.compactFilePath);
      }
    }
  }

  /**
   * Returns the target output directory, deriving a sensible default based on the contract file path
   * or falling back to {@code "gen"} if unconfigured.
   *
   * @return output directory path string
   */
  public String getOutputDirectory() {
    if (outputDirectory != null && !outputDirectory.trim().isEmpty()) {
      return outputDirectory;
    }
    if (compactFilePath != null && !compactFilePath.trim().isEmpty()) {
      return CompactToolchainUtil.deriveOutputDirectory(getProject(), compactFilePath);
    }
    return "gen";
  }

  /**
   * Sets the target directory where compiler artifacts should be written.
   *
   * @param outputDirectory target directory path
   */
  public void setOutputDirectory(String outputDirectory) {
    this.outputDirectory = outputDirectory != null ? outputDirectory : "";
  }

  /**
   * Returns whether ZK key generation is bypassed via {@code --skip-zk}.
   *
   * @return {@code true} if ZK key generation is skipped; {@code false} for full build
   */
  public boolean isSkipZk() {
    return skipZk;
  }

  /**
   * Configures whether to pass {@code --skip-zk} to the compiler.
   *
   * @param skipZk {@code true} to skip ZK key generation
   */
  public void setSkipZk(boolean skipZk) {
    this.skipZk = skipZk;
  }

  /**
   * Returns any custom CLI flags configured for the compiler invocation.
   *
   * @return custom compiler flags string
   */
  public String getCustomCompilerFlags() {
    return customCompilerFlags;
  }

  /**
   * Sets custom CLI flags to pass to the compiler.
   *
   * @param customCompilerFlags space-delimited flags string
   */
  public void setCustomCompilerFlags(String customCompilerFlags) {
    this.customCompilerFlags = customCompilerFlags != null ? customCompilerFlags : "";
  }

  /**
   * Creates the visual settings editor for editing this run configuration's properties.
   *
   * @return a new {@link CompactRunConfigurationEditor} instance
   */
  @Override
  public @NotNull SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
    return new CompactRunConfigurationEditor();
  }

  /**
   * Prepares and returns the execution profile state for running this configuration.
   *
   * @param executor the execution mode (e.g. Run, Debug)
   * @param environment execution environment details
   * @return a new {@link CompactRunProfileState}
   */
  @Override
  public @Nullable RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment environment) {
    return new CompactRunProfileState(environment, this);
  }

  /**
   * Deserializes configuration properties from the provided JDOM element.
   *
   * @param element serialized XML element from IntelliJ project files
   * @throws InvalidDataException if element parsing fails
   */
  @Override
  public void readExternal(@NotNull Element element) throws InvalidDataException {
    super.readExternal(element);
    compactFilePath = element.getAttributeValue("compactFilePath", "");
    String savedOutDir = element.getAttributeValue("outputDirectory", "");
    if (savedOutDir == null || savedOutDir.trim().isEmpty() || "gen".equals(savedOutDir.trim())) {
      if (compactFilePath != null && !compactFilePath.trim().isEmpty()) {
        outputDirectory = CompactToolchainUtil.deriveOutputDirectory(getProject(), compactFilePath);
      } else {
        outputDirectory = "gen";
      }
    } else {
      outputDirectory = savedOutDir;
    }
    skipZk = Boolean.parseBoolean(element.getAttributeValue("skipZk", "true"));
    customCompilerFlags = element.getAttributeValue("customCompilerFlags", "");
  }

  /**
   * Serializes current configuration properties into the provided JDOM element.
   *
   * @param element destination XML element
   * @throws WriteExternalException if serialization fails
   */
  @Override
  public void writeExternal(@NotNull Element element) throws WriteExternalException {
    super.writeExternal(element);
    element.setAttribute("compactFilePath", compactFilePath != null ? compactFilePath : "");
    element.setAttribute("outputDirectory", getOutputDirectory());
    element.setAttribute("skipZk", String.valueOf(skipZk));
    element.setAttribute("customCompilerFlags", customCompilerFlags != null ? customCompilerFlags : "");
  }

  /**
   * Builds the complete list of command-line arguments to pass to the Compact compiler binary (or {@code npx compactc}).
   *
   * @return ordered list of argument strings
   */
  public @NotNull List<String> buildCommandLineArgs() {
    List<String> args = new ArrayList<>();
    // 1. Force VS Code / IDE JSON diagnostic output format
    args.add("--vscode");

    // 2. Append fast dev flag if enabled
    if (skipZk) {
      args.add("--skip-zk");
    }

    // 3. Append custom user-specified flags
    if (customCompilerFlags != null && !customCompilerFlags.trim().isEmpty()) {
      for (String flag : customCompilerFlags.trim().split("\\s+")) {
        if (!flag.isEmpty()) {
          args.add(flag);
        }
      }
    }

    // 4. Source file path
    if (compactFilePath != null && !compactFilePath.isEmpty()) {
      args.add(compactFilePath);
    }

    // 5. Output directory
    String outDir = getOutputDirectory();
    if (outDir != null && !outDir.isEmpty()) {
      args.add(outDir);
    }
    return args;
  }
}
