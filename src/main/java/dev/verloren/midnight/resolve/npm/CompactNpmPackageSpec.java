package dev.verloren.midnight.resolve.npm;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Parsed npm package import specification (e.g. package name and optional subpath).
 */
public record CompactNpmPackageSpec(@NotNull String packageName, @NotNull String subpath) {

  public static @Nullable CompactNpmPackageSpec parse(@Nullable String importPath) {
    if (importPath == null || importPath.isBlank()) {
      return null;
    }
    String cleanPath = importPath.trim();
    if (cleanPath.startsWith("./") || cleanPath.startsWith("../") || cleanPath.startsWith("/") || cleanPath.startsWith("\\")) {
      return null;
    }
    if (cleanPath.endsWith(".compact")) {
      return null;
    }

    if (cleanPath.startsWith("@")) {
      // Scoped package: @scope/name or @scope/name/subpath
      int firstSlash = cleanPath.indexOf('/');
      if (firstSlash == -1) {
        return null;
      }
      int secondSlash = cleanPath.indexOf('/', firstSlash + 1);
      if (secondSlash == -1) {
        return new CompactNpmPackageSpec(cleanPath, "");
      } else {
        String pkgName = cleanPath.substring(0, secondSlash);
        String subpath = cleanPath.substring(secondSlash + 1);
        return new CompactNpmPackageSpec(pkgName, subpath);
      }
    } else {
      // Regular package: pkg or pkg/subpath
      int firstSlash = cleanPath.indexOf('/');
      if (firstSlash == -1) {
        return new CompactNpmPackageSpec(cleanPath, "");
      } else {
        String pkgName = cleanPath.substring(0, firstSlash);
        String subpath = cleanPath.substring(firstSlash + 1);
        return new CompactNpmPackageSpec(pkgName, subpath);
      }
    }
  }
}
