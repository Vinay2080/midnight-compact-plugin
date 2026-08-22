package dev.verloren.midnight.resolve.npm;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.psi.CompactFile;
import dev.verloren.midnight.psi.CompactNamedElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts exported symbols from TypeScript declaration files (.d.ts), TypeScript sources (.ts),
 * JavaScript sources (.js), and Compact sources (.compact) in a npm package.
 */
public final class CompactNpmSymbolExtractor {
  private static final Pattern FUNCTION_PATTERN = Pattern.compile(
      "(?:^|\\n|;)\\s*export\\s+(?:declare\\s+)?(?:async\\s+)?function\\s+([A-Za-z0-9_$]+)"
  );
  private static final Pattern VAR_PATTERN = Pattern.compile(
      "(?:^|\\n|;)\\s*export\\s+(?:declare\\s+)?(?:const|let|var)\\s+([^;\\n\\r=]+)"
  );
  private static final Pattern CLASS_PATTERN = Pattern.compile(
      "(?:^|\\n|;)\\s*export\\s+(?:declare\\s+)?(?:abstract\\s+)?class\\s+([A-Za-z0-9_$]+)"
  );
  private static final Pattern INTERFACE_PATTERN = Pattern.compile(
      "(?:^|\\n|;)\\s*export\\s+(?:declare\\s+)?interface\\s+([A-Za-z0-9_$]+)"
  );
  private static final Pattern TYPE_PATTERN = Pattern.compile(
      "(?:^|\\n|;)\\s*export\\s+(?:declare\\s+)?type\\s+([A-Za-z0-9_$]+)"
  );
  private static final Pattern ENUM_PATTERN = Pattern.compile(
      "(?:^|\\n|;)\\s*export\\s+(?:declare\\s+)?enum\\s+([A-Za-z0-9_$]+)"
  );
  private static final Pattern NAMESPACE_PATTERN = Pattern.compile(
      "(?:^|\\n|;)\\s*export\\s+(?:declare\\s+)?(?:namespace|module)\\s+([A-Za-z0-9_$]+)"
  );
  private static final Pattern EXPORT_CLAUSE_PATTERN = Pattern.compile(
          "(?:^|\\n|;)\\s*export\\s+(?:type\\s+)?\\{([^}]+)}(?:\\s*from\\s*['\"]([^'\"]+)['\"])?"
  );
  private static final Pattern STAR_EXPORT_PATTERN = Pattern.compile(
      "(?:^|\\n|;)\\s*export\\s+\\*\\s+(?:as\\s+([A-Za-z0-9_$]+)\\s+)?from\\s*['\"]([^'\"]+)['\"];?"
  );
  private static final Pattern AMBIENT_MODULE_PATTERN = Pattern.compile(
      "declare\\s+module\\s+['\"]([^'\"]+)['\"]\\s*\\{"
  );
  private static final Pattern CJS_EXPORTS_PATTERN = Pattern.compile(
      "(?:^|\\n|;)\\s*(?:module\\.)?exports\\.([A-Za-z0-9_$]+)\\s*="
  );
  private static final Pattern CJS_OBJECT_PATTERN = Pattern.compile(
          "(?:^|\\n|;)\\s*module\\.exports\\s*=\\s*\\{([^}]+)}"
  );

  private CompactNpmSymbolExtractor() {
  }

  /**
   * Extracts all exported symbols from the given candidate entry files of a npm package.
   *
   * @param project     current project
   * @param packageDir  root directory of the package
   * @param entryFiles  candidate entry files
   * @param packageName package name (e.g. {@code "vitest"} or {@code "@midnight-ntwrk/compact-runtime"})
   * @return map of exported symbol name to {@link CompactNpmSymbolElement}
   */
  public static @NotNull Map<String, CompactNpmSymbolElement> extractAllSymbols(
      @NotNull Project project,
      @NotNull VirtualFile packageDir,
      @NotNull List<VirtualFile> entryFiles,
      @NotNull String packageName
  ) {
    Map<String, CompactNpmSymbolElement> result = new LinkedHashMap<>();
    Set<VirtualFile> visited = new HashSet<>();

    for (VirtualFile entryFile : entryFiles) {
      extractFromFile(project, entryFile, packageName, packageDir, visited, result);
    }

    // Also, inspect any ambient .d.ts files in packageDir for `declare module '<packageName>'`
    scanAmbientDeclarationFiles(project, packageDir, packageName, visited, result);

    return result;
  }

