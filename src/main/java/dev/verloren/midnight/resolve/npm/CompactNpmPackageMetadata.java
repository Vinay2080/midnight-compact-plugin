package dev.verloren.midnight.resolve.npm;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import java.util.List;
import java.util.Map;

/**
 * Parses {@code package.json} metadata and resolves entry / declaration files for a npm package.
 */
public class CompactNpmPackageMetadata {
  private final @Nullable String types;
  private final @Nullable String main;
  private final @Nullable String module;
  private final @Nullable String compact;
  private final @Nullable JsonElement exports;

  public CompactNpmPackageMetadata(
      @Nullable String name,
      @Nullable String types,
      @Nullable String main,
      @Nullable String module,
      @Nullable String compact,
      @Nullable JsonElement exports
  ) {
    this.types = types;
    this.main = main;
    this.module = module;
    this.compact = compact;
    this.exports = exports;
  }

  public static @Nullable CompactNpmPackageMetadata from(@NotNull VirtualFile packageDir) {
    VirtualFile packageJsonFile = packageDir.findChild("package.json");
    if (packageJsonFile == null || !packageJsonFile.isValid()) {
      return null;
    }
    try {
      String text = new String(packageJsonFile.contentsToByteArray(), StandardCharsets.UTF_8);
      JsonObject obj = JsonParser.parseString(text).getAsJsonObject();
      String name = getString(obj, "name");
      String types = getString(obj, "types");
      if (types == null) {
        types = getString(obj, "typings");
      }
      String main = getString(obj, "main");
      String module = getString(obj, "module");
      String compact = getString(obj, "compact");
      JsonElement exports = obj.get("exports");
      return new CompactNpmPackageMetadata(name, types, main, module, compact, exports);
    } catch (Exception ignored) {
      return null;
    }
  }

  public @NotNull List<VirtualFile> findCandidateEntryFiles(@NotNull VirtualFile packageDir, @NotNull String subpath) {
    List<VirtualFile> results = new ArrayList<>();

    if (subpath.isEmpty() || ".".equals(subpath)) {
      // 1. Check the exports field for root "."
      if (exports != null) {
        List<String> exportPaths = extractPathsFromExports(exports, ".");
        for (String path : exportPaths) {
          addFileAndCompanionDeclarations(results, packageDir, path);
        }
      }

      // 2. Direct types / typings field
      if (types != null) {
        addFile(results, packageDir, types);
      }

      // 3. Compact field
      if (compact != null) {
        addFile(results, packageDir, compact);
      }

      // 4. Module field
      if (module != null) {
        addFileAndCompanionDeclarations(results, packageDir, module);
      }

      // 5. Main field
      if (main != null) {
        addFileAndCompanionDeclarations(results, packageDir, main);
      }

      // 6. Default root fallbacks
      String[] defaults = {
          "index.d.ts", "index.d.mts", "index.d.cts", "index.ts", "index.compact", "index.js", "index.mjs", "index.cjs"
      };
      for (String def : defaults) {
        addFile(results, packageDir, def);
      }
    } else {
      // Subpath specified (e.g. "suite" or "sub/dir")
      String normSubpath = subpath.startsWith("./") ? subpath : "./" + subpath;
      if (exports != null) {
        List<String> subpathExportPaths = extractPathsFromExports(exports, normSubpath);
        for (String path : subpathExportPaths) {
          addFileAndCompanionDeclarations(results, packageDir, path);
        }
      }

      String[] subpathCandidates = {
          subpath,
          subpath + ".d.ts",
          subpath + ".d.mts",
          subpath + ".d.cts",
          subpath + ".ts",
          subpath + ".compact",
          subpath + "/index.d.ts",
          subpath + "/index.d.mts",
          subpath + "/index.ts",
          subpath + "/index.compact",
          subpath + ".js",
          subpath + "/index.js"
      };
      for (String cand : subpathCandidates) {
        addFile(results, packageDir, cand);
      }
    }

    return results;
  }

  private static @NotNull List<String> extractPathsFromExports(@NotNull JsonElement exportsElement, @NotNull String targetKey) {
    List<String> paths = new ArrayList<>();
    if (exportsElement.isJsonPrimitive()) {
      if (".".equals(targetKey)) {
        paths.add(exportsElement.getAsString());
      }
      return paths;
    }

    if (exportsElement.isJsonObject()) {
      JsonObject exportsObj = exportsElement.getAsJsonObject();

      // Check the exact match (e.g. "." or "./suite")
      if (exportsObj.has(targetKey)) {
        if (!paths.isEmpty()) {
          return paths;
        }
      }

      // Check condition keys directly at top level if targetKey is "."
      if (".".equals(targetKey)) {
        collectConditions(exportsElement, paths);
        if (!paths.isEmpty()) {
          return paths;
        }
      }

      // Wildcard subpath matching (e.g. "./*" -> "./dist/*.d.ts")
      for (Map.Entry<String, JsonElement> entry : exportsObj.entrySet()) {
        String patternKey = entry.getKey();
        if (patternKey.contains("*")) {
          String matched = matchWildcardPattern(patternKey, targetKey, entry.getValue());
          if (matched != null) {
            paths.add(matched);
          }
        }
      }
    }

    return paths;
  }

