package dev.verloren.midnight.version;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.CapturingProcessHandler;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import dev.verloren.midnight.annotator.CompactProblemUtil;
import dev.verloren.midnight.run.CompactToolchainUtil;
import dev.verloren.midnight.settings.MidnightProjectSettings;
import dev.verloren.midnight.settings.MidnightSettingsState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Manages isolated Compact compiler versions installed under {@code ~/.compact/versions/<version>/}.
 *
 * <p>Supports official Midnight toolchains from GitHub Releases ({@code midnightntwrk/compact})
 * and official yarn CLI fetching ({@code yarn fetch-compactc}).</p>
 *
 * <p>Cross-platform support: Compact compiler binaries are Linux/macOS only. On Windows,
 * the toolchain runs within WSL (Windows Subsystem for Linux), storing versions under
 * WSL's {@code ~/.compact/versions/<version>/}. Any legacy Windows versions are automatically
 * migrated into WSL storage.</p>
 */
public final class CompactVersionManager {
  private static final Logger LOG = Logger.getInstance(CompactVersionManager.class);
  private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+\\.\\d+\\.\\d+)");

  private static final Map<String, String> EXECUTABLE_VERSION_CACHE = new ConcurrentHashMap<>();
  private static final Set<String> PENDING_DETECTIONS = ConcurrentHashMap.newKeySet();
  private static final AtomicBoolean MIGRATION_DONE = new AtomicBoolean(false);

  /**
   * Officially supported Midnight toolchain releases.
   */
  public static final List<String> KNOWN_VERSIONS = List.of(
      "0.34.0", // Compact language 0.26.0 (Midnight Ledger 9) - Latest
      "0.31.1", // Compact language 0.23.0 (Midnight Ledger 8)
      "0.31.0", // Compact language 0.23.0 (Midnight Ledger 8)
      "0.30.0", // Compact language 0.22.0 (Midnight Ledger 7)
      "0.29.0", // Compact language 0.21.0
      "0.28.0", // Compact language 0.20.0
      "0.26.0", // Compact language 0.18.0 (Legacy Toolchain)
      "0.25.0", // Compact language 0.17.0 (Legacy Toolchain)
      "0.24.0", // Compact language 0.16.0 (Legacy Toolchain)
      "0.23.0", // Compact language 0.15.0 (Legacy Toolchain)
      "0.22.0"  // Compact language 0.14.0 (Legacy Toolchain)
  );

  private CompactVersionManager() {
  }

  public static void cacheExecutableVersion(@NotNull String executablePath, @NotNull String version) {
    EXECUTABLE_VERSION_CACHE.put(executablePath, version);
  }

  /**
   * Returns the base directory where isolated versions are stored.
   * On Windows with WSL, points to WSL's {@code ~/.compact/versions/}.
   * On Linux/macOS, points to local {@code ~/.compact/versions/}.
   */
  public static @NotNull File getVersionsDirectory() {
    if (SystemInfo.isWindows) {
      File wslDir = getWslVersionsDirectory();
      if (wslDir != null) {
        migrateWindowsVersionsToWslIfNeeded(wslDir);
        return wslDir;
      }
    }
    String userHome = System.getProperty("user.home", ".");
    return new File(new File(userHome, ".compact"), "versions");
  }

  /**
   * Returns the directory for a specific version: {@code ~/.compact/versions/<version>/}.
   */
  public static @NotNull File getVersionDirectory(@NotNull String version) {
    return new File(getVersionsDirectory(), cleanVersion(version));
  }

  /**
   * Cleans a version string, removing prefixes like 'v' or whitespace.
   */
  public static @NotNull String cleanVersion(@NotNull String version) {
    Matcher m = VERSION_PATTERN.matcher(version.trim());
    if (m.find()) {
      return m.group(1);
    }
    return version.trim().replaceAll("^v", "");
  }

  /**
   * Resolves the official compiler toolchain version for a given Compact language version.
   * E.g. language version 0.26.0 maps to toolchain 0.34.0.
   */
  public static @NotNull String resolveToolchainVersionForLanguage(@NotNull String languageVersion) {
    String clean = cleanVersion(languageVersion);
    return switch (clean) {
      case "0.26.0" -> "0.34.0";
      case "0.23.0" -> "0.31.1";
      case "0.22.0" -> "0.30.0";
      case "0.21.0" -> "0.29.0";
      case "0.20.0" -> "0.28.0";
      case "0.18.0" -> "0.26.0";
      case "0.17.0" -> "0.25.0";
      case "0.16.0" -> "0.24.0";
      case "0.15.0" -> "0.23.0";
      case "0.14.0" -> "0.22.0";
      default -> clean;
    };
  }

  /**
   * Returns the Compact language version supported by a toolchain release.
   */
  public static @NotNull String getLanguageVersionForToolchain(@NotNull String toolchainVersion) {
    String clean = cleanVersion(toolchainVersion);
    return switch (clean) {
      case "0.34.0" -> "0.26.0";
      case "0.31.1", "0.31.0" -> "0.23.0";
      case "0.30.0" -> "0.22.0";
      case "0.29.0" -> "0.21.0";
      case "0.28.0" -> "0.20.0";
      case "0.26.2", "0.26.0" -> "0.18.0";
      case "0.25.0" -> "0.17.0";
      case "0.24.0" -> "0.16.0";
      case "0.23.0" -> "0.15.0";
      case "0.22.0" -> "0.14.0";
      default -> clean;
    };
  }

  /**
   * Returns a friendly description of what language version and network ledger this compiler targets.
   */
  public static @NotNull String getVersionDescription(@NotNull String version) {
    String clean = cleanVersion(version);
    return switch (clean) {
      case "0.34.0" -> "Compact language 0.26.0 (Midnight Ledger 9)";
      case "0.31.1", "0.31.0" -> "Compact language 0.23.0 (Midnight Ledger 8)";
      case "0.30.0" -> "Compact language 0.22.0 (Midnight Ledger 7)";
      case "0.29.0" -> "Compact language 0.21.0";
      case "0.28.0" -> "Compact language 0.20.0";
      case "0.26.2", "0.26.0" -> "Compact language 0.18.0 (Legacy Toolchain)";
      case "0.25.0" -> "Compact language 0.17.0 (Legacy Toolchain)";
      case "0.24.0" -> "Compact language 0.16.0 (Legacy Toolchain)";
      case "0.23.0" -> "Compact language 0.15.0 (Legacy Toolchain)";
      case "0.22.0" -> "Compact language 0.14.0 (Legacy Toolchain)";
      default -> "Compact compiler toolchain v" + clean;
    };
  }

  /**
   * Discovers all locally installed Compact compiler versions.
   *
   * @return SequencedMap of cleaned version string to an executable path.
   */
  public static @NotNull SequencedMap<String, String> getInstalledVersions() {
    SequencedMap<String, String> versions = new TreeMap<>(Comparator.reverseOrder());
    File baseDir = getVersionsDirectory();
    if (!baseDir.isDirectory()) {
      return versions;
    }

    File[] children = baseDir.listFiles(File::isDirectory);
    if (children == null) {
      return versions;
    }

    for (File versionDir : children) {
      File exe = findExecutableInVersionDir(versionDir);
      if (exe != null) {
        String path = exe.getAbsolutePath();
        if (SystemInfo.isWindows && (path.startsWith("\\\\wsl") || path.startsWith("//wsl"))) {
          path = CompactToolchainUtil.toWslPath(path);
        }
        versions.put(versionDir.getName(), path);
        cacheExecutableVersion(path, versionDir.getName());
      }
    }
    return versions;
  }

  /**
   * Checks whether a specific compiler version is installed and has a runnable executable.
   */
  public static boolean isVersionInstalled(@NotNull String version) {
    File dir = getVersionDirectory(version);
    if (!dir.isDirectory()) {
      return false;
    }
    File exe = findExecutableInVersionDir(dir);
    return exe != null;
  }

  /**
   * Returns the executable path for a specific installed version, or {@code null} if not found.
   */
  public static @Nullable String getInstalledExecutable(@NotNull String version) {
    File dir = getVersionDirectory(version);
    if (!dir.isDirectory()) {
      return null;
    }
    File exe = findExecutableInVersionDir(dir);
    if (exe == null) {
      return null;
    }
    String path = exe.getAbsolutePath();
    if (SystemInfo.isWindows && (path.startsWith("\\\\wsl") || path.startsWith("//wsl"))) {
      path = CompactToolchainUtil.toWslPath(path);
    }
    cacheExecutableVersion(path, version);
    return path;
  }

  /**
   * Locates the compiler executable within a version directory, checking root, architecture
   * subdirectories (e.g. x86_64-unknown-linux-musl), bin/, and node_modules/.bin/.
   */
  public static @Nullable File findExecutableInVersionDir(@NotNull File versionDir) {
    if (!versionDir.isDirectory()) {
      return null;
    }

    // 1. Direct root
    File exe = findExecutableNamed(versionDir, "compactc");
    if (exe != null) return exe;
    exe = findExecutableNamed(versionDir, "compact");
    if (exe != null) return exe;

    // 2. Architecture-specific subdirectories (e.g. x86_64-unknown-linux-musl, aarch64-unknown-linux-musl)
    File[] children = versionDir.listFiles(File::isDirectory);
    if (children != null) {
      for (File child : children) {
        String name = child.getName().toLowerCase(Locale.ROOT);
        if (name.contains("linux") || name.contains("darwin") || name.equals("bin") || name.equals(".bin")) {
          exe = findExecutableNamed(child, "compactc");
          if (exe != null) return exe;
          exe = findExecutableNamed(child, "compact");
          if (exe != null) return exe;
        }
      }
    }

    // 3. node_modules/.bin/ (if installed via npm/yarn)
    File nodeModulesBin = new File(versionDir, "node_modules" + File.separator + ".bin");
    if (nodeModulesBin.isDirectory()) {
      exe = findExecutableNamed(nodeModulesBin, "compactc");
      if (exe != null) return exe;
      exe = findExecutableNamed(nodeModulesBin, "compact");
      if (exe != null) return exe;
    }

    // 4. bin/ subfolder
    File binDir = new File(versionDir, "bin");
    if (binDir.isDirectory()) {
      exe = findExecutableNamed(binDir, "compactc");
      if (exe != null) return exe;
      exe = findExecutableNamed(binDir, "compact");
      return exe;
    }

    return null;
  }

  private static @Nullable File findExecutableNamed(@NotNull File dir, @NotNull String baseName) {
    if (SystemInfo.isWindows && !dir.getAbsolutePath().startsWith("\\\\wsl")) {
      File cmd = new File(dir, baseName + ".cmd");
      if (cmd.isFile()) return cmd;
      File exe = new File(dir, baseName + ".exe");
      if (exe.isFile()) return exe;
      File bat = new File(dir, baseName + ".bat");
      if (bat.isFile()) return bat;
    }
    File raw = new File(dir, baseName);
    if (raw.isFile()) return raw;
    return null;
  }

  @SuppressWarnings("unused")
  public static boolean installVersion(@NotNull String version, @Nullable ProgressIndicator indicator) {
    return installVersion(version, null, indicator);
  }

  /**
   * Installs a specific compiler version into {@code ~/.compact/versions/<version>/}.
   */
  public static boolean installVersion(
      @NotNull String version,
      @Nullable String projectBasePath,
      @Nullable ProgressIndicator indicator
  ) {
    String cleanVer = cleanVersion(version);
    File targetDir = getVersionDirectory(cleanVer);

    if (targetDir.exists() && isVersionInstalled(cleanVer)) {
      LOG.info("Version " + cleanVer + " is already installed at: " + targetDir.getAbsolutePath());
      return true;
    }

    if (!targetDir.exists() && !targetDir.mkdirs()) {
      LOG.warn("Could not create version directory: " + targetDir.getAbsolutePath());
      return false;
    }

    if (indicator != null) {
      indicator.setText("Downloading Compact compiler v" + cleanVer + "...");
    }

    // 1. Try downloading the official GitHub release binary
    boolean downloaded = downloadOfficialRelease(cleanVer, targetDir, indicator);
    if (downloaded) {
      recordInstalledExecutable(targetDir, cleanVer);
      return true;
    }

    // 2. Try fetching via yarn fetch-compactc
    if (indicator != null) {
      indicator.setText("Fetching Compact compiler via yarn fetch-compactc...");
    }
    boolean fetched = fetchViaYarnCli(cleanVer, targetDir, projectBasePath, indicator);
    if (fetched) {
      recordInstalledExecutable(targetDir, cleanVer);
      return true;
    }

    // Clean up empty directory if installation failed
    File[] files = targetDir.listFiles();
    if (files == null || files.length == 0) {
      FileUtil.delete(targetDir);
    }
    return false;
  }

  private static boolean fetchViaYarnCli(
      @NotNull String cleanVer,
      @NotNull File targetDir,
      @Nullable String projectBasePath,
      @Nullable ProgressIndicator indicator
  ) {
    try {
      File workDir = (projectBasePath != null && new File(projectBasePath).isDirectory()) ? new File(projectBasePath) : targetDir;
      GeneralCommandLine cmd;
      if (SystemInfo.isWindows && (targetDir.getAbsolutePath().startsWith("\\\\wsl") || targetDir.getAbsolutePath().startsWith("//wsl"))) {
        String distro = getWslDistribution();
        String wslPath = CompactToolchainUtil.toWslPath(targetDir.getAbsolutePath());
        cmd = new GeneralCommandLine("wsl.exe");
        if (distro != null) {
          cmd.addParameters("-d", distro);
        }
        cmd.addParameters("-e", "bash", "-c", "cd '" + wslPath + "' && yarn fetch-compactc --version=" + cleanVer);
      } else {
        cmd = new GeneralCommandLine("yarn", "fetch-compactc", "--version=" + cleanVer);
        cmd.setWorkDirectory(workDir);
      }

      CapturingProcessHandler handler = new CapturingProcessHandler(cmd);
      ProcessOutput output = indicator != null
          ? handler.runProcessWithProgressIndicator(indicator, 60_000)
          : handler.runProcess(60_000);

      if (output.getExitCode() == 0) {
        LOG.info("Successfully fetched via yarn fetch-compactc: " + output.getStdout());
        return true;
      } else {
        LOG.debug("yarn fetch-compactc exited with code " + output.getExitCode());
      }
    } catch (Exception e) {
      LOG.debug("yarn fetch-compactc not available or failed: " + e.getMessage());
    }
    return false;
  }

  private static boolean downloadOfficialRelease(
      @NotNull String version,
      @NotNull File targetDir,
      @Nullable ProgressIndicator indicator
  ) {
    String assetName = resolveReleaseAssetName(version);
    List<String> candidateUrls = List.of(
        "https://github.com/midnightntwrk/compact/releases/download/compactc-v" + version + "/" + assetName,
        "https://github.com/midnightntwrk/compact/releases/download/v" + version + "/" + assetName,
        "https://github.com/midnightntwrk/compact/releases/download/compact-v" + version + "/" + assetName
    );

    try (HttpClient client = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.ALWAYS)
        .connectTimeout(Duration.ofSeconds(15))
        .build()) {

      for (String url : candidateUrls) {
        LOG.info("Attempting to download Compact compiler from: " + url);
        try {
          HttpRequest request = HttpRequest.newBuilder()
              .uri(URI.create(url))
              .timeout(Duration.ofMinutes(3))
              .GET()
              .build();

          HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
          if (response.statusCode() == 200) {
            File zipFile = new File(targetDir, assetName);
            try (InputStream in = response.body();
                 OutputStream out = new BufferedOutputStream(new FileOutputStream(zipFile))) {
              byte[] buffer = new byte[8192];
              int read;
              while ((read = in.read(buffer)) != -1) {
                if (indicator != null && indicator.isCanceled()) {
                  FileUtil.delete(zipFile);
                  return false;
                }
                out.write(buffer, 0, read);
              }
            }

            if (indicator != null) {
              indicator.setText("Extracting compiler archive...");
            }

            extractZip(zipFile, targetDir);
            FileUtil.delete(zipFile);

            setExecutablePermissions(targetDir);

            File exe = findExecutableInVersionDir(targetDir);
            if (exe != null) {
              LOG.info("Successfully installed Compact compiler v" + version + " to: " + exe.getAbsolutePath());
              return true;
            } else {
              LOG.warn("Archive extracted but compiler binary not found in: " + targetDir.getAbsolutePath());
            }
          } else {
            LOG.debug("Download returned HTTP status " + response.statusCode() + " for: " + url);
          }
        } catch (Exception e) {
          LOG.debug("Failed download attempt from " + url + ": " + e.getMessage());
        }
      }
    } catch (Exception e) {
      LOG.warn("Failed to download or extract Compact compiler v" + version + ": " + e.getMessage(), e);
    }
    return false;
  }

  private static void extractZip(@NotNull File zipFile, @NotNull File destDir) throws IOException {
    try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile)))) {
      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        File file = new File(destDir, entry.getName());
        if (entry.isDirectory()) {
          if (!file.isDirectory() && !file.mkdirs()) {
            LOG.debug("Could not create directory from zip entry: " + file.getAbsolutePath());
          }
        } else {
          File parent = file.getParentFile();
          if (parent != null && !parent.exists() && !parent.mkdirs()) {
            LOG.debug("Could not create parent directory for zip entry: " + parent.getAbsolutePath());
          }
          try (OutputStream out = new BufferedOutputStream(new FileOutputStream(file))) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = zis.read(buffer)) != -1) {
              out.write(buffer, 0, len);
            }
          }
          // Preserve executable flag
          if (!file.setExecutable(true, false)) {
            LOG.debug("Could not set executable flag on: " + file.getAbsolutePath());
          }
        }
        zis.closeEntry();
      }
    }
  }

  private static void setExecutablePermissions(@NotNull File dir) {
    File[] files = dir.listFiles();
    if (files != null) {
      for (File f : files) {
        if (f.isDirectory()) {
          setExecutablePermissions(f);
        } else {
          if (!f.setExecutable(true, false)) {
            LOG.debug("Could not set executable flag on: " + f.getAbsolutePath());
          }
        }
      }
    }
  }

  public static @NotNull String getReleaseAssetName(@NotNull String version) {
    return resolveReleaseAssetName(version);
  }

  public static @NotNull String resolveReleaseAssetName(@NotNull String version) {
    String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
    String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
    boolean isArm = arch.contains("aarch64") || arch.contains("arm");

    if (os.contains("mac") || os.contains("darwin")) {
      return isArm
          ? "compactc_v" + version + "_aarch64-darwin.zip"
          : "compactc_v" + version + "_x86_64-darwin.zip";
    }
    // On Linux or Windows (WSL), the Linux musl binary is used
    return isArm
        ? "compactc_v" + version + "_aarch64-unknown-linux-musl.zip"
        : "compactc_v" + version + "_x86_64-unknown-linux-musl.zip";
  }

  /**
   * Uninstalls a specific compiler version by deleting its directory.
   */
  public static void uninstallVersion(@NotNull String version) {
    File targetDir = getVersionDirectory(version);
    if (targetDir.exists()) {
      FileUtil.delete(targetDir);
    }
    EXECUTABLE_VERSION_CACHE.entrySet().removeIf(e -> version.equals(e.getValue()));
  }

  /**
   * Detects the version string from an executable by executing it with {@code --version}.
   *
   * <p>Thread-safe and EDT-safe: Never runs external OS processes synchronously on the Event Dispatch Thread (EDT)
   * or while holding a Read Action. If not cached, it triggers background detection and returns null or the path-extracted version.</p>
   */
  public static @Nullable String detectVersionFromExecutable(@NotNull String executablePath) {
    if (executablePath.isBlank()) {
      return null;
    }

    // 1. Fast cache check
    String cached = EXECUTABLE_VERSION_CACHE.get(executablePath);
    if (cached != null) {
      return cached;
    }

    // 2. Fast path extraction if managed version directory
    String extracted = CompactSemVerUtil.extractVersion(executablePath);
    if (extracted != null && (executablePath.contains("/.compact/versions/") || executablePath.contains("\\.compact\\versions\\") || executablePath.contains("compactc/"))) {
      EXECUTABLE_VERSION_CACHE.put(executablePath, extracted);
      return extracted;
    }

    // 3. Threading check: strictly disallow synchronous execution on EDT or inside Read Action
    Application app = ApplicationManager.getApplication();
    if (app != null && (app.isDispatchThread() || app.isReadAccessAllowed())) {
      triggerAsyncVersionDetection(executablePath);
      return EXECUTABLE_VERSION_CACHE.get(executablePath);
    }

    // 4. Background execution (safe for a synchronous process)
    String detected = runVersionDetectionProcess(executablePath);
    if (detected != null) {
      EXECUTABLE_VERSION_CACHE.put(executablePath, detected);
    }
    return detected;
  }

  public static void triggerAsyncVersionDetection(@NotNull String executablePath) {
    if (EXECUTABLE_VERSION_CACHE.containsKey(executablePath) || !PENDING_DETECTIONS.add(executablePath)) {
      return;
    }
    Application app = ApplicationManager.getApplication();
    if (app == null) {
      return;
    }
    app.executeOnPooledThread(() -> {
      try {
        String detected = runVersionDetectionProcess(executablePath);
        if (detected != null) {
          EXECUTABLE_VERSION_CACHE.put(executablePath, detected);
          // Restart daemon across all open projects to refresh inspections with the discovered compiler version
          for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            CompactProblemUtil.clearProblemsAndRestart(project, null);
          }
        }
      } finally {
        PENDING_DETECTIONS.remove(executablePath);
      }
    });
  }

  private static @Nullable String runVersionDetectionProcess(@NotNull String executablePath) {
    try {
      GeneralCommandLine cmd;
      if (SystemInfo.isWindows && (executablePath.startsWith("/home/") || executablePath.startsWith("/usr/") || executablePath.startsWith("/root/"))) {
        String distro = getWslDistribution();
        List<String> args = new ArrayList<>();
        if (distro != null) {
          args.addAll(List.of("-d", distro));
        }
        args.addAll(List.of("-e", executablePath, "--version"));
        cmd = new GeneralCommandLine("wsl.exe");
        cmd.addParameters(args);
      } else {
        cmd = new GeneralCommandLine(executablePath, "--version");
      }
      CapturingProcessHandler handler = new CapturingProcessHandler(cmd);
      ProcessOutput output = handler.runProcess(5_000);
      if (output.getExitCode() == 0) {
        String stdout = output.getStdout();
        Matcher m = VERSION_PATTERN.matcher(stdout);
        if (m.find()) {
          return m.group(1);
        }
      }
    } catch (Exception e) {
      LOG.debug("Could not detect version from executable: " + executablePath, e);
    }
    return null;
  }

  // --- WSL Environment Discovery and Legacy Migration ---

  public static @Nullable String getWslDistribution() {
    if (!SystemInfo.isWindows) {
      return null;
    }
    List<String> distros = CompactToolchainUtil.getInstalledWslDistributions();
    if (!distros.isEmpty()) {
      return distros.getFirst();
    }
    return null;
  }

  public static @Nullable String getWslUser(@Nullable String distro) {
    if (!SystemInfo.isWindows || distro == null) {
      return null;
    }
    File homeDir = new File("\\\\wsl.localhost\\" + distro + "\\home");
    if (!homeDir.isDirectory()) {
      homeDir = new File("\\\\wsl$\\" + distro + "\\home");
    }
    if (homeDir.isDirectory()) {
      File[] users = homeDir.listFiles(File::isDirectory);
      if (users != null) {
        for (File user : users) {
          if (!"lost+found".equalsIgnoreCase(user.getName())) {
            return user.getName();
          }
        }
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

  /**
   * Checks if legacy compiler versions were saved to Windows host disk (e.g., C:\Users\<user>\.compact\versions)
   * and automatically migrates them into WSL.
   */
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
        if (!targetVerDir.exists() || targetVerDir.listFiles() == null || Objects.requireNonNull(targetVerDir.listFiles()).length == 0) {
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

  /**
   * Ensures executable permissions (+x) on compiler binaries in WSL.
   */
  public static void chmodWslDirectory() {
    if (!SystemInfo.isWindows) return;
    String distro = getWslDistribution();
    String user = getWslUser(distro);
    if (distro != null && user != null) {
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
  public static void ensureAndSwitchVersion(@NotNull Project project, @NotNull String toolchainVer, @Nullable VirtualFile vFile) {
    if (isVersionInstalled(toolchainVer)) {
      switchAndApplyVersion(project, toolchainVer, vFile);
    } else {
      ProgressManager.getInstance().run(new Task.Backgroundable(project, "Downloading Compact Compiler v" + toolchainVer, true) {
        @Override
        public void run(@NotNull ProgressIndicator indicator) {
          boolean success = installVersion(toolchainVer, project.getBasePath(), indicator);
          if (success) {
            switchAndApplyVersion(project, toolchainVer, vFile);
          }
        }
      });
    }
  }

  public static void switchAndApplyVersion(@NotNull Project project, @NotNull String toolchainVer, @Nullable VirtualFile vFile) {
    MidnightProjectSettings.getInstance(project).selectedCompilerVersion = toolchainVer;
    String installedExe = CompactVersionManager.getInstalledExecutable(toolchainVer);
    if (installedExe != null) {
      MidnightSettingsState state = MidnightSettingsState.getInstance();
      if (state != null) {
        state.compilerPath = installedExe;
      }
    }
    CompactProblemUtil.clearProblemsAndRestart(project, vFile);
  }

  private static void recordInstalledExecutable(@NotNull File targetDir, @NotNull String cleanVer) {
    chmodWslDirectory();
    File exe = findExecutableInVersionDir(targetDir);
    if (exe != null) {
      cacheExecutableVersion(exe.getAbsolutePath(), cleanVer);
      if (SystemInfo.isWindows && exe.getAbsolutePath().startsWith("\\\\wsl")) {
        cacheExecutableVersion(CompactToolchainUtil.toWslPath(exe.getAbsolutePath()), cleanVer);
      }
    }
  }
}