  private static void extractFromFile(
      @NotNull Project project,
      @NotNull VirtualFile file,
      @NotNull String packageName,
      @NotNull VirtualFile packageDir,
      @NotNull Set<VirtualFile> visited,
      @NotNull Map<String, CompactNpmSymbolElement> out
  ) {
    if (!visited.add(file) || !file.isValid() || file.isDirectory()) {
      return;
    }

    PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
    if (psiFile == null) {
      return;
    }

    // If Compact file, extract Compact declarations directly
    if (psiFile instanceof CompactFile compactFile) {
      for (CompactNamedElement named : PsiTreeUtil.findChildrenOfType(compactFile, CompactNamedElement.class)) {
        String name = named.getName();
        if (name != null && !name.isBlank()) {
          CompactNpmSymbolKind kind = CompactNpmSymbolKind.UNKNOWN;
          out.putIfAbsent(name, new CompactNpmSymbolElement(psiFile, name, named.getTextOffset(), kind, packageName));
        }
      }
      return;
    }

    String content;
    try {
      content = new String(file.contentsToByteArray(), StandardCharsets.UTF_8);
    } catch (Exception e) {
      return;
    }

    // 1. Check for ambient module blocks matching packageName
    extractAmbientModules(project, psiFile, content, packageName, packageDir, visited, out);

    // 2. Direct export declarations in current file
    extractDirectExports(psiFile, content, packageName, out);

    // 3. Star re-exports: export * from './other';
    extractStarExports(project, psiFile, content, packageName, packageDir, file, visited, out);

    // 4. Export clauses: export { a, b as c } from './other';
    extractExportClauses(project, psiFile, content, packageName, packageDir, file, visited, out);

    // 5. CommonJS exports
    extractCommonJsExports(psiFile, content, packageName, out);
  }

  private static void extractDirectExports(
      @NotNull PsiFile psiFile,
      @NotNull String content,
      @NotNull String packageName,
      @NotNull Map<String, CompactNpmSymbolElement> out
  ) {
    // Functions
    Matcher funcMatcher = FUNCTION_PATTERN.matcher(content);
    while (funcMatcher.find()) {
      String name = funcMatcher.group(1);
      int offset = funcMatcher.start(1);
      out.putIfAbsent(name, new CompactNpmSymbolElement(psiFile, name, offset, CompactNpmSymbolKind.FUNCTION, packageName));
    }

    // Const / Let / Var
    Matcher varMatcher = VAR_PATTERN.matcher(content);
    while (varMatcher.find()) {
      String decls = varMatcher.group(1);
      int startOffset = varMatcher.start(1);
      extractVarIdentifiers(psiFile, decls, startOffset, packageName, out);
    }

    // Classes
    Matcher classMatcher = CLASS_PATTERN.matcher(content);
    while (classMatcher.find()) {
      String name = classMatcher.group(1);
      int offset = classMatcher.start(1);
      out.putIfAbsent(name, new CompactNpmSymbolElement(psiFile, name, offset, CompactNpmSymbolKind.CLASS, packageName));
    }

    // Interfaces
    Matcher ifaceMatcher = INTERFACE_PATTERN.matcher(content);
    while (ifaceMatcher.find()) {
      String name = ifaceMatcher.group(1);
      int offset = ifaceMatcher.start(1);
      out.putIfAbsent(name, new CompactNpmSymbolElement(psiFile, name, offset, CompactNpmSymbolKind.INTERFACE, packageName));
    }

    // Types
    Matcher typeMatcher = TYPE_PATTERN.matcher(content);
    while (typeMatcher.find()) {
      String name = typeMatcher.group(1);
      int offset = typeMatcher.start(1);
      out.putIfAbsent(name, new CompactNpmSymbolElement(psiFile, name, offset, CompactNpmSymbolKind.TYPE_ALIAS, packageName));
    }

    // Enums
    Matcher enumMatcher = ENUM_PATTERN.matcher(content);
    while (enumMatcher.find()) {
      String name = enumMatcher.group(1);
      int offset = enumMatcher.start(1);
      out.putIfAbsent(name, new CompactNpmSymbolElement(psiFile, name, offset, CompactNpmSymbolKind.ENUM, packageName));
    }

    // Namespaces
    Matcher nsMatcher = NAMESPACE_PATTERN.matcher(content);
    while (nsMatcher.find()) {
      String name = nsMatcher.group(1);
      int offset = nsMatcher.start(1);
      out.putIfAbsent(name, new CompactNpmSymbolElement(psiFile, name, offset, CompactNpmSymbolKind.NAMESPACE, packageName));
    }
  }