  private static void collectConditions(@NotNull JsonElement element, @NotNull List<String> out) {
    if (element.isJsonPrimitive()) {
      out.add(element.getAsString());
      return;
    }
    if (element.isJsonObject()) {
      JsonObject obj = element.getAsJsonObject();
      // Prioritize type condition
      if (obj.has("types")) {
        collectConditions(obj.get("types"), out);
      }
      if (obj.has("import")) {
        collectConditions(obj.get("import"), out);
      }
      if (obj.has("default")) {
        collectConditions(obj.get("default"), out);
      }
      if (obj.has("require")) {
        collectConditions(obj.get("require"), out);
      }
    }
  }

  private static @Nullable String matchWildcardPattern(@NotNull String patternKey, @NotNull String targetKey, @NotNull JsonElement targetValue) {
    int starIdx = patternKey.indexOf('*');
    if (starIdx == -1) return null;
    String prefix = patternKey.substring(0, starIdx);
    String suffix = patternKey.substring(starIdx + 1);

    if (targetKey.startsWith(prefix) && targetKey.endsWith(suffix) && targetKey.length() >= prefix.length() + suffix.length()) {
      String starContent = targetKey.substring(prefix.length(), targetKey.length() - suffix.length());
      if (targetValue.isJsonPrimitive()) {
        return targetValue.getAsString().replace("*", starContent);
      }
      if (targetValue.isJsonObject()) {
        List<String> conds = new ArrayList<>();
        collectConditions(targetValue, conds);
        if (!conds.isEmpty()) {
          return conds.getFirst().replace("*", starContent);
        }
      }
    }
    return null;
  }

  private static void addFileAndCompanionDeclarations(@NotNull List<VirtualFile> out, @NotNull VirtualFile packageDir, @NotNull String path) {
    String cleanPath = path.startsWith("./") ? path.substring(2) : path;
    VirtualFile file = packageDir.findFileByRelativePath(cleanPath);
    if (file != null && file.isValid() && !file.isDirectory()) {
      // If it's a declaration or source file, add it directly
      if (!isDeclarationOrSource(file)) {
        // If it's JS, check for matching .d.ts or .d.mts alongside
        findCompanionDeclarations(out, packageDir, cleanPath);
      }
      if (!out.contains(file)) out.add(file);
    } else {
      findCompanionDeclarations(out, packageDir, cleanPath);
    }
  }

  private static void findCompanionDeclarations(@NotNull List<VirtualFile> out, @NotNull VirtualFile packageDir, @NotNull String relativePath) {
    String baseWithoutExt = stripJsExtension(relativePath);
    String[] companionExtensions = {".d.ts", ".d.mts", ".d.cts", ".ts", ".compact"};
    for (String ext : companionExtensions) {
      VirtualFile candidate = packageDir.findFileByRelativePath(baseWithoutExt + ext);
      if (candidate != null && candidate.isValid() && !candidate.isDirectory() && !out.contains(candidate)) {
        out.add(candidate);
      }
    }
    // Also check index.d.ts in the same directory or package root
    int lastSlash = relativePath.lastIndexOf('/');
    if (lastSlash != -1) {
      String dirPath = relativePath.substring(0, lastSlash);
      VirtualFile dirIndex = packageDir.findFileByRelativePath(dirPath + "/index.d.ts");
      if (dirIndex != null && dirIndex.isValid() && !out.contains(dirIndex)) {
        out.add(dirIndex);
      }
    }
    VirtualFile rootIndex = packageDir.findFileByRelativePath("index.d.ts");
    if (rootIndex != null && rootIndex.isValid() && !out.contains(rootIndex)) {
      out.add(rootIndex);
    }
  }

  private static void addFile(@NotNull List<VirtualFile> out, @NotNull VirtualFile packageDir, @NotNull String path) {
    String cleanPath = path.startsWith("./") ? path.substring(2) : path;
    VirtualFile file = packageDir.findFileByRelativePath(cleanPath);
    if (file != null && file.isValid() && !file.isDirectory() && !out.contains(file)) {
      out.add(file);
    }
  }

  private static boolean isDeclarationOrSource(@NotNull VirtualFile file) {
    String name = file.getName().toLowerCase();
    return name.endsWith(".d.ts") || name.endsWith(".d.mts") || name.endsWith(".d.cts") || name.endsWith(".ts") || name.endsWith(".compact");
  }

  private static @NotNull String stripJsExtension(@NotNull String path) {
    if (path.endsWith(".js")) return path.substring(0, path.length() - 3);
    String substring = path.substring(0, path.length() - 4);
    if (path.endsWith(".mjs")) return substring;
    if (path.endsWith(".cjs")) return substring;
    return path;
  }

  private static @Nullable String getString(@NotNull JsonObject obj, @NotNull String key) {
    JsonElement elem = obj.get(key);
    return (elem != null && elem.isJsonPrimitive()) ? elem.getAsString() : null;
  }
}
