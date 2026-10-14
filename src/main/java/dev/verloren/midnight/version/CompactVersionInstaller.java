package dev.verloren.midnight.version;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.CapturingProcessHandler;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import dev.verloren.midnight.run.CompactToolchainUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Handles network downloads, zip extraction, and CLI fetching of Compact compiler releases.
 */
public final class CompactVersionInstaller {
  private static final Logger LOG = Logger.getInstance(CompactVersionInstaller.class);

  private CompactVersionInstaller() {
  }

  public static boolean installVersion(
      @NotNull String version,
      @Nullable String projectBasePath,
      @Nullable ProgressIndicator indicator
  ) {
    String cleanVer = CompactVersionManager.cleanVersion(version);
    File targetDir = CompactVersionManager.getVersionDirectory(cleanVer);

    if (targetDir.exists() && CompactVersionManager.isVersionInstalled(cleanVer)) {
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
    if (downloadOfficialRelease(cleanVer, targetDir, indicator)) {
      recordInstalledExecutable(targetDir, cleanVer);
      return true;
    }

    // 2. Try fetching via yarn fetch-compactc
    if (indicator != null) {
      indicator.setText("Fetching Compact compiler via yarn fetch-compactc...");
    }
    if (fetchViaYarnCli(cleanVer, targetDir, projectBasePath, indicator)) {
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

  public static boolean fetchViaYarnCli(
      @NotNull String cleanVer,
      @NotNull File targetDir,
      @Nullable String projectBasePath,
      @Nullable ProgressIndicator indicator
  ) {
    try {
      File workDir = (projectBasePath != null && new File(projectBasePath).isDirectory()) ? new File(projectBasePath) : targetDir;
      GeneralCommandLine cmd;
      if (SystemInfo.isWindows && (targetDir.getAbsolutePath().startsWith("\\\\wsl") || targetDir.getAbsolutePath().startsWith("//wsl"))) {
        String distro = CompactWslStorageHelper.getWslDistribution();
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
      }
      LOG.debug("yarn fetch-compactc exited with code " + output.getExitCode());
    } catch (Exception e) {
      LOG.debug("yarn fetch-compactc not available or failed: " + e.getMessage());
    }
    return false;
  }

  public static boolean downloadOfficialRelease(
      @NotNull String version,
      @NotNull File targetDir,
      @Nullable ProgressIndicator indicator
  ) {
    String assetName = CompactVersionManager.resolveReleaseAssetName(version);
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
        if (tryDownloadCandidate(client, url, assetName, version, targetDir, indicator)) {
          return true;
        }
      }
    } catch (Exception e) {
      LOG.warn("Failed to download or extract Compact compiler v" + version + ": " + e.getMessage(), e);
    }
    return false;
  }

  private static boolean tryDownloadCandidate(
      @NotNull HttpClient client,
      @NotNull String url,
      @NotNull String assetName,
      @NotNull String version,
      @NotNull File targetDir,
      @Nullable ProgressIndicator indicator
  ) {
    LOG.info("Attempting to download Compact compiler from: " + url);
    try {
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(url))
          .timeout(Duration.ofMinutes(3))
          .GET()
          .build();

      HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
      if (response.statusCode() != 200) {
        LOG.debug("Download returned HTTP status " + response.statusCode() + " for: " + url);
        return false;
      }

      File zipFile = new File(targetDir, assetName);
      if (!downloadToFile(response, zipFile, indicator)) {
        return false;
      }

      if (indicator != null) {
        indicator.setText("Extracting compiler archive...");
      }

      extractZip(zipFile, targetDir);
      FileUtil.delete(zipFile);
      setExecutablePermissions(targetDir);

      File exe = CompactVersionManager.findExecutableInVersionDir(targetDir);
      if (exe != null) {
        LOG.info("Successfully installed Compact compiler v" + version + " to: " + exe.getAbsolutePath());
        return true;
      }
      LOG.warn("Archive extracted but compiler binary not found in: " + targetDir.getAbsolutePath());
    } catch (Exception e) {
      LOG.debug("Failed download attempt from " + url + ": " + e.getMessage());
    }
    return false;
  }

  private static boolean downloadToFile(
      @NotNull HttpResponse<InputStream> response,
      @NotNull File zipFile,
      @Nullable ProgressIndicator indicator
  ) throws IOException {
    boolean canceled = false;
    try (InputStream in = response.body();
         OutputStream out = new BufferedOutputStream(new FileOutputStream(zipFile))) {
      byte[] buffer = new byte[8192];
      int read;
      while ((read = in.read(buffer)) != -1) {
        if (indicator != null && indicator.isCanceled()) {
          canceled = true;
          break;
        }
        out.write(buffer, 0, read);
      }
    }
    if (canceled) {
      FileUtil.delete(zipFile);
      return false;
    }
    return true;
  }

  public static void extractZip(@NotNull File zipFile, @NotNull File destDir) throws IOException {
    java.nio.file.Path destDirPath = destDir.toPath().normalize();
    try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile)))) {
      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        File file = new File(destDir, entry.getName());
        java.nio.file.Path targetPath = file.toPath().normalize();
        if (!targetPath.startsWith(destDirPath)) {
          throw new IOException("Zip entry is outside target directory: " + entry.getName());
        }
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
          if (!file.setExecutable(true, false)) {
            LOG.debug("Could not set executable flag on extracted zip entry: " + file.getAbsolutePath());
          }
        }
        zis.closeEntry();
      }
    }
  }

  public static void setExecutablePermissions(@NotNull File dir) {
    File[] files = dir.listFiles();
    if (files != null) {
      for (File f : files) {
        if (f.isDirectory()) {
          setExecutablePermissions(f);
        } else if (!f.setExecutable(true, false)) {
          LOG.debug("Could not set executable flag during recursive directory scan on: " + f.getAbsolutePath());
        }
      }
    }
  }

  private static void recordInstalledExecutable(@NotNull File targetDir, @NotNull String cleanVer) {
    CompactWslStorageHelper.chmodWslDirectory();
    File exe = CompactVersionManager.findExecutableInVersionDir(targetDir);
    if (exe != null) {
      CompactVersionManager.cacheExecutableVersion(exe.getAbsolutePath(), cleanVer);
      if (SystemInfo.isWindows && exe.getAbsolutePath().startsWith("\\\\wsl")) {
        CompactVersionManager.cacheExecutableVersion(CompactToolchainUtil.toWslPath(exe.getAbsolutePath()), cleanVer);
      }
    }
  }
}
