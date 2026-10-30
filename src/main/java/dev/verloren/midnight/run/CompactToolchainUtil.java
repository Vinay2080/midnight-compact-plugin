package dev.verloren.midnight.run;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import dev.verloren.midnight.settings.MidnightProjectSettings;
import dev.verloren.midnight.settings.MidnightSettingsState;
import dev.verloren.midnight.version.CompactVersionManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;
import java.util.SequencedMap;

/**
 * Central coordinator and command builder for the Midnight Compact smart contract compiler toolchain.
 *
 * <h3>Architectural Role</h3>
 * <p>Serves as the primary facade for discovering, validating, and launching the Compact compiler across
 * diverse runtime environments. Unifies support for:</p>
 * <ul>
 *   <li>The low-level standalone compiler binary ({@code compactc})</li>
 *   <li>The high-level Midnight developer CLI ({@code compact}) invoking {@code compact compile}</li>
 *   <li>Native OS host execution on Windows, macOS, and Linux</li>
 *   <li>Windows Subsystem for Linux (WSL) execution with automatic path translation</li>
 *   <li>Project-local {@code node_modules/.bin} npm packages ({@code @midnight-ntwrk/compactc})</li>
 *   <li>Plugin-managed compiler versions downloaded via {@link CompactVersionManager}</li>
 * </ul>
 *
 * <h3>Toolchain Resolution Waterfall</h3>
 * <pre>
 *   [CompactToolchainUtil.getToolchainInfo(project)]
 *                          │
 *                          ├─► 1. Project-level custom compiler path / selected version
 *                          │      (MidnightProjectSettings)
 *                          │
 *                          ├─► 2. Global application settings (MidnightSettingsState)
 *                          │
 *                          ├─► 3. Project node_modules/.bin (compactc / compact)
 *                          │
 *                          ├─► 4. Managed downloaded versions (~/.compact/versions/)
 *                          │
 *                          └─► 5. System PATH &amp; Conventional locations
 *                                 ├─► WSL Linux probe (/home/user/.local/bin, /usr/bin)
 *                                 ├─► System PATH (PathEnvironmentVariableUtil)
 *                                 └─► Common package managers (npm, pnpm, yarn, brew, cargo)
 * </pre>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Read / Execution:</b> Safe to invoke from background execution threads. Lookups read project/global
 *       settings state and perform local filesystem existence probes.</li>
 * </ul>
 *
 * @see CompactToolchainDiscovery
 * @see CompactToolchainPathUtil
 * @see CompactRunProfileState
 * @see CompactVersionManager
 */
public final class CompactToolchainUtil {

  /**
   * Encapsulates discovered compiler toolchain metadata required to construct command lines.
   *
   * @param executablePath absolute path to the binary on host or within WSL Linux filesystem
   * @param isWsl whether the executable resides inside a Windows Subsystem for Linux environment
   * @param wslDistribution name of the target WSL distribution (e.g. {@code "Ubuntu"}), or {@code null}
   * @param isCompactCli {@code true} if the binary is the higher-level {@code compact} CLI; {@code false} if {@code compactc}
   */
  public record ToolchainInfo(
      @NotNull String executablePath,
      boolean isWsl,
      @Nullable String wslDistribution,
      boolean isCompactCli
  ) {
    /**
     * Checks whether the toolchain metadata contains a non-empty executable path.
     *
     * @return {@code true} if valid; {@code false} otherwise
     */
    public boolean isValid() {
      return !executablePath.isEmpty();
    }
  }

  /**
   * Private constructor to prevent instantiation of static utility class.
   */
  private CompactToolchainUtil() {
  }

  /**
   * Discovers installed WSL distributions by delegating to {@link CompactToolchainDiscovery}.
   *
   * @return list of WSL distribution names
   */
  public static @NotNull List<String> getInstalledWslDistributions() {
    return CompactToolchainDiscovery.getInstalledWslDistributions();
  }

  /**
   * Resolves compiler toolchain metadata by executing the multi-tier resolution waterfall.
   *
   * @param project current project context, or {@code null}
   * @return resolved {@link ToolchainInfo}, or {@code null} if no compiler is found
   */
  public static @Nullable ToolchainInfo getToolchainInfo(@Nullable Project project) {
    ToolchainInfo projectInfo = resolveFromProjectSettings(project);
    if (projectInfo != null) {
      return projectInfo;
    }
    ToolchainInfo globalInfo = resolveFromGlobalSettings();
    if (globalInfo != null) {
      return globalInfo;
    }
    ToolchainInfo nodeInfo = resolveFromNodeModules(project);
    if (nodeInfo != null) {
      return nodeInfo;
    }
    ToolchainInfo managedInfo = resolveFromManagedVersions();
    if (managedInfo != null) {
      return managedInfo;
    }
    return resolveFromHostOrPath();
  }

