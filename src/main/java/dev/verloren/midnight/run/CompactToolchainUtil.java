package dev.verloren.midnight.run;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.PathEnvironmentVariableUtil;
import com.intellij.execution.wsl.WSLDistribution;
import com.intellij.execution.wsl.WslDistributionManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import dev.verloren.midnight.settings.MidnightProjectSettings;
import dev.verloren.midnight.settings.MidnightSettingsState;
import dev.verloren.midnight.version.CompactSemVerUtil;
import dev.verloren.midnight.version.CompactVersionManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SequencedMap;

/**
 * Cross-platform discovery and execution utility for the Compact smart contract compiler toolchain.
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

  /**
   * Discovers currently installed WSL distributions on the Windows host.
   * Uses IntelliJ Platform's native {@link WslDistributionManager} to avoid probing non-existent distributions.
   */
  public static @NotNull List<String> getInstalledWslDistributions() {
    if (!SystemInfo.isWindows) {
      return List.of();
    }
    try {
      List<WSLDistribution> distros = WslDistributionManager.getInstance().getInstalledDistributions();
      if (!distros.isEmpty()) {
        List<String> names = new ArrayList<>(distros.size());
        for (WSLDistribution distro : distros) {
          String msId = distro.getMsId();
          if (!msId.isBlank()) {
            names.add(msId.trim());
          }
        }
        if (!names.isEmpty()) {
          return names;
        }
      }
    } catch (Throwable _) {
    }
    // Safe fallback: ONLY check "Ubuntu" if its folder actually exists, avoid probing non-existent distros
    File ubuntuWslLocalhost = new File("\\\\wsl.localhost\\Ubuntu");
    if (ubuntuWslLocalhost.isDirectory()) {
      return List.of("Ubuntu");
    }
    File ubuntuWsl = new File("\\\\wsl$\\Ubuntu");
    if (ubuntuWsl.isDirectory()) {
      return List.of("Ubuntu");
    }
    return List.of();
  }

  /**
   * Resolves the compiler toolchain info using settings, project node_modules, system PATH, package managers, and WSL.
   */
  public static @Nullable ToolchainInfo getToolchainInfo(@Nullable Project project) {
    // 0. Project-level settings (specific version or custom path)
    if (project != null) {
      MidnightProjectSettings projectSettings = MidnightProjectSettings.getInstance(project);
      if (projectSettings != null) {
        if (projectSettings.customCompilerPath != null && !projectSettings.customCompilerPath.trim().isEmpty()) {
          ToolchainInfo customInfo = parseConfiguredPath(projectSettings.customCompilerPath.trim());
          if (customInfo != null) {
            return customInfo;
          }
        }
        if (projectSettings.selectedCompilerVersion != null && !projectSettings.selectedCompilerVersion.trim().isEmpty()) {
          String versionExe = CompactVersionManager.getInstalledExecutable(projectSettings.selectedCompilerVersion.trim());
          if (versionExe != null) {
            CompactVersionManager.cacheExecutableVersion(versionExe, projectSettings.selectedCompilerVersion.trim());
            ToolchainInfo versionInfo = parseConfiguredPath(versionExe);
            if (versionInfo != null) {
              return versionInfo;
            }
            boolean isCli = isCompactCliName(new File(versionExe).getName());
            return new ToolchainInfo(versionExe, false, null, isCli);
          }
        }
      }
    }

    // 1. Explicitly configured path in global settings
    MidnightSettingsState state = MidnightSettingsState.getInstance();
    if (state != null && state.compilerPath != null && !state.compilerPath.trim().isEmpty()) {
      ToolchainInfo customInfo = parseConfiguredPath(state.compilerPath.trim());
      if (customInfo != null) {
        return customInfo;
      }
    }

    // 2. Project local node_modules/.bin/
    if (project != null && project.getBasePath() != null) {
      File nodeModulesBin = new File(project.getBasePath(), "node_modules/.bin");
      if (nodeModulesBin.isDirectory()) {
        File localCompactc = findExecutableInDir(nodeModulesBin, "compactc");
        if (localCompactc != null) {
          return new ToolchainInfo(localCompactc.getAbsolutePath(), false, null, false);
        }
        File localCompact = findExecutableInDir(nodeModulesBin, "compact");
        if (localCompact != null) {
          return new ToolchainInfo(localCompact.getAbsolutePath(), false, null, true);
        }
      }
    }

    // 3. Managed versions installed via plugin (~/.compact/versions/)
    SequencedMap<String, String> installedVersions = CompactVersionManager.getInstalledVersions();
    if (!installedVersions.isEmpty()) {
      String latestExe = installedVersions.firstEntry().getValue();
      ToolchainInfo managedInfo = parseConfiguredPath(latestExe);
      if (managedInfo != null) {
        return managedInfo;
      }
      boolean isCli = isCompactCliName(new File(latestExe).getName());
      return new ToolchainInfo(latestExe, false, null, isCli);
    }

    // 4. On Windows, check WSL first before checking Windows system PATH
    if (SystemInfo.isWindows) {
      ToolchainInfo wslInfo = findInWsl();
      if (wslInfo != null) {
        return wslInfo;
      }
    }

    // 5. System PATH lookup (excluding Windows system/compression executables)
    File pathCompactc = findExecutableInPath("compactc");
    if (pathCompactc != null) {
      return new ToolchainInfo(pathCompactc.getAbsolutePath(), false, null, false);
    }
    File pathCompact = findExecutableInPath("compact");
    if (pathCompact != null) {
      return new ToolchainInfo(pathCompact.getAbsolutePath(), false, null, true);
    }

    // 6. Common package manager locations
    File commonCompactc = findInCommonLocations("compactc");
    if (commonCompactc != null) {
      return new ToolchainInfo(commonCompactc.getAbsolutePath(), false, null, false);
    }
    File commonCompact = findInCommonLocations("compact");
    if (commonCompact != null) {
      return new ToolchainInfo(commonCompact.getAbsolutePath(), false, null, true);
    }

    // 7. Automatic WSL discovery fallback
    if (SystemInfo.isWindows) {
      return findInWsl();
    }

    return null;
  }

  /**
   * Resolves the path to the executable (for display or direct inspection).
   */
  public static @Nullable String getCompilerExecutablePath(@Nullable Project project) {
    ToolchainInfo info = getToolchainInfo(project);
    return info != null ? info.executablePath() : null;
  }

  /**
   * Returns the active compiler version for the project (e.g. "0.34.0", "0.26.0").
   */
  public static @Nullable String getActiveCompilerVersion(@Nullable Project project) {
    if (project != null) {
      MidnightProjectSettings projectSettings = MidnightProjectSettings.getInstance(project);
      if (projectSettings != null && projectSettings.selectedCompilerVersion != null && !projectSettings.selectedCompilerVersion.trim().isEmpty()) {
        String ver = projectSettings.selectedCompilerVersion.trim();
        if (com.intellij.openapi.application.ApplicationManager.getApplication() != null && com.intellij.openapi.application.ApplicationManager.getApplication().isUnitTestMode()) {
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
   * Constructs an executable {@link GeneralCommandLine}, handling WSL arguments, path translations, and CLI commands.
   */
  public static @NotNull GeneralCommandLine createCommandLine(
      @Nullable Project project,
      @NotNull List<String> compilerArgs,
      @Nullable String workingDir
  ) throws ExecutionException {
    ToolchainInfo info = getToolchainInfo(project);
    if (info == null) {
      throw new ExecutionException(
              """
                      Cannot find Compact compiler ('compactc' or 'compact').
                      
                      Please verify that compact / compactc is installed or configure its location:
                        • In WSL: e.g. /home/<user>/.local/bin/compact or /home/<user>/.compact/bin/compactc
                        • In project: npm install --save-dev @midnight-ntwrk/compactc
                        • Globally: npm install -g @midnight-ntwrk/compactc
                        • Configure in IntelliJ: Settings -> Languages & Frameworks -> Midnight Compact"""
      );
    }

    GeneralCommandLine cmd = new GeneralCommandLine();

    if (info.isWsl()) {
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

    } else {
      cmd.setExePath(info.executablePath());
      if (info.isCompactCli()) {
        cmd.addParameter("compile");
      }
      cmd.addParameters(compilerArgs);
    }
    if (workingDir != null && !workingDir.isEmpty()) {
      cmd.setWorkDirectory(workingDir);
    }

    return cmd;
  }

  /**
   * Converts a Windows file path (or UNC WSL path) to its Linux WSL path equivalent.
   */
  public static @NotNull String toWslPath(@NotNull String path) {
    if (path.isEmpty() || path.startsWith("-")) {
      return path;
    }
    String normalized = path.replace('\\', '/');
    if (normalized.startsWith("//wsl$/") || normalized.startsWith("//wsl.localhost/")) {
      int prefixLen = normalized.startsWith("//wsl$/") ? 7 : 16;
      int nextSlash = normalized.indexOf('/', prefixLen);
      if (nextSlash != -1) {
        return normalized.substring(nextSlash);
      }
    }
    if (normalized.length() >= 2 && Character.isLetter(normalized.charAt(0)) && normalized.charAt(1) == ':') {
      char drive = Character.toLowerCase(normalized.charAt(0));
      String rest = normalized.substring(2);
      if (!rest.startsWith("/")) {
        rest = "/" + rest;
      }
      return "/mnt/" + drive + rest;
    }
    return path;
  }

  /**
   * Parses a user-configured path string from settings (which can be a Windows path, WSL path, or UNC path).
   */
  public static @Nullable ToolchainInfo parseConfiguredPath(@NotNull String customPath) {
    String normalized = customPath.replace('\\', '/');

    // WSL UNC path: //wsl$/Ubuntu/home/... or //wsl.localhost/Ubuntu/home/...
    if (normalized.startsWith("//wsl$/") || normalized.startsWith("//wsl.localhost/")) {
      int prefixLen = normalized.startsWith("//wsl$/") ? 7 : 16;
      int nextSlash = normalized.indexOf('/', prefixLen);
      if (nextSlash != -1) {
        String distro = normalized.substring(prefixLen, nextSlash);
        String linuxPath = normalized.substring(nextSlash);
        boolean isCli = isCompactCliName(linuxPath);
        return new ToolchainInfo(linuxPath, true, distro, isCli);
      }
    }

    // Direct Linux / WSL path: /home/... or /usr/... or /root/... or wsl:...
    if (normalized.startsWith("/home/") || normalized.startsWith("/usr/") || normalized.startsWith("/root/") || normalized.startsWith("wsl:")) {
      String linuxPath = normalized.startsWith("wsl:") ? normalized.substring(4) : normalized;
      String distro = null;
      if (linuxPath.contains(":") && !linuxPath.contains(":\\")) {
        int colon = linuxPath.indexOf(':');
        distro = linuxPath.substring(0, colon);
        linuxPath = linuxPath.substring(colon + 1);
      }
      if (distro == null && SystemInfo.isWindows) {
        distro = findWslDistroForPath(linuxPath);
      }
      boolean isCli = isCompactCliName(linuxPath);
      return new ToolchainInfo(linuxPath, true, distro, isCli);
    }

    // Direct host file check (excluding Windows system executables)
    File resolved = findExecutableFile(customPath);
    if (resolved != null && !isWindowsSystemExecutable(resolved)) {
      boolean isCli = isCompactCliName(resolved.getName());
      return new ToolchainInfo(resolved.getAbsolutePath(), false, null, isCli);
    }

    return null;
  }

  public static @Nullable String findWslDistroForPath(@NotNull String linuxPath) {
    String pathWithoutLeadingSlash = linuxPath.startsWith("/") ? linuxPath.substring(1) : linuxPath;
    for (String distro : getInstalledWslDistributions()) {
      File fileWslLocalhost = new File("\\\\wsl.localhost\\" + distro + "\\" + pathWithoutLeadingSlash.replace('/', '\\'));
      if (fileWslLocalhost.exists()) {
        return distro;
      }
      File fileWsl = new File("\\\\wsl$\\" + distro + "\\" + pathWithoutLeadingSlash.replace('/', '\\'));
      if (fileWsl.exists()) {
        return distro;
      }
    }
    return null;
  }

  public static boolean isWindowsSystemExecutable(@NotNull File file) {
    if (!SystemInfo.isWindows) {
      return false;
    }
    String path = file.getAbsolutePath().toLowerCase();
    String winDir = System.getenv("WINDIR");
    if (winDir != null && path.startsWith(winDir.toLowerCase())) {
      return true;
    }
    String systemRoot = System.getenv("SystemRoot");
    if (systemRoot != null && path.startsWith(systemRoot.toLowerCase())) {
      return true;
    }
    return path.contains("\\windows\\system32\\")
        || path.contains("\\windows\\syswow64\\")
        || path.contains("\\windows\\winsxs\\");
  }

  private static boolean isCompactCliName(@NotNull String path) {
    String name = new File(path).getName().toLowerCase();
    return name.equals("compact") || name.startsWith("compact.") || name.equals("compact.exe") || name.equals("compact.cmd");
  }

  /**
   * Probes known WSL locations across installed distributions on Windows.
   */
  private static @Nullable ToolchainInfo findInWsl() {
    for (String distro : getInstalledWslDistributions()) {
      File distroWslLocalhost = new File("\\\\wsl.localhost\\" + distro);
      File distroWsl = new File("\\\\wsl$\\" + distro);
      File base = distroWslLocalhost.isDirectory() ? distroWslLocalhost : (distroWsl.isDirectory() ? distroWsl : null);
      if (base == null) {
        continue;
      }

      File homeDir = new File(base, "home");
      if (homeDir.isDirectory()) {
        File[] users = homeDir.listFiles();
        if (users != null) {
          for (File user : users) {
            if (user.isDirectory()) {
              // 1. Check ~/.compact/bin/
              File compactBinCompactc = new File(user, ".compact/bin/compactc");
              if (compactBinCompactc.isFile()) {
                return new ToolchainInfo("/home/" + user.getName() + "/.compact/bin/compactc", true, distro, false);
              }
              File compactBinCompact = new File(user, ".compact/bin/compact");
              if (compactBinCompact.isFile()) {
                return new ToolchainInfo("/home/" + user.getName() + "/.compact/bin/compact", true, distro, true);
              }

              // 2. Check ~/.compact/versions/*/compactc (the newest installed version)
              File versionsDir = new File(user, ".compact/versions");
              if (versionsDir.isDirectory()) {
                File[] verDirs = versionsDir.listFiles(File::isDirectory);
                if (verDirs != null && verDirs.length > 0) {
                  List<File> sortedVerDirs = new ArrayList<>(Arrays.asList(verDirs));
                  sortedVerDirs.sort((d1, d2) -> {
                    CompactSemVerUtil.SemVer s1 = CompactSemVerUtil.parse(d1.getName());
                    CompactSemVerUtil.SemVer s2 = CompactSemVerUtil.parse(d2.getName());
                    if (s1 != null && s2 != null) return s2.compareTo(s1);
                    return d2.getName().compareTo(d1.getName());
                  });
                  for (File vd : sortedVerDirs) {
                    File exe = CompactVersionManager.findExecutableInVersionDir(vd);
                    if (exe != null) {
                      String linuxPath = toWslPath(exe.getAbsolutePath());
                      boolean isCli = isCompactCliName(exe.getName());
                      return new ToolchainInfo(linuxPath, true, distro, isCli);
                    }
                  }
                }
              }

              // 3. Check ~/.local/bin/
              File localBinCompact = new File(user, ".local/bin/compact");
              if (localBinCompact.isFile()) {
                return new ToolchainInfo("/home/" + user.getName() + "/.local/bin/compact", true, distro, true);
              }
              File localBinCompactc = new File(user, ".local/bin/compactc");
              if (localBinCompactc.isFile()) {
                return new ToolchainInfo("/home/" + user.getName() + "/.local/bin/compactc", true, distro, false);
              }

              // 4. Check ~/.cargo/bin/
              File cargoBinCompact = new File(user, ".cargo/bin/compact");
              if (cargoBinCompact.isFile()) {
                return new ToolchainInfo("/home/" + user.getName() + "/.cargo/bin/compact", true, distro, true);
              }
              File cargoBinCompactc = new File(user, ".cargo/bin/compactc");
              if (cargoBinCompactc.isFile()) {
                return new ToolchainInfo("/home/" + user.getName() + "/.cargo/bin/compactc", true, distro, false);
              }
            }
          }
        }
      }

      // Check /usr/local/bin and /usr/bin
      File usrLocalCompact = new File(base, "usr/local/bin/compact");
      if (usrLocalCompact.isFile()) {
        return new ToolchainInfo("/usr/local/bin/compact", true, distro, true);
      }
      File usrLocalCompactc = new File(base, "usr/local/bin/compactc");
      if (usrLocalCompactc.isFile()) {
        return new ToolchainInfo("/usr/local/bin/compactc", true, distro, false);
      }
      File usrCompact = new File(base, "usr/bin/compact");
      if (usrCompact.isFile()) {
        return new ToolchainInfo("/usr/bin/compact", true, distro, true);
      }
      File usrCompactc = new File(base, "usr/bin/compactc");
      if (usrCompactc.isFile()) {
        return new ToolchainInfo("/usr/bin/compactc", true, distro, false);
      }
    }
    return null;
  }

  public static @Nullable File findExecutableFile(@NotNull String path) {
    File direct = new File(path);
    if (direct.exists() && direct.isFile() && !isWindowsSystemExecutable(direct)) {
      return direct;
    }
    if (SystemInfo.isWindows) {
      String[] exts = {".cmd", ".bat", ".exe", ".ps1"};
      for (String ext : exts) {
        File file = new File(path + ext);
        if (file.exists() && file.isFile() && !isWindowsSystemExecutable(file)) {
          return file;
        }
      }
    }
    return null;
  }

  public static @Nullable File findExecutableInDir(@NotNull File dir, @NotNull String baseName) {
    if (!dir.isDirectory()) {
      return null;
    }
    if (SystemInfo.isWindows) {
      String[] exts = {".cmd", ".bat", ".exe", ""};
      for (String ext : exts) {
        File file = new File(dir, baseName + ext);
        if (file.isFile() && !isWindowsSystemExecutable(file)) {
          return file;
        }
      }
    } else {
      File file = new File(dir, baseName);
      if (file.isFile()) {
        return file;
      }
    }
    return null;
  }

  public static @Nullable File findExecutableInPath(@NotNull String baseName) {
    String pathEnv = PathEnvironmentVariableUtil.getPathVariableValue();
    if (pathEnv == null || pathEnv.isBlank()) {
      pathEnv = System.getenv("PATH");
    }
    if (pathEnv != null && !pathEnv.isBlank()) {
      String[] dirs = pathEnv.split(java.util.regex.Pattern.quote(File.pathSeparator));
      for (String dirPath : dirs) {
        if (dirPath.isBlank()) {
          continue;
        }
        File dir = new File(dirPath.trim());
        File found = findExecutableInDir(dir, baseName);
        if (found != null) {
          return found;
        }
      }
    }
    return null;
  }

  private static @Nullable File findInCommonLocations(@NotNull String baseName) {
    String userHome = System.getProperty("user.home");
    List<File> searchDirs = new ArrayList<>();

    if (SystemInfo.isWindows) {
      String appData = System.getenv("APPDATA");
      if (appData != null) {
        searchDirs.add(new File(appData, "npm"));
      }
      String localAppData = System.getenv("LOCALAPPDATA");
      if (localAppData != null) {
        searchDirs.add(new File(localAppData, "pnpm"));
        searchDirs.add(new File(localAppData, "Yarn/bin"));
      }
      if (userHome != null) {
        searchDirs.add(new File(userHome, "AppData/Roaming/npm"));
        searchDirs.add(new File(userHome, ".cargo/bin"));
      }
    } else {
      searchDirs.add(new File("/usr/local/bin"));
      searchDirs.add(new File("/opt/homebrew/bin"));
      searchDirs.add(new File("/usr/bin"));
      if (userHome != null) {
        searchDirs.add(new File(userHome, ".nvm/versions/node"));
        searchDirs.add(new File(userHome, ".local/bin"));
        searchDirs.add(new File(userHome, ".cargo/bin"));
        searchDirs.add(new File(userHome, ".yarn/bin"));
      }
    }

    for (File dir : searchDirs) {
      if (dir.isDirectory()) {
        File exe = findExecutableInDir(dir, baseName);
        if (exe != null) {
          return exe;
        }
      }
    }
    return null;
  }

  /**
   * Deterministically calculates a unique output directory for a given Compact smart contract file.
   *
   * <p>Examples:
   * <ul>
   *   <li>{@code circuits/calculator.compact} -> {@code gen/calculator}</li>
   *   <li>{@code circuits/counter.compact} -> {@code gen/counter}</li>
   *   <li>{@code circuits/bboard.compact} -> {@code gen/bboard}</li>
   *   <li>{@code circuits/tokens/erc20.compact} -> {@code gen/tokens/erc20}</li>
   *   <li>{@code src/v1/counter.compact} -> {@code gen/v1/counter}</li>
   * </ul>
   *
   * @param project the project context (can be null)
   * @param compactFilePath the source file path (absolute or relative)
   * @return normalized output directory path (e.g. "gen/calculator")
   */
  public static @NotNull String deriveOutputDirectory(@Nullable Project project, @NotNull String compactFilePath) {
    return deriveOutputDirectory(project, compactFilePath, null);
  }

  /**
   * Deterministically calculates a unique output directory for a given Compact smart contract file.
   *
   * @param project the project context (can be null)
   * @param compactFilePath the source file path (absolute or relative)
   * @param baseOutputDir the base output directory (e.g. "gen", or null/empty to use settings/default)
   * @return normalized output directory path (e.g. "gen/calculator")
   */
  public static @NotNull String deriveOutputDirectory(
      @Nullable Project project,
      @NotNull String compactFilePath,
      @Nullable String baseOutputDir
  ) {
    String base = getBase(baseOutputDir);

    String path = compactFilePath.trim().replace('\\', '/');
    if (path.isEmpty()) {
      return base;
    }

    // 1. If absolute path within a project, make it relative to the project base path
    if (project != null && project.getBasePath() != null) {
      String projectPath = project.getBasePath().trim().replace('\\', '/');
      while (projectPath.endsWith("/")) {
        projectPath = projectPath.substring(0, projectPath.length() - 1);
      }
      if (!projectPath.isEmpty() && path.startsWith(projectPath + "/")) {
        path = path.substring(projectPath.length() + 1);
      }
    }

    // 2. Remove drive letters (e.g. "C:") or leading slashes if still present
    if (path.length() >= 2 && Character.isLetter(path.charAt(0)) && path.charAt(1) == ':') {
      path = path.substring(2);
    }
    while (path.startsWith("/")) {
      path = path.substring(1);
    }

    // 3. Strip file extension (.compact, .compactc, or other)
    if (path.endsWith(".compact")) {
      path = path.substring(0, path.length() - ".compact".length());
    } else if (path.endsWith(".compactc")) {
      path = path.substring(0, path.length() - ".compactc".length());
    } else {
      int lastDot = path.lastIndexOf('.');
      int lastSlash = path.lastIndexOf('/');
      if (lastDot > lastSlash && lastDot > 0) {
        path = path.substring(0, lastDot);
      }
    }

    // 4. Strip common top-level source root prefixes to avoid redundant nesting while preserving subdirectories
    String[] sourceRoots = {"circuits", "contracts", "src", "compact", "source", "contract"};
    boolean stripped;
    do {
      stripped = false;
      for (String root : sourceRoots) {
        if (path.startsWith(root + "/") && path.length() > root.length() + 1) {
          path = path.substring(root.length() + 1);
          stripped = true;
          break;
        }
      }
    } while (stripped);

    while (path.startsWith("/")) {
      path = path.substring(1);
    }
    while (path.endsWith("/")) {
      path = path.substring(0, path.length() - 1);
    }

    if (path.isEmpty()) {
      return base;
    }

    return base + "/" + path;
  }

  private static @NonNull String getBase(@org.jspecify.annotations.Nullable String baseOutputDir) {
    String base = (baseOutputDir != null && !baseOutputDir.trim().isEmpty())
        ? baseOutputDir.trim().replace('\\', '/')
        : null;

    if (base == null) {
      MidnightSettingsState state = MidnightSettingsState.getInstance();
      if (state != null && state.defaultOutputDir != null && !state.defaultOutputDir.trim().isEmpty()) {
        base = state.defaultOutputDir.trim().replace('\\', '/');
      } else {
        base = "gen";
      }
    }

    while (base.endsWith("/")) {
      base = base.substring(0, base.length() - 1);
    }
    if (base.isEmpty()) {
      base = "gen";
    }
    return base;
  }
}
