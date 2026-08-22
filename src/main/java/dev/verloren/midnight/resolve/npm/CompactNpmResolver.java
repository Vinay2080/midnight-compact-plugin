package dev.verloren.midnight.resolve.npm;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import dev.verloren.midnight.psi.CompactImportDeclarationImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Main resolution engine for external npm dependencies in Compact smart contracts.
 *
 * <p>Handles:
 * <ul>
 *   <li>Distinguishing relative local paths from external npm package specifiers.</li>
 *   <li>Locating package directories inside the project's {@code node_modules}.</li>
 *   <li>Reading package metadata ({@code package.json}, {@code exports}, {@code types}, {@code main}).</li>
 *   <li>Resolving imported symbols against package declarations.</li>
 * </ul>
 * </p>
 */
public final class CompactNpmResolver {
  private CompactNpmResolver() {
  }

  /**
   * Checks if an import path string represents an external npm package rather than a local relative path.
   *
   * @param path the raw import path from source text
   * @return {@code true} if the path is an external npm package (e.g. {@code "vitest"} or {@code "@midnight-ntwrk/compact-runtime"})
   */
  public static boolean isExternalPackagePath(@Nullable String path) {
    if (path == null || path.isBlank()) {
      return false;
    }
    String clean = path.trim();
    if (clean.startsWith(".") || clean.startsWith("/") || clean.startsWith("\\")) {
      return false;
    }
    return !clean.endsWith(".compact");
  }

  /**
   * Resolves the navigation target for an npm package import path (e.g. entry {@link PsiFile}, {@code package.json}, or package {@link PsiDirectory}).
   *
   * @param importDecl the import declaration containing the package string literal
   * @return resolved {@link PsiElement} or {@code null} if package not installed
   */
  public static @Nullable PsiElement resolvePackageTarget(@NotNull CompactImportDeclarationImpl importDecl) {
    String path = importDecl.getImportPath();
    if (!isExternalPackagePath(path)) {
      return null;
    }

    CompactNpmPackageSpec spec = CompactNpmPackageSpec.parse(path);
    if (spec == null) {
      return null;
    }

    VirtualFile packageDir = CompactNpmPackageFinder.findPackageDir(importDecl, spec.packageName());
    if (packageDir == null) {
      return null;
    }

    Project project = importDecl.getProject();
    PsiManager psiManager = PsiManager.getInstance(project);

    // Try finding candidate entry file
    CompactNpmPackageMetadata metadata = CompactNpmPackageMetadata.from(packageDir);
    List<VirtualFile> candidateFiles = metadata != null
        ? metadata.findCandidateEntryFiles(packageDir, spec.subpath())
        : Collections.emptyList();

    for (VirtualFile candidate : candidateFiles) {
      PsiFile psiFile = psiManager.findFile(candidate);
      if (psiFile != null) {
        return psiFile;
      }
    }

    // Fallback: package.json or package directory
    VirtualFile packageJson = packageDir.findChild("package.json");
    if (packageJson != null && packageJson.isValid()) {
      PsiFile jsonFile = psiManager.findFile(packageJson);
      if (jsonFile != null) {
        return jsonFile;
      }
    }

    return psiManager.findDirectory(packageDir);
  }

  /**
   * Resolves a specific imported symbol name from the external npm package referenced by an import declaration.
   *
   * @param importDecl the import declaration
   * @param symbolName the imported identifier name
   * @return {@link CompactNpmSymbolElement} if resolved, or {@code null} if nonexistent or package missing
   */
  public static @Nullable CompactNpmSymbolElement resolveImportedSymbol(
      @NotNull CompactImportDeclarationImpl importDecl,
      @NotNull String symbolName
  ) {
    Map<String, CompactNpmSymbolElement> symbolMap = getCachedPackageSymbols(importDecl);
    return symbolMap.get(symbolName);
  }

  /**
   * Collects all exported symbols available from the npm package referenced by an import declaration.
   *
   * @param importDecl the import declaration
   * @return collection of exported symbols
   */
  public static @NotNull Collection<CompactNpmSymbolElement> collectPackageSymbols(
      @NotNull CompactImportDeclarationImpl importDecl
  ) {
    return getCachedPackageSymbols(importDecl).values();
  }

  private static @NotNull Map<String, CompactNpmSymbolElement> getCachedPackageSymbols(
      @NotNull CompactImportDeclarationImpl importDecl
  ) {
    return CachedValuesManager.getCachedValue(importDecl, () ->
        CachedValueProvider.Result.create(
            doExtractPackageSymbols(importDecl),
            PsiModificationTracker.MODIFICATION_COUNT
        )
    );
  }

  private static @NotNull Map<String, CompactNpmSymbolElement> doExtractPackageSymbols(
      @NotNull CompactImportDeclarationImpl importDecl
  ) {
    String path = importDecl.getImportPath();
    if (!isExternalPackagePath(path)) {
      return Collections.emptyMap();
    }

    CompactNpmPackageSpec spec = CompactNpmPackageSpec.parse(path);
    if (spec == null) {
      return Collections.emptyMap();
    }

    VirtualFile packageDir = CompactNpmPackageFinder.findPackageDir(importDecl, spec.packageName());
    if (packageDir == null) {
      return Collections.emptyMap();
    }

    CompactNpmPackageMetadata metadata = CompactNpmPackageMetadata.from(packageDir);
    List<VirtualFile> candidateFiles = metadata != null
        ? metadata.findCandidateEntryFiles(packageDir, spec.subpath())
        : Collections.emptyList();

    if (candidateFiles.isEmpty()) {
      // Default fallbacks
      String[] defaults = {"index.d.ts", "index.d.mts", "index.d.cts", "index.ts", "index.compact", "index.js"};
      candidateFiles = new java.util.ArrayList<>();
      for (String def : defaults) {
        VirtualFile f = packageDir.findFileByRelativePath(def);
        if (f != null && f.isValid() && !f.isDirectory()) {
          candidateFiles.add(f);
        }
      }
    }

    return CompactNpmSymbolExtractor.extractAllSymbols(
        importDecl.getProject(),
        packageDir,
        candidateFiles,
        spec.packageName()
    );
  }
}
