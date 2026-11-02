package dev.verloren.midnight.run;

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
 * WSL toolchain discovery engine for detecting Compact compiler installations inside Windows Subsystem for Linux (WSL) distributions.
 *
 * <h3>Architectural Role</h3>
 * <p>On Windows hosts, developers frequently run the Midnight Compact toolchain (or Linux ELF binaries of {@code compactc})
 * inside a WSL Linux environment (such as Ubuntu). This utility discovers installed WSL distributions via IntelliJ's
 * {@link WslDistributionManager} or UNC network path heuristics ({@code \\wsl.localhost} and {@code \\wsl$}), then probes
 * common user home and system paths for {@code compactc} (standalone compiler) or {@code compact} (Midnight CLI wrapper).</p>
 *
 * <h3>Discovery Strategy &amp; Priority</h3>
 * <ol>
 *   <li><b>WSL Distribution Discovery:</b> Query {@link WslDistributionManager} for registered distributions; fall back to checking {@code \\wsl.localhost\Ubuntu} and {@code \\wsl$\Ubuntu}.</li>
 *   <li><b>User-Level Binaries:</b> Probe {@code /home/<user>/} directories for:
 *     <ul>
 *       <li>{@code .compact/bin/} &mdash; official Midnight installer location</li>
 *       <li>{@code .compact/versions/} &mdash; plugin or multi-version managed directories, sorted by descending SemVer</li>
 *       <li>{@code .local/bin/} &mdash; standard Linux user binary directory (pip, npm, cargo)</li>
 *       <li>{@code .cargo/bin/} &mdash; Rust/Cargo installations</li>
 *     </ul>
 *   </li>
 *   <li><b>System-Level Binaries:</b> Probe {@code /usr/local/bin} and {@code /usr/bin}.</li>
 * </ol>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Thread Safety:</b> Stateless utility class with static methods only. Safe to invoke from background execution or pooled worker threads.</li>
 * </ul>
 *
 * @see CompactToolchainUtil
 * @see CompactToolchainDiscovery
 * @see CompactToolchainPathUtil
 */
public final class CompactWslToolchainDiscovery {

  /**
   * Private constructor to prevent instantiation of static utility class.
   */
  private CompactWslToolchainDiscovery() {}

  /**
   * Discovers currently installed WSL distributions on the Windows host.
   *
   * <p>First attempts detection using the IntelliJ Platform {@link WslDistributionManager}. If the manager is
   * unavailable or yields no results (such as in headless testing or mock environments), it falls back to inspecting
   * standard UNC network mount roots ({@code \\wsl.localhost} and {@code \\wsl$}).</p>
   *
   * @return ordered list of installed WSL distribution names (e.g. {@code ["Ubuntu"]}), or empty list on non-Windows hosts
   */
  public static @NotNull List<String> getInstalledWslDistributions() {
    // WSL is exclusively available on Windows hosts
    if (!SystemInfo.isWindows) {
      return List.of();
    }

    // 1. Primary: Use IntelliJ Platform WslDistributionManager to discover registered distributions
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
      // Swallowed: WslDistributionManager might throw in headless test sandboxes or when WSL service is stopped
    }