  private static void extractVarIdentifiers(
      @NotNull PsiFile psiFile,
      @NotNull String decls,
      int baseOffset,
      @NotNull String packageName,
      @NotNull Map<String, CompactNpmSymbolElement> out
  ) {
    String[] parts = decls.split(",");
    int runningOffset = baseOffset;
    for (String part : parts) {
      String trimmed = part.trim();
      if (!trimmed.isEmpty()) {
        // Extract identifier before: or = or whitespace
        Matcher idMatcher = Pattern.compile("([A-Za-z0-9_$]+)").matcher(trimmed);
        if (idMatcher.find()) {
          String name = idMatcher.group(1);
          int offset = runningOffset + part.indexOf(name);
          out.putIfAbsent(name, new CompactNpmSymbolElement(psiFile, name, offset, CompactNpmSymbolKind.CONST, packageName));
        }
      }
      runningOffset += part.length() + 1;
    }
  }

  private static void extractStarExports(
      @NotNull Project project,
      @NotNull PsiFile psiFile,
      @NotNull String content,
      @NotNull String packageName,
      @NotNull VirtualFile packageDir,
      @NotNull VirtualFile currentFile,
      @NotNull Set<VirtualFile> visited,
      @NotNull Map<String, CompactNpmSymbolElement> out
  ) {
    Matcher starMatcher = STAR_EXPORT_PATTERN.matcher(content);
    while (starMatcher.find()) {
      String asNamespace = starMatcher.group(1);
      String relPath = starMatcher.group(2);
      if (asNamespace != null && !asNamespace.isBlank()) {
        int offset = starMatcher.start(1);
        out.putIfAbsent(asNamespace, new CompactNpmSymbolElement(psiFile, asNamespace, offset, CompactNpmSymbolKind.NAMESPACE, packageName));
      } else {
        VirtualFile target = resolveRelativeFile(currentFile, packageDir, relPath);
        if (target != null) {
          extractFromFile(project, target, packageName, packageDir, visited, out);
        }
      }
    }
  }

  private static void extractExportClauses(
      @NotNull Project project,
      @NotNull PsiFile psiFile,
      @NotNull String content,
      @NotNull String packageName,
      @NotNull VirtualFile packageDir,
      @NotNull VirtualFile currentFile,
      @NotNull Set<VirtualFile> visited,
      @NotNull Map<String, CompactNpmSymbolElement> out
  ) {
    Matcher clauseMatcher = EXPORT_CLAUSE_PATTERN.matcher(content);
    while (clauseMatcher.find()) {
      String clauseBody = clauseMatcher.group(1);
      String fromPath = clauseMatcher.group(2);

      Map<String, CompactNpmSymbolElement> fromSymbols = null;
      if (fromPath != null && !fromPath.isBlank()) {
        VirtualFile target = resolveRelativeFile(currentFile, packageDir, fromPath);
        if (target != null) {
          Map<String, CompactNpmSymbolElement> targetOut = new LinkedHashMap<>();
          extractFromFile(project, target, packageName, packageDir, visited, targetOut);
          fromSymbols = targetOut;
        }
      }

      String[] elements = clauseBody.split(",");
      for (String elem : elements) {
        String trimmed = elem.trim();
        if (trimmed.startsWith("type ")) {
          trimmed = trimmed.substring(5).trim();
        }
        if (trimmed.isEmpty()) continue;

        String sourceName = trimmed;
        String exportName = trimmed;
        if (trimmed.contains(" as ")) {
          String[] asParts = trimmed.split("\\s+as\\s+");
          sourceName = asParts[0].trim();
          exportName = asParts.length > 1 ? asParts[1].trim() : sourceName;
        }

        if (fromSymbols != null && fromSymbols.containsKey(sourceName)) {
          CompactNpmSymbolElement original = fromSymbols.get(sourceName);
          out.putIfAbsent(exportName, new CompactNpmSymbolElement(
              original.getContainingFile(),
              exportName,
              original.getTextOffset(),
              original.getKind(),
              packageName
          ));
        } else {
          int offset = clauseMatcher.start(1) + clauseBody.indexOf(exportName);
          out.putIfAbsent(exportName, new CompactNpmSymbolElement(
              psiFile,
              exportName,
              Math.max(0, offset),
              CompactNpmSymbolKind.UNKNOWN,
              packageName
          ));
        }
      }
    }
  }