  private static @Nullable ToolchainInfo resolveFromProjectSettings(@Nullable Project project) {
    if (project == null) {
      return null;
    }
    MidnightProjectSettings settings = MidnightProjectSettings.getInstance(project);
    if (settings == null) {
      return null;
    }
    if (settings.customCompilerPath != null && !settings.customCompilerPath.trim().isEmpty()) {
      ToolchainInfo customInfo = parseConfiguredPath(settings.customCompilerPath.trim());
      if (customInfo != null) {
        return customInfo;
      }
    }
    if (settings.selectedCompilerVersion != null && !settings.selectedCompilerVersion.trim().isEmpty()) {
      String versionExe = CompactVersionManager.getInstalledExecutable(settings.selectedCompilerVersion.trim());
      if (versionExe != null) {
        CompactVersionManager.cacheExecutableVersion(versionExe, settings.selectedCompilerVersion.trim());
        ToolchainInfo versionInfo = parseConfiguredPath(versionExe);
        if (versionInfo != null) {
          return versionInfo;
        }
        boolean isCli = CompactToolchainDiscovery.isCompactCliName(new File(versionExe).getName());
        return new ToolchainInfo(versionExe, false, null, isCli);
      }
    }
    return null;
  }

  private static @Nullable ToolchainInfo resolveFromGlobalSettings() {
    MidnightSettingsState state = MidnightSettingsState.getInstance();
    if (state != null && state.compilerPath != null && !state.compilerPath.trim().isEmpty()) {
      return parseConfiguredPath(state.compilerPath.trim());
    }
    return null;
  }

  private static @Nullable ToolchainInfo resolveFromNodeModules(@Nullable Project project) {
    if (project == null || project.getBasePath() == null) {
      return null;
    }
    File nodeModulesBin = new File(project.getBasePath(), "node_modules/.bin");
    if (!nodeModulesBin.isDirectory()) {
      return null;
    }
    File localCompactc = findExecutableInDir(nodeModulesBin, "compactc");
    if (localCompactc != null) {
      return new ToolchainInfo(localCompactc.getAbsolutePath(), false, null, false);
    }
    File localCompact = findExecutableInDir(nodeModulesBin, "compact");
    if (localCompact != null) {
      return new ToolchainInfo(localCompact.getAbsolutePath(), false, null, true);
    }
    return null;
  }

  private static @Nullable ToolchainInfo resolveFromManagedVersions() {
    SequencedMap<String, String> installedVersions = CompactVersionManager.getInstalledVersions();
    if (installedVersions.isEmpty()) {
      return null;
    }
    String latestExe = installedVersions.firstEntry().getValue();
    ToolchainInfo managedInfo = parseConfiguredPath(latestExe);
    if (managedInfo != null) {
      return managedInfo;
    }
    boolean isCli = CompactToolchainDiscovery.isCompactCliName(new File(latestExe).getName());
    return new ToolchainInfo(latestExe, false, null, isCli);
  }

  private static @Nullable ToolchainInfo resolveFromHostOrPath() {
    if (SystemInfo.isWindows) {
      ToolchainInfo wslInfo = CompactToolchainDiscovery.findInWsl();
      if (wslInfo != null) {
        return wslInfo;
      }
    }
    File pathCompactc = findExecutableInPath("compactc");
    if (pathCompactc != null) {
      return new ToolchainInfo(pathCompactc.getAbsolutePath(), false, null, false);
    }
    File pathCompact = findExecutableInPath("compact");
    if (pathCompact != null) {
      return new ToolchainInfo(pathCompact.getAbsolutePath(), false, null, true);
    }
    File commonCompactc = CompactToolchainDiscovery.findInCommonLocations("compactc");
    if (commonCompactc != null) {
      return new ToolchainInfo(commonCompactc.getAbsolutePath(), false, null, false);
    }
    File commonCompact = CompactToolchainDiscovery.findInCommonLocations("compact");
    if (commonCompact != null) {
      return new ToolchainInfo(commonCompact.getAbsolutePath(), false, null, true);
    }
    return null;
  }

  /**
   * Resolves and returns the absolute executable path of the active Compact compiler.
   *
   * @param project current project context, or {@code null}
   * @return executable path string, or {@code null} if toolchain is unresolvable
   */
  public static @Nullable String getCompilerExecutablePath(@Nullable Project project) {
    ToolchainInfo info = getToolchainInfo(project);
    return info != null ? info.executablePath() : null;
  }

  /**
   * Determines the active SemVer version string of the Compact compiler for the given project.
   *
   * @param project current project context
   * @return SemVer string (e.g. {@code "0.24.0"}), or {@code null} if unresolvable
   */
  public static @Nullable String getActiveCompilerVersion(@Nullable Project project) {
    if (project != null) {
      MidnightProjectSettings projectSettings = MidnightProjectSettings.getInstance(project);
      if (projectSettings != null && projectSettings.selectedCompilerVersion != null
          && !projectSettings.selectedCompilerVersion.trim().isEmpty()) {
        String ver = projectSettings.selectedCompilerVersion.trim();
        Application app = ApplicationManager.getApplication();
        if (app != null && app.isUnitTestMode()) {
          return ver;
        }
        if (CompactVersionManager.isVersionInstalled(ver)) {
          return ver;
        }
      }
    }
    ToolchainInfo info = getToolchainInfo(project);
    if (info == null) {
      return null;
    }
    return CompactVersionManager.detectVersionFromExecutable(info.executablePath());
  }

