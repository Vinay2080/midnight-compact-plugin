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
 * Cross-platform discovery, coordinator, and execution utility for the Compact smart contract compiler toolchain.
 *
 * <p>Supports both the official {@code compact} CLI tool (e.g. {@code compact compile ...}) and direct {@code compactc}
 * compilers across Windows, WSL (Windows Subsystem for Linux), macOS, and Linux.</p>
 */
public final class CompactToolchainUtil {

  /**
   * Encapsulates discovered compiler toolchain metadata.
   */
  public record ToolchainInfo(
      @NotNull String executablePath,
      boolean isWsl,
      @Nullable String wslDistribution,
      boolean isCompactCli
  ) {
    public boolean isValid() {
      return !executablePath.isEmpty();
    }
  }

  private CompactToolchainUtil() {
  }

  public static @NotNull List<String> getInstalledWslDistributions() {
    return CompactToolchainDiscovery.getInstalledWslDistributions();
  }

  /**
   * Resolves compiler toolchain info using settings, project node_modules, system PATH, package managers, and WSL.
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

  public static @Nullable String getCompilerExecutablePath(@Nullable Project project) {
    ToolchainInfo info = getToolchainInfo(project);
    return info != null ? info.executablePath() : null;
  }

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

  public static @NotNull GeneralCommandLine createCommandLine(
      @Nullable Project project,
      @NotNull List<String> compilerArgs,
      @Nullable String workingDir
  ) throws ExecutionException {
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

  public static @Nullable String findWslDistroForPath(@NotNull String linuxPath) {
    return CompactToolchainDiscovery.findWslDistroForPath(linuxPath);
  }

  public static boolean isWindowsSystemExecutable(@NotNull File file) {
    return CompactToolchainDiscovery.isWindowsSystemExecutable(file);
  }

  public static @Nullable File findExecutableFile(@NotNull String path) {
    return CompactToolchainDiscovery.findExecutableFile(path);
  }

  public static @Nullable File findExecutableInDir(@NotNull File dir, @NotNull String baseName) {
    return CompactToolchainDiscovery.findExecutableInDir(dir, baseName);
  }

  public static @Nullable File findExecutableInPath(@NotNull String baseName) {
    return CompactToolchainDiscovery.findExecutableInPath(baseName);
  }
}
