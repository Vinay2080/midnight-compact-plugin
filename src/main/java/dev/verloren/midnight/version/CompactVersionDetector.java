package dev.verloren.midnight.version;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.CapturingProcessHandler;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.SystemInfo;
import dev.verloren.midnight.annotator.CompactProblemUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles running executable version detection via CLI processes on background threads with caching.
 */
public final class CompactVersionDetector {
  private static final Logger LOG = Logger.getInstance(CompactVersionDetector.class);
  private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+\\.\\d+\\.\\d+)");

  private static final Map<String, String> EXECUTABLE_VERSION_CACHE = new ConcurrentHashMap<>();
  private static final Set<String> PENDING_DETECTIONS = ConcurrentHashMap.newKeySet();

  private CompactVersionDetector() {
  }

  public static void cacheExecutableVersion(@NotNull String executablePath, @NotNull String version) {
    EXECUTABLE_VERSION_CACHE.put(executablePath, version);
  }

  public static void removeVersionFromCache(@NotNull String version) {
    EXECUTABLE_VERSION_CACHE.entrySet().removeIf(e -> version.equals(e.getValue()));
  }

  /**
   * Detects the version string from an executable by executing it with {@code --version}.
   */
  public static @Nullable String detectVersionFromExecutable(@NotNull String executablePath) {
    if (executablePath.isBlank()) {
      return null;
    }

    String cached = EXECUTABLE_VERSION_CACHE.get(executablePath);
    if (cached != null) {
      return cached;
    }

    String extracted = CompactSemVerUtil.extractVersion(executablePath);
    if (extracted != null && (executablePath.contains("/.compact/versions/") || executablePath.contains("\\.compact\\versions\\") || executablePath.contains("compactc/"))) {
      EXECUTABLE_VERSION_CACHE.put(executablePath, extracted);
      return extracted;
    }

    Application app = ApplicationManager.getApplication();
    if (app != null && (app.isDispatchThread() || app.isReadAccessAllowed())) {
      triggerAsyncVersionDetection(executablePath);
      return EXECUTABLE_VERSION_CACHE.get(executablePath);
    }

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
          for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            if (!project.isDisposed()) {
              CompactProblemUtil.clearProblemsAndRestart(project, null);
            }
          }
        }
      } finally {
        PENDING_DETECTIONS.remove(executablePath);
      }
    });
  }

  public static @Nullable String runVersionDetectionProcess(@NotNull String executablePath) {
    try {
      GeneralCommandLine cmd;
      if (SystemInfo.isWindows && (executablePath.startsWith("/home/") || executablePath.startsWith("/usr/") || executablePath.startsWith("/root/"))) {
        String distro = CompactWslStorageHelper.getWslDistribution();
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
}
