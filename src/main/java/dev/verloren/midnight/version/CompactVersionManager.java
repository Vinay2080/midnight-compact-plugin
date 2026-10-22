package dev.verloren.midnight.version;

import com.intellij.codeInsight.intention.preview.IntentionPreviewUtils;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import dev.verloren.midnight.annotator.CompactProblemUtil;
import dev.verloren.midnight.run.CompactToolchainUtil;
import dev.verloren.midnight.settings.MidnightProjectSettings;
import dev.verloren.midnight.settings.MidnightSettingsState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Coordinates isolated Compact compiler versions installed under {@code ~/.compact/versions/<version>/}.
 *
 * <p>Supports official Midnight toolchains from GitHub Releases ({@code midnightntwrk/compact})
 * and official yarn CLI fetching ({@code yarn fetch-compactc}).</p>
 *
 * <p>Cross-platform support: Compact compiler binaries are Linux/macOS only. On Windows,
 * the toolchain runs within WSL (Windows Subsystem for Linux), storing versions under
 * WSL's {@code ~/.compact/versions/<version>/}. Any legacy Windows versions are automatically
 * migrated into WSL storage via {@link CompactWslStorageHelper}.</p>
 */
public final class CompactVersionManager {
  private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+\\.\\d+\\.\\d+)");
  private static volatile SequencedMap<String, String> TEST_INSTALLED_VERSIONS = null;

  private record ReleaseMapping(@NotNull String toolchain, @NotNull String language, @Nullable String ledger, boolean isLegacy) {}

  private static final List<ReleaseMapping> RELEASES = List.of(
      new ReleaseMapping("0.35.0", "0.27.0", "Midnight Ledger 9", false),
      new ReleaseMapping("0.34.0", "0.26.0", "Midnight Ledger 9", false),
      new ReleaseMapping("0.31.1", "0.23.0", "Midnight Ledger 8", false),
      new ReleaseMapping("0.31.0", "0.23.0", "Midnight Ledger 8", false),
      new ReleaseMapping("0.30.0", "0.22.0", "Midnight Ledger 7", false),
      new ReleaseMapping("0.29.0", "0.21.0", null, false),
      new ReleaseMapping("0.28.0", "0.20.0", null, false),
      new ReleaseMapping("0.26.0", "0.18.0", null, true),
      new ReleaseMapping("0.26.2", "0.18.0", null, true),
      new ReleaseMapping("0.25.0", "0.17.0", null, true),
      new ReleaseMapping("0.24.0", "0.16.0", null, true),
      new ReleaseMapping("0.23.0", "0.15.0", null, true),
      new ReleaseMapping("0.22.0", "0.14.0", null, true)
  );

  /**
   * Officially supported Midnight toolchain releases.
   */
  public static final List<String> KNOWN_VERSIONS = List.of(
      "0.35.0", "0.34.0", "0.31.1", "0.31.0", "0.30.0", "0.29.0",
      "0.28.0", "0.26.0", "0.25.0", "0.24.0", "0.23.0", "0.22.0"
  );

  private CompactVersionManager() {
  }

  @TestOnly
  public static void setInstalledVersionsForTesting(@Nullable SequencedMap<String, String> versions) {
    TEST_INSTALLED_VERSIONS = versions;
  }

  public static void cacheExecutableVersion(@NotNull String executablePath, @NotNull String version) {
    CompactVersionDetector.cacheExecutableVersion(executablePath, version);
  }

  public static @NotNull File getVersionsDirectory() {
    if (SystemInfo.isWindows) {
      File wslDir = CompactWslStorageHelper.getWslVersionsDirectory();
      if (wslDir != null) {
        CompactWslStorageHelper.migrateWindowsVersionsToWslIfNeeded(wslDir);
        return wslDir;
      }
    }
    String userHome = System.getProperty("user.home", ".");
    return new File(new File(userHome, ".compact"), "versions");
  }

  public static @NotNull File getVersionDirectory(@NotNull String version) {
    return new File(getVersionsDirectory(), cleanVersion(version));
  }

  public static @NotNull String cleanVersion(@NotNull String version) {
    String trimmed = version.trim();
    CompactSemVerUtil.SemVer semVer = CompactSemVerUtil.parse(trimmed);
    if (semVer != null) {
      return semVer.major() + "." + semVer.minor() + "." + semVer.patch();
    }
    Matcher m = VERSION_PATTERN.matcher(trimmed);
    if (m.find()) {
      return m.group(1);
    }
    return trimmed.replaceAll("^v", "");
  }

  public static @NotNull String resolveToolchainVersionForLanguage(@NotNull String languageVersion) {
    String clean = cleanVersion(languageVersion);
    for (ReleaseMapping r : RELEASES) {
      if (r.language.equals(clean)) {
        return r.toolchain;
      }
    }
    return clean;
  }

  public static @NotNull String getLanguageVersionForToolchain(@NotNull String toolchainVersion) {
    String clean = cleanVersion(toolchainVersion);
    for (ReleaseMapping r : RELEASES) {
      if (r.toolchain.equals(clean)) {
        return r.language;
      }
    }
    return clean;
  }

  public static @Nullable String getLedgerVersionForToolchain(@NotNull String toolchainVersion) {
    String clean = cleanVersion(toolchainVersion);
    for (ReleaseMapping r : RELEASES) {
      if (r.toolchain.equals(clean)) {
        return r.ledger;
      }
    }
    return null;
  }

  public static @NotNull String getVersionDescription(@NotNull String version) {
    String clean = cleanVersion(version);
    for (ReleaseMapping r : RELEASES) {
      if (r.toolchain.equals(clean)) {
        String base = "Language: " + r.language;
        if (r.ledger != null) {
          return base + " | " + r.ledger;
        }
        return r.isLegacy ? base + " | Legacy Toolchain" : base;
      }
    }
    return "Language: " + clean;
  }

  public static @NotNull SequencedMap<String, String> getInstalledVersions() {
    SequencedMap<String, String> versions = new TreeMap<>(CompactSemVerUtil.DESCENDING_COMPARATOR);
    if (TEST_INSTALLED_VERSIONS != null) {
      versions.putAll(TEST_INSTALLED_VERSIONS);
      return versions;
    }
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

  public static boolean isVersionInstalled(@NotNull String version) {
    if (TEST_INSTALLED_VERSIONS != null) {
      return TEST_INSTALLED_VERSIONS.containsKey(cleanVersion(version));
    }
    File dir = getVersionDirectory(version);
    if (!dir.isDirectory()) {
      return false;
    }
    return findExecutableInVersionDir(dir) != null;
  }

  public static @Nullable String getInstalledExecutable(@NotNull String version) {
    if (TEST_INSTALLED_VERSIONS != null) {
      return TEST_INSTALLED_VERSIONS.get(cleanVersion(version));
    }
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

  public static @Nullable File findExecutableInVersionDir(@NotNull File versionDir) {
    if (!versionDir.isDirectory()) {
      return null;
    }

    File exe = findExecutableNamed(versionDir, "compactc");
    if (exe != null) return exe;
    exe = findExecutableNamed(versionDir, "compact");
    if (exe != null) return exe;

    return findExecutableInSubdirectories(versionDir);
  }

  private static @Nullable File findExecutableInSubdirectories(@NotNull File versionDir) {
    File[] children = versionDir.listFiles(File::isDirectory);
    if (children != null) {
      for (File child : children) {
        String name = child.getName().toLowerCase(Locale.ROOT);
        if (name.contains("linux") || name.contains("darwin") || name.equals("bin") || name.equals(".bin")) {
          File exe = findExecutableNamed(child, "compactc");
          if (exe != null) return exe;
          exe = findExecutableNamed(child, "compact");
          if (exe != null) return exe;
        }
      }
    }

    File nodeModulesBin = new File(versionDir, "node_modules" + File.separator + ".bin");
    if (nodeModulesBin.isDirectory()) {
      File exe = findExecutableNamed(nodeModulesBin, "compactc");
      if (exe != null) return exe;
      return findExecutableNamed(nodeModulesBin, "compact");
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
    return raw.isFile() ? raw : null;
  }

  @SuppressWarnings("unused")
  public static boolean installVersion(@NotNull String version, @Nullable ProgressIndicator indicator) {
    return CompactVersionInstaller.installVersion(version, null, indicator);
  }

  public static boolean installVersion(
      @NotNull String version,
      @Nullable String projectBasePath,
      @Nullable ProgressIndicator indicator
  ) {
    return CompactVersionInstaller.installVersion(version, projectBasePath, indicator);
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
    return isArm
        ? "compactc_v" + version + "_aarch64-unknown-linux-musl.zip"
        : "compactc_v" + version + "_x86_64-unknown-linux-musl.zip";
  }

  public static @NotNull String getReleaseDownloadUrl(@NotNull String version) {
    String clean = cleanVersion(version);
    return "https://github.com/midnightntwrk/compact/releases/download/compactc-v" + clean + "/" + resolveReleaseAssetName(clean);
  }

  public static void uninstallVersion(@NotNull String version) {
    File targetDir = getVersionDirectory(version);
    if (targetDir.exists()) {
      FileUtil.delete(targetDir);
    }
    CompactVersionDetector.removeVersionFromCache(version);
  }

  public static @Nullable String detectVersionFromExecutable(@NotNull String executablePath) {
    return CompactVersionDetector.detectVersionFromExecutable(executablePath);
  }

  public static void triggerAsyncVersionDetection(@NotNull String executablePath) {
    CompactVersionDetector.triggerAsyncVersionDetection(executablePath);
  }

  public static @Nullable String getWslDistribution() {
    return CompactWslStorageHelper.getWslDistribution();
  }

  public static @Nullable String getWslUser(@Nullable String distro) {
    return CompactWslStorageHelper.getWslUser(distro);
  }

  public static @Nullable File getWslVersionsDirectory() {
    return CompactWslStorageHelper.getWslVersionsDirectory();
  }

  public static void migrateWindowsVersionsToWslIfNeeded(@NotNull File wslVersionsDir) {
    CompactWslStorageHelper.migrateWindowsVersionsToWslIfNeeded(wslVersionsDir);
  }

  public static void chmodWslDirectory() {
    CompactWslStorageHelper.chmodWslDirectory();
  }

  public static void ensureAndSwitchVersion(@NotNull Project project, @NotNull String toolchainVer, @Nullable VirtualFile vFile) {
    if (IntentionPreviewUtils.isIntentionPreviewActive()) {
      return;
    }
    String cleanToolchain = cleanVersion(toolchainVer);
    if (isVersionInstalled(cleanToolchain)) {
      switchAndApplyVersion(project, cleanToolchain, vFile);
    } else {
      ProgressManager.getInstance().run(new Task.Backgroundable(project, "Downloading Compact Compiler v" + cleanToolchain, true) {
        @Override
        public void run(@NotNull ProgressIndicator indicator) {
          boolean success = installVersion(cleanToolchain, project.getBasePath(), indicator);
          if (success) {
            switchAndApplyVersion(project, cleanToolchain, vFile);
          }
        }
      });
    }
  }

  public static void switchAndApplyVersion(@NotNull Project project, @NotNull String toolchainVer, @Nullable VirtualFile vFile) {
    if (IntentionPreviewUtils.isIntentionPreviewActive()) {
      return;
    }
    String cleanToolchain = cleanVersion(toolchainVer);
    MidnightProjectSettings.getInstance(project).selectedCompilerVersion = cleanToolchain;
    String installedExe = CompactVersionManager.getInstalledExecutable(cleanToolchain);
    if (installedExe != null) {
      MidnightSettingsState state = MidnightSettingsState.getInstance();
      if (state != null) {
        state.compilerPath = installedExe;
      }
    }
    CompactProblemUtil.clearProblemsAndRestart(project, vFile);
  }
}