  /**
   * Constructs a fully-configured {@link GeneralCommandLine} ready to execute the Compact compiler,
   * handling WSL execution wrapping (via {@code wsl.exe -d <distro> -e <exe>}) or native host execution.
   *
   * @param project current project context
   * @param compilerArgs list of CLI parameters (flags, contract path, output directory)
   * @param workingDir optional working directory, or {@code null}
   * @return executable {@link GeneralCommandLine}
   * @throws ExecutionException if no valid toolchain could be discovered
   */
  public static @NotNull GeneralCommandLine createCommandLine(
      @Nullable Project project,
      @NotNull List<String> compilerArgs,
      @Nullable String workingDir
  ) throws ExecutionException {
    // 1. Resolve compiler toolchain metadata
    ToolchainInfo info = getToolchainInfo(project);
    if (info == null) {
      throw new ExecutionException("""
          Cannot find Compact compiler ('compactc' or 'compact').
          
          Please verify that compact / compactc is installed or configure its location:
            • In WSL: e.g. /home/<user>/.local/bin/compact or /home/<user>/.compact/bin/compactc
            • In project: npm install --save-dev @midnight-ntwrk/compactc
            • Globally: npm install -g @midnight-ntwrk/compactc
            • Configure in IntelliJ: Settings -> Languages & Frameworks -> Midnight Compact""");
    }

    // 2. Instantiate and configure command line parameters for WSL or Host OS
    GeneralCommandLine cmd = new GeneralCommandLine();
    if (info.isWsl()) {
      configureWslParameters(cmd, info, compilerArgs);
    } else {
      configureHostParameters(cmd, info, compilerArgs);
    }
    if (workingDir != null && !workingDir.isEmpty()) {
      cmd.setWorkDirectory(workingDir);
    }
    return cmd;
  }

  /**
   * Configures parameters for executing the compiler within a Windows Subsystem for Linux (WSL) container.
   *
   * @param cmd target command line object
   * @param info WSL toolchain metadata
   * @param compilerArgs raw CLI arguments to be converted into Linux WSL paths
   */
  private static void configureWslParameters(
      @NotNull GeneralCommandLine cmd,
      @NotNull ToolchainInfo info,
      @NotNull List<String> compilerArgs
  ) {
    cmd.setExePath("wsl.exe");
    if (info.wslDistribution() != null && !info.wslDistribution().isEmpty()) {
      cmd.addParameters("-d", info.wslDistribution());
    }
    cmd.addParameter("-e");
    cmd.addParameter(info.executablePath());
    if (info.isCompactCli()) {
      cmd.addParameter("compile");
    }
    for (String arg : compilerArgs) {
      cmd.addParameter(toWslPath(arg));
    }
  }

  /**
   * Configures parameters for executing the compiler natively on the host operating system.
   *
   * @param cmd target command line object
   * @param info host toolchain metadata
   * @param compilerArgs CLI arguments
   */
  private static void configureHostParameters(
      @NotNull GeneralCommandLine cmd,
      @NotNull ToolchainInfo info,
      @NotNull List<String> compilerArgs
  ) {
    cmd.setExePath(info.executablePath());
    if (info.isCompactCli()) {
      cmd.addParameter("compile");
    }
    cmd.addParameters(compilerArgs);
  }

  public static @NotNull String toWslPath(@NotNull String path) {
    return CompactToolchainPathUtil.toWslPath(path);
  }

  public static @NotNull String toWindowsPath(@NotNull String path) {
    return CompactToolchainPathUtil.toWindowsPath(path);
  }

  public static @NotNull String deriveOutputDirectory(@Nullable Project project, @NotNull String compactFilePath) {
    return CompactToolchainPathUtil.deriveOutputDirectory(project, compactFilePath);
  }

  public static @NotNull String deriveOutputDirectory(
      @Nullable Project project,
      @NotNull String compactFilePath,
      @Nullable String baseOutputDir
  ) {
    return CompactToolchainPathUtil.deriveOutputDirectory(project, compactFilePath, baseOutputDir);
  }

  public static @Nullable ToolchainInfo parseConfiguredPath(@NotNull String customPath) {
    return CompactToolchainDiscovery.parseConfiguredPath(customPath);
  }

  public static boolean isWindowsSystemExecutable(@NotNull File file) {
    return CompactToolchainDiscovery.isWindowsSystemExecutable(file);
  }

  public static @Nullable File findExecutableInDir(@NotNull File dir, @NotNull String baseName) {
    return CompactToolchainDiscovery.findExecutableInDir(dir, baseName);
  }

  public static @Nullable File findExecutableInPath(@NotNull String baseName) {
    return CompactToolchainDiscovery.findExecutableInPath(baseName);
  }
}