    // 2. Fallback: Probe UNC virtual file system shares exposed by the Windows 10/11 WSL 9P/P9 server
    for (String root : new String[]{"\\\\wsl.localhost\\Ubuntu", "\\\\wsl$\\Ubuntu"}) {
      if (new File(root).isDirectory()) {
        return List.of("Ubuntu");
      }
    }
    return List.of();
  }

  /**
   * Probes all installed WSL distributions for installed Compact toolchains across user home folders
   * and system directories.
   *
   * @return {@link CompactToolchainUtil.ToolchainInfo} if discovered in WSL; {@code null} otherwise
   */
  public static @Nullable CompactToolchainUtil.ToolchainInfo findInWsl() {
    for (String distro : getInstalledWslDistributions()) {
      // Windows 11 uses \\wsl.localhost\<distro>, while older Windows 10 builds use \\wsl$\<distro>
      File distroWslLocalhost = new File("\\\\wsl.localhost\\" + distro);
      File distroWsl = new File("\\\\wsl$\\" + distro);
      File base = distroWslLocalhost.isDirectory() ? distroWslLocalhost : (distroWsl.isDirectory() ? distroWsl : null);
      if (base == null) {
        continue;
      }

      // Priority 1: User home directories (where developers install standalone tools, ~/.compact, ~/.local)
      File homeDir = new File(base, "home");
      if (homeDir.isDirectory()) {
        CompactToolchainUtil.ToolchainInfo userToolchain = findInWslUserDirs(distro, homeDir);
        if (userToolchain != null) {
          return userToolchain;
        }
      }

      // Priority 2: System directories (/usr/local/bin, /usr/bin)
      CompactToolchainUtil.ToolchainInfo sysToolchain = findInWslSystemDirs(distro, base);
      if (sysToolchain != null) {
        return sysToolchain;
      }
    }
    return null;
  }

  /**
   * Scans user home directories in {@code /home/*} across common candidate subdirectories
   * (e.g. {@code .compact/bin}, {@code .local/bin}, {@code .cargo/bin}).
   *
   * @param distro target WSL distribution name (e.g. "Ubuntu")
   * @param homeDir Windows File pointing to the UNC mount of the {@code /home} directory
   * @return {@link CompactToolchainUtil.ToolchainInfo} if found, or {@code null}
   */
  private static @Nullable CompactToolchainUtil.ToolchainInfo findInWslUserDirs(
      @NotNull String distro,
      @NotNull File homeDir
  ) {
    File[] users = homeDir.listFiles();
    if (users == null) {
      return null;
    }

    // Relative subdirectories within each user's home where compiler binaries are customarily placed
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
        // If probing the .compact/bin root did not find a direct binary, check if managed versions exist in ~/.compact/versions
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

  /**
   * Checks a specific relative directory within a user's home folder for {@code compactc} or {@code compact}.
   *
   * @param distro target WSL distribution name
   * @param user   Windows File pointing to the specific user's home folder on the UNC mount
   * @param relDir relative sub-path from user home (e.g. ".compact/bin")
   * @return {@link CompactToolchainUtil.ToolchainInfo} if executable file exists, or {@code null}
   */
  private static @Nullable CompactToolchainUtil.ToolchainInfo findInUserBin(
      @NotNull String distro,
      @NotNull File user,
      @NotNull String relDir
  ) {
    // Prefer standalone compiler 'compactc' over CLI wrapper 'compact'
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

  /**
   * Probes the multi-version storage directory ({@code ~/.compact/versions/}) inside a WSL user's home folder,
   * selecting the latest compiler version by descending SemVer.
   *
   * @param distro target WSL distribution name
   * @param user   Windows File pointing to the user's home directory on the UNC mount
   * @return {@link CompactToolchainUtil.ToolchainInfo} for the highest SemVer executable found, or {@code null}
   */
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

    // Sort version directories in descending order so the highest SemVer release takes precedence
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
        // Convert the host UNC path (e.g. //wsl.localhost/Ubuntu/home/user/.compact/versions/...) to Linux path (/home/user/...)
        String linuxPath = CompactToolchainPathUtil.toWslPath(exe.getAbsolutePath());
        boolean isCli = CompactToolchainDiscovery.isCompactCliName(exe.getName());
        return new CompactToolchainUtil.ToolchainInfo(linuxPath, true, distro, isCli);
      }
    }
    return null;
  }

  /**
   * Probes common Linux system directories ({@code /usr/local/bin} and {@code /usr/bin}) on the WSL root mount.
   *
   * @param distro target WSL distribution name
   * @param base   Windows File pointing to the root UNC mount of the WSL filesystem
   * @return {@link CompactToolchainUtil.ToolchainInfo} if system binary exists, or {@code null}
   */
  private static @Nullable CompactToolchainUtil.ToolchainInfo findInWslSystemDirs(
      @NotNull String distro,
      @NotNull File base
  ) {
    for (String dir : new String[]{"usr/local/bin", "usr/bin"}) {
      // In system directories, check for package-managed 'compact' CLI or 'compactc'
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
}
