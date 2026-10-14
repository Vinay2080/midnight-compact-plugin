package dev.verloren.midnight.run;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import dev.verloren.midnight.settings.MidnightSettingsState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Handles cross-platform path conversions (Windows host, WSL, and POSIX) and deterministic
 * output directory calculations for Compact smart contract compilation.
 */
public final class CompactToolchainPathUtil {

  private static final String[] SOURCE_ROOTS = {"circuits", "contracts", "src", "compact", "source", "contract"};

  private CompactToolchainPathUtil() {
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
   * Converts a Linux WSL path (e.g. /mnt/c/path) to its Windows file path equivalent.
   */
  public static @NotNull String toWindowsPath(@NotNull String path) {
    if (!path.startsWith("/mnt/")) {
      return path;
    }
    if (path.length() >= 6 && Character.isLetter(path.charAt(5)) && (path.length() == 6 || path.charAt(6) == '/')) {
      char drive = Character.toUpperCase(path.charAt(5));
      String rest = path.substring(6);
      if (rest.isEmpty() || rest.equals("/")) {
        return drive + ":\\";
      }
      return drive + ":" + rest.replace('/', '\\');
    }
    return path;
  }

  /**
   * Deterministically calculates a unique output directory for a given Compact smart contract file.
   */
  public static @NotNull String deriveOutputDirectory(@Nullable Project project, @NotNull String compactFilePath) {
    return deriveOutputDirectory(project, compactFilePath, null);
  }

  /**
   * Deterministically calculates a unique output directory for a given Compact smart contract file.
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

    path = stripProjectPrefix(project, path);
    path = stripLeadingDriveAndSlashes(path);
    path = stripExtension(path);
    path = stripSourceRoots(path);

    while (path.startsWith("/")) {
      path = path.substring(1);
    }
    while (path.endsWith("/")) {
      path = path.substring(0, path.length() - 1);
    }

    return path.isEmpty() ? base : base + "/" + path;
  }

  private static @NotNull String stripProjectPrefix(@Nullable Project project, @NotNull String path) {
    if (project == null || project.getBasePath() == null) {
      return path;
    }
    String projectPath = project.getBasePath().trim().replace('\\', '/');
    while (projectPath.endsWith("/")) {
      projectPath = projectPath.substring(0, projectPath.length() - 1);
    }
    if (projectPath.isEmpty()) {
      return path;
    }

    String prefixWithSlash = projectPath + "/";
    boolean matches = SystemInfo.isFileSystemCaseSensitive
        ? path.startsWith(prefixWithSlash)
        : path.regionMatches(true, 0, prefixWithSlash, 0, prefixWithSlash.length());
    return matches ? path.substring(prefixWithSlash.length()) : path;
  }

  private static @NotNull String stripLeadingDriveAndSlashes(@NotNull String path) {
    String result = path;
    if (result.length() >= 2 && Character.isLetter(result.charAt(0)) && result.charAt(1) == ':') {
      result = result.substring(2);
    }
    while (result.startsWith("/")) {
      result = result.substring(1);
    }
    return result;
  }

  private static @NotNull String stripExtension(@NotNull String path) {
    if (path.endsWith(".compact")) {
      return path.substring(0, path.length() - ".compact".length());
    }
    if (path.endsWith(".compactc")) {
      return path.substring(0, path.length() - ".compactc".length());
    }
    int lastDot = path.lastIndexOf('.');
    int lastSlash = path.lastIndexOf('/');
    if (lastDot > lastSlash && lastDot > 0) {
      return path.substring(0, lastDot);
    }
    return path;
  }

  private static @NotNull String stripSourceRoots(@NotNull String path) {
    String result = path;
    boolean stripped;
    do {
      stripped = false;
      for (String root : SOURCE_ROOTS) {
        String rootSlash = root + "/";
        boolean matches = SystemInfo.isFileSystemCaseSensitive
            ? result.startsWith(rootSlash)
            : result.regionMatches(true, 0, rootSlash, 0, rootSlash.length());
        if (matches && result.length() > root.length() + 1) {
          result = result.substring(root.length() + 1);
          stripped = true;
          break;
        }
      }
    } while (stripped);
    return result;
  }

  private static @NotNull String getBase(@Nullable String baseOutputDir) {
    String base = (baseOutputDir != null && !baseOutputDir.isBlank())
        ? baseOutputDir.trim().replace('\\', '/')
        : null;

    if (base == null) {
      MidnightSettingsState state = MidnightSettingsState.getInstance();
      if (state != null && state.defaultOutputDir != null && !state.defaultOutputDir.isBlank()) {
        base = state.defaultOutputDir.trim().replace('\\', '/');
      } else {
        base = "gen";
      }
    }

    while (base.endsWith("/")) {
      base = base.substring(0, base.length() - 1);
    }
    return base.isEmpty() ? "gen" : base;
  }
}
