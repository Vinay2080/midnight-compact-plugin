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
 * WSL toolchain discovery engine for detecting Compact compiler installations inside WSL distributions.
 */
public final class CompactWslToolchainDiscovery {

  private CompactWslToolchainDiscovery() {}

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
   * Probes all installed WSL distributions for installed Compact toolchains across user home folders
   * and system directories.
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
        boolean isCli = CompactToolchainDiscovery.isCompactCliName(exe.getName());
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
}
