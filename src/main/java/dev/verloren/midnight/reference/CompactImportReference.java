package dev.verloren.midnight.reference;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.psi.*;
import dev.verloren.midnight.resolve.CompactResolveUtil;
import dev.verloren.midnight.stdlib.CompactStdlibService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Resolves imported module identifiers, file paths, and selectively imported symbols in Compact import declarations.
 *
 * <p>Handles three cases:
 * <ul>
 *   <li>{@link Kind#MODULE}: Resolves the module name in {@code import ModuleName;} to a {@link dev.verloren.midnight.psi.CompactModuleDefinition} or {@link CompactFile}.</li>
 *   <li>{@link Kind#FILE}: Resolves the file path in {@code import ... from './path';} to a {@link CompactFile}.</li>
 *   <li>{@link Kind#IMPORT_ELEMENT}: Resolves the source identifier in {@code import { Symbol } from ...;} to the exported declaration inside the file or module.</li>
 * </ul>
 * </p>
 */
public class CompactImportReference extends CompactReferenceBase {
  private final Kind kind;

  public CompactImportReference(@NotNull PsiElement element, @NotNull TextRange rangeInElement, @NotNull Kind kind) {
    super(element, rangeInElement);
    this.kind = kind;
  }

  @Override
  protected ResolveResult @NotNull [] resolveInner() {
    CompactImportDeclarationImpl importDecl = getImportDeclaration();

    if (kind == Kind.FILE) {
      if (isStandardLibrary(getValue())) {
        List<CompactFile> stdlib = CompactStdlibService.getInstance(getElement().getProject()).getStandardLibraryFiles();
        if (!stdlib.isEmpty()) {
          return toResults(List.of(stdlib.getFirst()));
        }
      }
      CompactFile file = importDecl != null ? importDecl.resolveImportedFile() : null;
      return file == null ? ResolveResult.EMPTY_ARRAY : toResults(List.of(file));
    }
    if (kind == Kind.MODULE) {
      if (isStandardLibrary(getValue())) {
        List<CompactFile> stdlib = CompactStdlibService.getInstance(getElement().getProject()).getStandardLibraryFiles();
        if (!stdlib.isEmpty()) {
          return toResults(List.of(stdlib.getFirst()));
        }
      }
      CompactModuleDefinition module = CompactResolveUtil.findModule(getElement(), getValue());
      if (module != null) {
        return toResults(List.of(module));
      }
      CompactFile file = importDecl != null ? importDecl.resolveImportedFile() : null;
      return file == null ? ResolveResult.EMPTY_ARRAY : toResults(List.of(file));
    }
    if (kind == Kind.IMPORT_ELEMENT && getElement() instanceof CompactImportElementImpl importElement) {
      CompactNamedElement target = CompactResolveUtil.resolveImportElementSource(importElement);
      return target == null ? ResolveResult.EMPTY_ARRAY : toResults(List.of(target));
    }
    return ResolveResult.EMPTY_ARRAY;
  }

  private @Nullable CompactImportDeclarationImpl getImportDeclaration() {
    if (getElement() instanceof CompactImportDeclarationImpl decl) {
      return decl;
    }
    return PsiTreeUtil.getParentOfType(getElement(), CompactImportDeclarationImpl.class);
  }

  private static boolean isStandardLibrary(@NotNull String name) {
    return "CompactStandardLibrary".equals(name)
        || "standard-library".equals(name)
        || "standard-library.compact".equals(name);
  }

  public enum Kind {
    MODULE,
    FILE,
    IMPORT_ELEMENT
  }
}