  private static void extractAmbientModules(
      @NotNull Project project,
      @NotNull PsiFile psiFile,
      @NotNull String content,
      @NotNull String packageName,
      @NotNull VirtualFile packageDir,
      @NotNull Set<VirtualFile> visited,
      @NotNull Map<String, CompactNpmSymbolElement> out
  ) {
    Matcher modMatcher = AMBIENT_MODULE_PATTERN.matcher(content);
    while (modMatcher.find()) {
      String modName = modMatcher.group(1);
      if (modName.endsWith("/" + packageName) || packageName.endsWith(modName)) {
        int braceStart = modMatcher.end() - 1;
        String blockContent = extractBraceBlock(content, braceStart);
        if (blockContent != null) {
          // Extract direct declarations inside ambient module block
          extractDirectExports(psiFile, blockContent, packageName, out);
        }
      }
    }
  }

  private static void extractCommonJsExports(
      @NotNull PsiFile psiFile,
      @NotNull String content,
      @NotNull String packageName,
      @NotNull Map<String, CompactNpmSymbolElement> out
  ) {
    Matcher cjsPropMatcher = CJS_EXPORTS_PATTERN.matcher(content);
    while (cjsPropMatcher.find()) {
      String name = cjsPropMatcher.group(1);
      int offset = cjsPropMatcher.start(1);
      out.putIfAbsent(name, new CompactNpmSymbolElement(psiFile, name, offset, CompactNpmSymbolKind.FUNCTION, packageName));
    }

    Matcher cjsObjMatcher = CJS_OBJECT_PATTERN.matcher(content);
    while (cjsObjMatcher.find()) {
      String body = cjsObjMatcher.group(1);
      int baseOffset = cjsObjMatcher.start(1);
      String[] entries = body.split(",");
      for (String entry : entries) {
        String trimmed = entry.trim();
        if (trimmed.isEmpty()) continue;
        int colonIdx = trimmed.indexOf(':');
        String key = (colonIdx != -1 ? trimmed.substring(0, colonIdx) : trimmed).trim();
        Matcher keyMatcher = Pattern.compile("([A-Za-z0-9_$]+)").matcher(key);
        if (keyMatcher.find()) {
          String name = keyMatcher.group(1);
          int offset = baseOffset + body.indexOf(name);
          out.putIfAbsent(name, new CompactNpmSymbolElement(psiFile, name, Math.max(0, offset), CompactNpmSymbolKind.FUNCTION, packageName));
        }
      }
    }
  }

  private static void scanAmbientDeclarationFiles(
      @NotNull Project project,
      @NotNull VirtualFile packageDir,
      @NotNull String packageName,
      @NotNull Set<VirtualFile> visited,
      @NotNull Map<String, CompactNpmSymbolElement> out
  ) {
    for (VirtualFile child : packageDir.getChildren()) {
      if (!child.isDirectory() && child.getName().endsWith(".d.ts")) {
        extractFromFile(project, child, packageName, packageDir, visited, out);
      }
    }
  }

  private static @Nullable String extractBraceBlock(@NotNull String text, int openBraceIndex) {
    if (openBraceIndex < 0 || openBraceIndex >= text.length() || text.charAt(openBraceIndex) != '{') {
      return null;
    }
    int depth = 0;
    for (int i = openBraceIndex; i < text.length(); i++) {
      char c = text.charAt(i);
      if (c == '{') {
        depth++;
      } else if (c == '}') {
        depth--;
        if (depth == 0) {
          return text.substring(openBraceIndex + 1, i);
        }
      }
    }
    return text.substring(openBraceIndex + 1);
  }

  public static @Nullable VirtualFile resolveRelativeFile(
      @NotNull VirtualFile currentFile,
      @NotNull VirtualFile packageDir,
      @NotNull String relPath
  ) {
    VirtualFile startDir = currentFile.isDirectory() ? currentFile : currentFile.getParent();
    if (startDir == null) {
      startDir = packageDir;
    }

    String cleanPath = relPath.startsWith("./") ? relPath.substring(2) : relPath;
    String[] candidates = {
        cleanPath,
        cleanPath + ".d.ts",
        cleanPath + ".d.mts",
        cleanPath + ".d.cts",
        cleanPath + ".ts",
        cleanPath + ".compact",
        cleanPath + "/index.d.ts",
        cleanPath + "/index.d.mts",
        cleanPath + "/index.ts",
        cleanPath + "/index.compact",
        cleanPath + ".js",
        cleanPath + "/index.js"
    };

    for (String cand : candidates) {
      VirtualFile f = startDir.findFileByRelativePath(cand);
      if (f != null && f.isValid() && !f.isDirectory()) {
        return f;
      }
      VirtualFile pkgRel = packageDir.findFileByRelativePath(cand);
      if (pkgRel != null && pkgRel.isValid() && !pkgRel.isDirectory()) {
        return pkgRel;
      }
    }

    return null;
  }
}
