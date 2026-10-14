package dev.verloren.midnight.version;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.CapturingProcessHandler;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import dev.verloren.midnight.run.CompactToolchainUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages WSL storage discovery, path mapping, and legacy Windows toolchain migration.
 */
public final class CompactWslStorageHelper {
  private static final Logger LOG = Logger.getInstance(CompactWslStorageHelper.class);
  private static final AtomicBoolean MIGRATION_DONE = new AtomicBoolean(false);

  private CompactWslStorageHelper() {
  }

  public static @Nullable String getWslDistribution() {
    if (!SystemInfo.isWindows) {
      return null;
    }
    List<String> distros = CompactToolchainUtil.getInstalledWslDistributions();
    return distros.isEmpty() ? null : distros.getFirst();
  }

  public static @Nullable String getWslUser(@Nullable String distro) {
    if (!SystemInfo.isWindows || distro == null) {
      return null;
    }
    File homeDir = new File("\\\\wsl.localhost\\" + distro + "\\home");
    if (!homeDir.isDirectory()) {
      homeDir = new File("\\\\wsl$\\" + distro + "\\home");
    }
    if (!homeDir.isDirectory()) {
      return null;
    }
    File[] users = homeDir.listFiles(File::isDirectory);
    if (users == null) {
      return null;
    }
    for (File user : users) {
      if (!"lost+found".equalsIgnoreCase(user.getName())) {
        return user.getName();
      }
    }
    return null;
  }

  public static @Nullable File getWslVersionsDirectory() {
    String distro = getWslDistribution();
    if (distro == null) return null;
    String user = getWslUser(distro);
    if (user == null) return null;

    File dir = new File("\\\\wsl.localhost\\" + distro + "\\home\\" + user + "\\.compact\\versions");
    if (dir.exists() || dir.mkdirs()) {
      return dir;
    }
    File fallback = new File("\\\\wsl$\\" + distro + "\\home\\" + user + "\\.compact\\versions");
    if (fallback.exists() || fallback.mkdirs()) {
      return fallback;
    }
    return null;
  }

  public static void migrateWindowsVersionsToWslIfNeeded(@NotNull File wslVersionsDir) {
    if (!SystemInfo.isWindows || !MIGRATION_DONE.compareAndSet(false, true)) {
      return;
    }
    Application app = ApplicationManager.getApplication();
    if (app != null && (app.isDispatchThread() || app.isReadAccessAllowed())) {
      app.executeOnPooledThread(() -> doMigrateWindowsVersionsToWsl(wslVersionsDir));
    } else {
      doMigrateWindowsVersionsToWsl(wslVersionsDir);
    }
  }

  private static void doMigrateWindowsVersionsToWsl(@NotNull File wslVersionsDir) {
    String userHome = System.getProperty("user.home", ".");
    File winBase = new File(new File(userHome, ".compact"), "versions");
    if (!winBase.isDirectory()) return;

    File[] winVersions = winBase.listFiles(File::isDirectory);
    if (winVersions == null || winVersions.length == 0) return;

    for (File winVer : winVersions) {
      File targetVerDir = new File(wslVersionsDir, winVer.getName());
      try {
        File[] targetFiles = targetVerDir.listFiles();
        if (!targetVerDir.exists() || targetFiles == null || targetFiles.length == 0) {
          LOG.info("Migrating legacy Windows version to WSL: " + winVer.getName());
          FileUtil.copyDir(winVer, targetVerDir);
        }
        FileUtil.delete(winVer);
      } catch (Exception e) {
        LOG.warn("Failed to migrate " + winVer.getName() + " to WSL: " + e.getMessage());
      }
    }

    chmodWslDirectory();
  }

  public static void chmodWslDirectory() {
    if (!SystemInfo.isWindows) return;
    String distro = getWslDistribution();
    String user = getWslUser(distro);
    if (distro == null || user == null) return;

    Runnable r = () -> {
      try {
        GeneralCommandLine cmd = new GeneralCommandLine("wsl.exe", "-d", distro, "-e", "bash", "-c",
            "chmod -R +x /home/" + user + "/.compact/versions 2>/dev/null || true");
        new CapturingProcessHandler(cmd).runProcess(10_000);
      } catch (Exception _) {
      }
    };
    Application app = ApplicationManager.getApplication();
    if (app != null && (app.isDispatchThread() || app.isReadAccessAllowed())) {
      app.executeOnPooledThread(r);
    } else {
      r.run();
    }
  }
}
