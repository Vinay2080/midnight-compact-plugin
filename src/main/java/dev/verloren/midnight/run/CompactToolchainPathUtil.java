package dev.verloren.midnight.run;

import com.intellij.openapi.project.Project;
import dev.verloren.midnight.settings.MidnightSettingsState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utility class providing cross-platform path conversions and deterministic output directory
 * calculations for the Compact smart contract compiler toolchain.
 *
 * <h3>Architectural Role</h3>
 * <p>Handles translations between Windows host paths, WSL Linux paths, and UNC network shares.
 * Provides deterministic artifact directory naming logic that maps contract source paths (e.g.
 * {@code "src/contracts/counter.compact"}) into clean output paths (e.g. {@code "gen/counter"})
 * while stripping boilerplate source folders (such as {@code "src"}, {@code "contracts"}, {@code "circuits"}).</p>
 *
 * <h3>Path Conversion Pipeline</h3>
 * <pre>
 *   Windows Host Path: "C:\Projects\midnight\contracts\token.compact"
 *                               │
 *                               ▼
 *   [CompactToolchainPathUtil.toWslPath]
 *                               │
 *                               ▼
 *   WSL Linux Path:    "/mnt/c/Projects/midnight/contracts/token.compact"
 * </pre>
 *
 * <h3>Output Directory Derivation Pipeline</h3>
 * <pre>
 *   Contract Source Path:  "C:/MyProject/src/contracts/escrow/vault.compact"
 *                                       │
 *                                       ▼
 *   [stripProjectPrefix]         ──► "src/contracts/escrow/vault.compact"
 *   [stripLeadingDriveAndSlashes]──► (if standalone)
 *   [stripExtension]             ──► "src/contracts/escrow/vault"
 *   [stripSourceRoots]           ──► "escrow/vault" (strips 'src/', 'contracts/')
 *                                       │
 *                                       ▼
 *   Resulting Artifact Directory: "gen/escrow/vault"
 * </pre>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Pure Function:</b> Stateless and thread-safe. All methods perform pure string manipulations
 *       and can be called concurrently without synchronization.</li>
 * </ul>
 *
 * @see CompactToolchainUtil
 * @see CompactToolchainDiscovery
 */
public final class CompactToolchainPathUtil {

  /**
   * Recognized standard source root directory names stripped when deriving clean output directories.
   */
  private static final String[] SOURCE_ROOTS = {"circuits", "contracts", "src", "compact", "source", "contract"};

  /**
   * Private constructor to prevent instantiation of static utility class.
   */
  private CompactToolchainPathUtil() {
  }

  /**
   * Converts a Windows file path (e.g. {@code "C:\dir\file.compact"}) or WSL UNC network path
   * (e.g. {@code "\\wsl$\Ubuntu\home\..."}) to its Linux WSL path equivalent (e.g. {@code "/mnt/c/dir/file.compact"}).
   *
   * @param path Windows or UNC path string
   * @return normalized Linux path string for use inside WSL
   */
  public static @NotNull String toWslPath(@NotNull String path) {
    if (path.isEmpty() || path.startsWith("-")) {
      return path;
    }
    String normalized = path.replace('\\', '/');

    // 1. Handle WSL UNC paths (//wsl$/... or //wsl.localhost/...)
    if (normalized.startsWith("//wsl$/") || normalized.startsWith("//wsl.localhost/")) {
      int prefixLen = normalized.startsWith("//wsl$/") ? 7 : 16;
      int nextSlash = normalized.indexOf('/', prefixLen);
      if (nextSlash != -1) {
        return normalized.substring(nextSlash);
      }
    }

    // 2. Handle standard Windows drive letters (C:\... -> /mnt/c/...)
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
   * Converts a Linux WSL mount path (e.g. {@code "/mnt/c/dir/file.compact"}) into its Windows host
   * file path equivalent (e.g. {@code "C:\dir\file.compact"}).
   *
   * @param path Linux WSL path string
   * @return Windows host file path
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
   * Deterministically calculates a unique output directory for a given Compact smart contract file,
   * falling back to global settings or {@code "gen"}.
   *
   * @param project current project context (used to strip project base directory prefix)
   * @param compactFilePath absolute or relative path to the {@code .compact} contract file
   * @return relative output path (e.g. {@code "gen/counter"})
   */
  public static @NotNull String deriveOutputDirectory(@Nullable Project project, @NotNull String compactFilePath) {
    return deriveOutputDirectory(project, compactFilePath, null);
  }

  /**
   * Deterministically calculates a unique output directory for a given Compact smart contract file
   * relative to the specified {@code baseOutputDir}.
   *
   * @param project current project context
   * @param compactFilePath absolute or relative path to the {@code .compact} contract file
   * @param baseOutputDir explicit base output folder (e.g. {@code "build/gen"}), or {@code null} to use defaults
   * @return relative output directory path
   */
  public static @NotNull String deriveOutputDirectory(
      @Nullable Project project,
      @NotNull String compactFilePath,
      @Nullable String baseOutputDir
  ) {
    // 1. Resolve base directory from arguments, settings, or default "gen"
    String base = getBase(baseOutputDir);
    String path = compactFilePath.trim().replace('\\', '/');
    if (path.isEmpty()) {
      return base;
    }

    // 2. Normalize and strip project path prefix if present
    path = stripProjectPrefix(project, path);

    // 3. Strip leading drive letters (C:) and slashes
    path = stripLeadingDriveAndSlashes(path);

    // 4. Strip contract file extension (.compact / .compactc)
    path = stripExtension(path);

    // 5. Strip common root folders (e.g. src/, contracts/, circuits/)
    path = stripSourceRoots(path);

    // 6. Clean residual boundary slashes
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
    boolean matches = path.regionMatches(true, 0, prefixWithSlash, 0, prefixWithSlash.length());
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
        boolean matches = result.regionMatches(true, 0, rootSlash, 0, rootSlash.length());
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
