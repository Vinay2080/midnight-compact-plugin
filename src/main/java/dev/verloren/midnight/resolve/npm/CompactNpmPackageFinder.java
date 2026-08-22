package dev.verloren.midnight.resolve.npm;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * Discovers the installation directory of a npm package in the project's {@code node_modules}.
 */
public final class CompactNpmPackageFinder {
  private CompactNpmPackageFinder() {
  }

  /**
   * Finds the root directory of the specified npm package relative to the given context element.
   *
   * @param context     PSI element from which the import originates
   * @param packageName npm package name (e.g. {@code "vitest"} or {@code "@midnight-ntwrk/compact-runtime"})
   * @return {@link VirtualFile} representing the package root directory, or {@code null} if not found
   */
  public static @Nullable VirtualFile findPackageDir(@NotNull PsiElement context, @NotNull String packageName) {
    Project project = context.getProject();
    PsiFile containingFile = context.getContainingFile();

    VirtualFile startDir = null;
    if (containingFile != null) {
      VirtualFile vFile = containingFile.getOriginalFile().getVirtualFile();
      if (vFile == null) {
        vFile = containingFile.getViewProvider().getVirtualFile();
      }
      startDir = vFile.isDirectory() ? vFile : vFile.getParent();
    }

    Set<VirtualFile> visitedDirs = new HashSet<>();

    // 1. Walk upward through the directory hierarchy from the containing file
    for (VirtualFile dir = startDir; dir != null && visitedDirs.add(dir); dir = dir.getParent()) {
      VirtualFile pkgDir = checkNodeModulesInDir(dir, packageName);
      if (pkgDir != null) {
        return pkgDir;
      }
    }

    // 2. Search project content roots
    VirtualFile[] contentRoots = ProjectRootManager.getInstance(project).getContentRoots();
    for (VirtualFile root : contentRoots) {
      if (visitedDirs.add(root)) {
        VirtualFile pkgDir = checkNodeModulesInDir(root, packageName);
        if (pkgDir != null) {
          return pkgDir;
        }
      }
    }

    // 3. Search the project base directory
    String basePath = project.getBasePath();
    if (basePath != null) {
      VirtualFile baseDir = LocalFileSystem.getInstance().findFileByPath(basePath);
      if (baseDir != null && visitedDirs.add(baseDir)) {
        VirtualFile pkgDir = checkNodeModulesInDir(baseDir, packageName);
        if (pkgDir != null) {
          return pkgDir;
        }
      }
    }

    // 4. Also check @types/<packageName> fallback
    String typesPackageName = toTypesPackageName(packageName);
    if (typesPackageName != null && !typesPackageName.equals(packageName)) {
      visitedDirs.clear();
      for (VirtualFile dir = startDir; dir != null && visitedDirs.add(dir); dir = dir.getParent()) {
        VirtualFile pkgDir = checkNodeModulesInDir(dir, typesPackageName);
        if (pkgDir != null) {
          return pkgDir;
        }
      }
      for (VirtualFile root : contentRoots) {
        if (visitedDirs.add(root)) {
          VirtualFile pkgDir = checkNodeModulesInDir(root, typesPackageName);
          if (pkgDir != null) {
            return pkgDir;
          }
        }
      }
    }

    return null;
  }

  private static @Nullable VirtualFile checkNodeModulesInDir(@NotNull VirtualFile dir, @NotNull String packageName) {
    // Check direct relative path if dir itself is node_modules
    if ("node_modules".equals(dir.getName())) {
      VirtualFile direct = dir.findFileByRelativePath(packageName);
      if (direct != null && direct.isDirectory()) {
        return direct;
      }
    }

    VirtualFile nodeModules = dir.findChild("node_modules");
    if (nodeModules != null && nodeModules.isDirectory()) {
      VirtualFile direct = nodeModules.findFileByRelativePath(packageName);
      if (direct != null && direct.isDirectory()) {
        return direct;
      }
    }
    return null;
  }

  private static @Nullable String toTypesPackageName(@NotNull String packageName) {
    if (packageName.startsWith("@types/")) {
      return null;
    }
    if (packageName.startsWith("@")) {
      // @scope/name -> @types/scope__name
      String withoutAt = packageName.substring(1);
      return "@types/" + withoutAt.replace("/", "__");
    }
    return "@types/" + packageName;
  }
}
