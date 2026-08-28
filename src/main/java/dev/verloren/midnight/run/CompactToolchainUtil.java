package dev.verloren.midnight.run;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.PathEnvironmentVariableUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import dev.verloren.midnight.settings.MidnightSettingsState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Cross-platform discovery and execution utility for the Compact smart contract compiler toolchain.
 *
 * <p>Supports both the official {@code compact} CLI tool (e.g. {@code compact compile ...}) and direct {@code compactc}
 * compilers across Windows, WSL (Windows Subsystem for Linux), macOS, and Linux.
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

  private static final String[] COMMON_WSL_DISTROS = {
      "Ubuntu", "Ubuntu-24.04", "Ubuntu-22.04", "Ubuntu-20.04", "Debian", "kali-linux", "openSUSE", "Arch"
  };

  private CompactToolchainUtil() {
  }

  /**
   * Resolves the compiler toolchain info using settings, project node_modules, system PATH, package managers, and WSL.
   */
  public static @Nullable ToolchainInfo getToolchainInfo(@Nullable Project project) {
    // 1. Explicitly configured path in settings
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

    // 3. System PATH lookup
    File pathCompactc = findExecutableInPath("compactc");
    if (pathCompactc != null) {
      return new ToolchainInfo(pathCompactc.getAbsolutePath(), false, null, false);
    }
    File pathCompact = findExecutableInPath("compact");
    if (pathCompact != null) {
      return new ToolchainInfo(pathCompact.getAbsolutePath(), false, null, true);
    }

    // 4. Common package manager locations
    File commonCompactc = findInCommonLocations("compactc");
    if (commonCompactc != null) {
      return new ToolchainInfo(commonCompactc.getAbsolutePath(), false, null, false);
    }
    File commonCompact = findInCommonLocations("compact");
    if (commonCompact != null) {
      return new ToolchainInfo(commonCompact.getAbsolutePath(), false, null, true);
    }

    // 5. Automatic WSL discovery (on Windows)
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
                        • In WSL: e.g. /home/<user>/.local/bin/compact
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

    // Direct Linux / WSL path: /home/... or /usr/... or wsl:...
    if (normalized.startsWith("/home/") || normalized.startsWith("/usr/") || normalized.startsWith("/root/") || normalized.startsWith("wsl:")) {
      String linuxPath = normalized.startsWith("wsl:") ? normalized.substring(4) : normalized;
      String distro = null;
      if (linuxPath.contains(":") && !linuxPath.contains(":\\")) {
        int colon = linuxPath.indexOf(':');
        distro = linuxPath.substring(0, colon);
        linuxPath = linuxPath.substring(colon + 1);
      }
      boolean isCli = isCompactCliName(linuxPath);
      return new ToolchainInfo(linuxPath, true, distro, isCli);
    }

    // Direct host file check
    File resolved = findExecutableFile(customPath);
    if (resolved != null) {
      boolean isCli = isCompactCliName(resolved.getName());
      return new ToolchainInfo(resolved.getAbsolutePath(), false, null, isCli);
    }

    return null;
  }

  private static boolean isCompactCliName(@NotNull String path) {
    String name = new File(path).getName().toLowerCase();
    return name.equals("compact") || name.startsWith("compact.") || name.equals("compact.exe") || name.equals("compact.cmd");
  }

  /**
   * Probes known WSL locations across installed distributions on Windows.
   */
  private static @Nullable ToolchainInfo findInWsl() {
    for (String distro : COMMON_WSL_DISTROS) {
      File distroWsl = new File("\\\\wsl$\\" + distro);
      File distroWslLocalhost = new File("\\\\wsl.localhost\\" + distro);
      File base = distroWsl.isDirectory() ? distroWsl : (distroWslLocalhost.isDirectory() ? distroWslLocalhost : null);
      if (base == null) {
        continue;
      }

      // Check /home/<user>/.local/bin
      File homeDir = new File(base, "home");
      if (homeDir.isDirectory()) {
        File[] users = homeDir.listFiles();
        if (users != null) {
          for (File user : users) {
            if (user.isDirectory()) {
              File localBinCompact = new File(user, ".local/bin/compact");
              if (localBinCompact.isFile()) {
                return new ToolchainInfo("/home/" + user.getName() + "/.local/bin/compact", true, distro, true);
              }
              File localBinCompactc = new File(user, ".local/bin/compactc");
              if (localBinCompactc.isFile()) {
                return new ToolchainInfo("/home/" + user.getName() + "/.local/bin/compactc", true, distro, false);
              }
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
    if (direct.exists() && direct.isFile()) {
      return direct;
    }
    if (SystemInfo.isWindows) {
      String[] exts = {".cmd", ".bat", ".exe", ".ps1"};
      for (String ext : exts) {
        File file = new File(path + ext);
        if (file.exists() && file.isFile()) {
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
        if (file.isFile()) {
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
    if (SystemInfo.isWindows) {
      String[] names = {baseName + ".cmd", baseName + ".bat", baseName + ".exe", baseName};
      for (String name : names) {
        File found = PathEnvironmentVariableUtil.findInPath(name);
        if (found != null && found.isFile()) {
          return found;
        }
      }
    } else {
      File found = PathEnvironmentVariableUtil.findInPath(baseName);
      if (found != null && found.isFile()) {
        return found;
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
