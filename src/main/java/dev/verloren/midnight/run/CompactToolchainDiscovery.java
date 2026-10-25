package dev.verloren.midnight.run;

import com.intellij.execution.configurations.PathEnvironmentVariableUtil;
import com.intellij.execution.wsl.WSLDistribution;
import com.intellij.execution.wsl.WslDistributionManager;
import com.intellij.openapi.util.SystemInfo;
import dev.verloren.midnight.version.CompactSemVerUtil;
import dev.verloren.midnight.version.CompactVersionManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Discovery utility responsible for detecting and resolving the Compact compiler toolchain across
 * diverse host operating systems, WSL distributions, system PATH directories, and package managers.
 *
 * <h3>Architectural Role</h3>
 * <p>Acts as the low-level discovery engine for {@link CompactToolchainUtil}. Implements multi-tier probing
 * for both standalone compiler binaries ({@code compactc}) and the Midnight CLI tool ({@code compact}).
 * Provides robust Windows Subsystem for Linux (WSL) inspection via {@link WslDistributionManager},
 * shields the plugin from Windows' native NTFS compression utility ({@code compact.exe}), and scans
 * common runtime locations across Node/npm, pnpm, Yarn, Homebrew, and Cargo.</p>
 *
 * <h3>Toolchain Discovery Waterfall</h3>
 * <pre>
 *   [CompactToolchainDiscovery.findInWsl / findInCommonLocations]
 *                                │
 *            ┌───────────────────┴───────────────────┐
 *            │                                       │
 *       Windows / WSL                           macOS / Linux
 *            │                                       │
 *            ├─► Probe WslDistributionManager        ├─► /usr/local/bin, /opt/homebrew/bin
 *            ├─► Scan \\wsl.localhost\               ├─► ~/.nvm/versions/node/
 *            │     ├─► ~/.compact/bin                ├─► ~/.local/bin
 *            │     ├─► ~/.compact/versions/          └─► ~/.cargo/bin
 *            │     └─► /usr/local/bin
 *            └─► Check AppData/Roaming/npm
 * </pre>
 *
 * <h3>Windows Native Conflict Guard</h3>
 * <p>Windows includes a native system tool {@code C:\Windows\System32\compact.exe} used for filesystem
 * compression. {@link #isWindowsSystemExecutable(File)} explicitly guards against selecting this binary,
 * preventing accidental invocations of the OS compression utility.</p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>I/O Bound:</b> File system existence checks are performed synchronously. Should be called
 *       from background threads or cached during intensive operations.</li>
 * </ul>
 *
 * @see CompactToolchainUtil
 * @see CompactToolchainPathUtil
 */
public final class CompactToolchainDiscovery {

  /**
   * Private constructor to prevent instantiation of static utility class.
   */
  private CompactToolchainDiscovery() {
  }

  /**
   * Discovers currently installed WSL distributions on the Windows host.
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
    for (String root : new String[]{"\\\\wsl.localhost\\Ubuntu", "\\\\wsl$\\Ubuntu"}) {
      if (new File(root).isDirectory()) {
        return List.of("Ubuntu");
      }
    }
    return List.of();
  }

  /**
   * Parses a user-configured path string from settings, resolving Windows paths, WSL UNC paths,
   * or direct Linux container paths into a structured {@link CompactToolchainUtil.ToolchainInfo}.
   *
   * @param customPath raw path string entered by user
   * @return resolved {@link CompactToolchainUtil.ToolchainInfo}, or {@code null} if file does not exist
   */
  public static @Nullable CompactToolchainUtil.ToolchainInfo parseConfiguredPath(@NotNull String customPath) {
    // 1. Non-Windows POSIX host: directly locate executable
    if (!SystemInfo.isWindows) {
      File hostExe = findExecutableFile(customPath);
      if (hostExe != null && !isWindowsSystemExecutable(hostExe)) {
        boolean isCli = isCompactCliName(hostExe.getName());
        return new CompactToolchainUtil.ToolchainInfo(hostExe.getAbsolutePath(), false, null, isCli);
      }
    }

    String normalized = customPath.replace('\\', '/');

    // 2. Check for WSL UNC paths (//wsl$/... or //wsl.localhost/...)
    CompactToolchainUtil.ToolchainInfo uncInfo = parseWslUncPath(normalized);
    if (uncInfo != null) {
      return uncInfo;
    }

    // 3. Check for direct Linux container paths (/home/..., /usr/..., wsl:...)
    CompactToolchainUtil.ToolchainInfo linuxInfo = parseDirectLinuxOrWslPath(normalized);
    if (linuxInfo != null) {
      return linuxInfo;
    }

    // 4. Fallback to Windows host executable resolution
    File resolved = findExecutableFile(customPath);
    if (resolved != null && !isWindowsSystemExecutable(resolved)) {
      boolean isCli = isCompactCliName(resolved.getName());
      return new CompactToolchainUtil.ToolchainInfo(resolved.getAbsolutePath(), false, null, isCli);
    }
    return null;
  }

  /**
   * Parses WSL network UNC paths ({@code "//wsl$/<distro>/..."} or {@code "//wsl.localhost/<distro>/..."}).
   *
   * @param normalized forward-slash normalized path string
   * @return {@link CompactToolchainUtil.ToolchainInfo} if valid WSL UNC path; {@code null} otherwise
   */
  private static @Nullable CompactToolchainUtil.ToolchainInfo parseWslUncPath(@NotNull String normalized) {
    if (!normalized.startsWith("//wsl$/") && !normalized.startsWith("//wsl.localhost/")) {
      return null;
    }
    int prefixLen = normalized.startsWith("//wsl$/") ? 7 : 16;
    int nextSlash = normalized.indexOf('/', prefixLen);
    if (nextSlash != -1) {
      String distro = normalized.substring(prefixLen, nextSlash);
      String linuxPath = normalized.substring(nextSlash);
      boolean isCli = isCompactCliName(linuxPath);
      return new CompactToolchainUtil.ToolchainInfo(linuxPath, true, distro, isCli);
    }
    return null;
  }

  /**
   * Parses direct Linux container paths (e.g. {@code "/home/user/.local/bin/compact"} or {@code "wsl:Ubuntu:/usr/bin/compact"}).
   *
   * @param normalized forward-slash normalized path string
   * @return {@link CompactToolchainUtil.ToolchainInfo} if recognized Linux/WSL format; {@code null} otherwise
   */
  private static @Nullable CompactToolchainUtil.ToolchainInfo parseDirectLinuxOrWslPath(@NotNull String normalized) {
    if (!normalized.startsWith("/home/") && !normalized.startsWith("/usr/")
        && !normalized.startsWith("/root/") && !normalized.startsWith("wsl:")) {
      return null;
    }
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
    return new CompactToolchainUtil.ToolchainInfo(linuxPath, true, distro, isCli);
  }

  /**
   * Scans installed WSL distributions to find which distribution contains the specified Linux path.
   *
   * @param linuxPath absolute Linux path (e.g. {@code "/home/alice/.compact/bin/compactc"})
   * @return the name of the containing WSL distribution (e.g. {@code "Ubuntu"}), or {@code null} if not found
   */
  public static @Nullable String findWslDistroForPath(@NotNull String linuxPath) {
    String subPath = linuxPath.startsWith("/") ? linuxPath.substring(1) : linuxPath;
    String winSubPath = subPath.replace('/', '\\');
    for (String distro : getInstalledWslDistributions()) {
      for (String prefix : new String[]{"\\\\wsl.localhost\\", "\\\\wsl$\\"}) {
        if (new File(prefix + distro + "\\" + winSubPath).exists()) {
          return distro;
        }
      }
    }
    return null;
  }

  /**
   * Guards against accidentally invoking Windows' native NTFS file compression utility ({@code C:\Windows\System32\compact.exe}).
   *
   * @param file the candidate executable file
   * @return {@code true} if {@code file} resides in a Windows system directory; {@code false} otherwise
   */
  public static boolean isWindowsSystemExecutable(@NotNull File file) {
    if (!SystemInfo.isWindows) {
      return false;
    }
    String path = file.getAbsolutePath().toLowerCase().replace('/', '\\');
    for (String env : new String[]{System.getenv("WINDIR"), System.getenv("SystemRoot")}) {
      if (env != null) {
        String norm = env.toLowerCase().replace('/', '\\');
        norm = norm.endsWith("\\") ? norm : norm + "\\";
        if (path.startsWith(norm) || path.equalsIgnoreCase(env)) {
          return true;
        }
      }
    }
    return path.contains("\\windows\\system32\\")
        || path.contains("\\windows\\syswow64\\")
        || path.contains("\\windows\\winsxs\\");
  }

  /**
   * Checks whether the given executable name corresponds to the higher-level Midnight {@code compact} CLI
   * rather than the low-level {@code compactc} compiler binary.
   *
   * @param path file path or executable name to test
   * @return {@code true} if the binary represents {@code compact} CLI; {@code false} if {@code compactc}
   */
  public static boolean isCompactCliName(@NotNull String path) {
    String name = new File(path).getName().toLowerCase();
    return name.equals("compact") || name.startsWith("compact.")
        || name.equals("compact.exe") || name.equals("compact.cmd");
  }

  /**
   * Probes all installed WSL distributions for installed Compact toolchains across user home folders
   * (e.g. {@code ~/.compact/bin}, {@code ~/.local/bin}, {@code ~/.cargo/bin}) and system directories.
   *
   * @return {@link CompactToolchainUtil.ToolchainInfo} if discovered in WSL; {@code null} otherwise
   */
  public static @Nullable CompactToolchainUtil.ToolchainInfo findInWsl() {
    for (String distro : getInstalledWslDistributions()) {
      File distroWslLocalhost = new File("\\\\wsl.localhost\\" + distro);
      File distroWsl = new File("\\\\wsl$\\" + distro);
      File base = distroWslLocalhost.isDirectory() ? distroWslLocalhost : (distroWsl.isDirectory() ? distroWsl : null);
      if (base == null) {
        continue;
      }
      File homeDir = new File(base, "home");
      if (homeDir.isDirectory()) {
        CompactToolchainUtil.ToolchainInfo userToolchain = findInWslUserDirs(distro, homeDir);
        if (userToolchain != null) {
          return userToolchain;
        }
      }
      CompactToolchainUtil.ToolchainInfo sysToolchain = findInWslSystemDirs(distro, base);
      if (sysToolchain != null) {
        return sysToolchain;
      }
    }
    return null;
  }

  private static @Nullable CompactToolchainUtil.ToolchainInfo findInWslUserDirs(
      @NotNull String distro,
      @NotNull File homeDir
  ) {
    File[] users = homeDir.listFiles();
    if (users == null) {
      return null;
    }
    String[] relDirs = {".compact/bin", ".local/bin", ".cargo/bin"};
    for (File user : users) {
      if (!user.isDirectory()) {
        continue;
      }
      for (String relDir : relDirs) {
        CompactToolchainUtil.ToolchainInfo info = findInUserBin(distro, user, relDir);
        if (info != null) {
          return info;
        }
        if (relDir.equals(".compact/bin")) {
          CompactToolchainUtil.ToolchainInfo versionInfo = findInWslVersionsDir(distro, user);
          if (versionInfo != null) {
            return versionInfo;
          }
        }
      }
    }
    return null;
  }

  private static @Nullable CompactToolchainUtil.ToolchainInfo findInUserBin(
      @NotNull String distro,
      @NotNull File user,
      @NotNull String relDir
  ) {
    File compactc = new File(user, relDir + "/compactc");
    if (compactc.isFile()) {
      return new CompactToolchainUtil.ToolchainInfo("/home/" + user.getName() + "/" + relDir + "/compactc", true, distro, false);
    }
    File compact = new File(user, relDir + "/compact");
    if (compact.isFile()) {
      return new CompactToolchainUtil.ToolchainInfo("/home/" + user.getName() + "/" + relDir + "/compact", true, distro, true);
    }
    return null;
  }

  private static @Nullable CompactToolchainUtil.ToolchainInfo findInWslVersionsDir(
      @NotNull String distro,
      @NotNull File user
  ) {
    File versionsDir = new File(user, ".compact/versions");
    if (!versionsDir.isDirectory()) {
      return null;
    }
    File[] verDirs = versionsDir.listFiles(File::isDirectory);
    if (verDirs == null || verDirs.length == 0) {
      return null;
    }
    Arrays.sort(verDirs, (d1, d2) -> {
      CompactSemVerUtil.SemVer s1 = CompactSemVerUtil.parse(d1.getName());
      CompactSemVerUtil.SemVer s2 = CompactSemVerUtil.parse(d2.getName());
      if (s1 != null && s2 != null) {
        return s2.compareTo(s1);
      }
      return d2.getName().compareTo(d1.getName());
    });
    for (File vd : verDirs) {
      File exe = CompactVersionManager.findExecutableInVersionDir(vd);
      if (exe != null) {
        String linuxPath = CompactToolchainPathUtil.toWslPath(exe.getAbsolutePath());
        boolean isCli = isCompactCliName(exe.getName());
        return new CompactToolchainUtil.ToolchainInfo(linuxPath, true, distro, isCli);
      }
    }
    return null;
  }

  private static @Nullable CompactToolchainUtil.ToolchainInfo findInWslSystemDirs(
      @NotNull String distro,
      @NotNull File base
  ) {
    for (String dir : new String[]{"usr/local/bin", "usr/bin"}) {
      File compact = new File(base, dir + "/compact");
      if (compact.isFile()) {
        return new CompactToolchainUtil.ToolchainInfo("/" + dir + "/compact", true, distro, true);
      }
      File compactc = new File(base, dir + "/compactc");
      if (compactc.isFile()) {
        return new CompactToolchainUtil.ToolchainInfo("/" + dir + "/compactc", true, distro, false);
      }
    }
    return null;
  }

  /**
   * Resolves an executable file from a direct path, automatically appending platform extensions
   * ({@code .cmd}, {@code .bat}, {@code .exe}, {@code .ps1} on Windows) if omitted.
   *
   * @param path file path string
   * @return resolved {@link File} object, or {@code null} if no matching executable exists
   */
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

  /**
   * Searches a specific directory for an executable with the specified base name, checking platform extensions.
   *
   * @param dir target directory
   * @param baseName binary name without extension (e.g. {@code "compactc"})
   * @return resolved {@link File}, or {@code null} if not found
   */
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

  /**
   * Scans system PATH directories to locate an executable with the given base name.
   *
   * @param baseName binary name (e.g. {@code "compact"} or {@code "compactc"})
   * @return resolved {@link File} on PATH, or {@code null} if not present
   */
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

  /**
   * Probes conventional global toolchain installations across Node/npm, pnpm, Yarn, Homebrew, and Cargo.
   *
   * @param baseName binary name (e.g. {@code "compact"} or {@code "compactc"})
   * @return resolved {@link File} in standard locations, or {@code null} if not found
   */
  public static @Nullable File findInCommonLocations(@NotNull String baseName) {
    for (File dir : getCommonLocationsSearchDirs()) {
      if (dir.isDirectory()) {
        File exe = findExecutableInDir(dir, baseName);
        if (exe != null) {
          return exe;
        }
      }
    }
    return null;
  }

  private static @NotNull List<File> getCommonLocationsSearchDirs() {
    String userHome = System.getProperty("user.home");
    List<File> searchDirs = new ArrayList<>();
    if (SystemInfo.isWindows) {
      String appData = System.getenv("APPDATA");
      if (appData != null) searchDirs.add(new File(appData, "npm"));
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
    return searchDirs;
  }
}
