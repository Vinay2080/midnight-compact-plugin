package dev.verloren.midnight.reference;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.ResolveResult;
import dev.verloren.midnight.psi.CompactFile;
import dev.verloren.midnight.psi.CompactImportDeclarationImpl;
import dev.verloren.midnight.psi.CompactImportElementImpl;
import dev.verloren.midnight.psi.CompactModuleDefinition;
import dev.verloren.midnight.psi.CompactNamedElement;
import dev.verloren.midnight.resolve.CompactResolveUtil;
import org.jetbrains.annotations.NotNull;

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
    if (kind == Kind.FILE && getElement() instanceof CompactImportDeclarationImpl) {
      CompactFile file = ((CompactImportDeclarationImpl) getElement()).resolveImportedFile();
      return file == null ? ResolveResult.EMPTY_ARRAY : toResults(List.of(file));
    }
    if (kind == Kind.MODULE && getElement() instanceof CompactImportDeclarationImpl) {
      CompactModuleDefinition module = CompactResolveUtil.findModule(getElement(), getValue());
      if (module != null) {
        return toResults(List.of(module));
      }
      CompactFile file = ((CompactImportDeclarationImpl) getElement()).resolveImportedFile();
      return file == null ? ResolveResult.EMPTY_ARRAY : toResults(List.of(file));
    }
    if (kind == Kind.IMPORT_ELEMENT && getElement() instanceof CompactImportElementImpl) {
      CompactNamedElement target = CompactResolveUtil.resolveImportElementSource((CompactImportElementImpl) getElement());
      return target == null ? ResolveResult.EMPTY_ARRAY : toResults(List.of(target));
    }
    return ResolveResult.EMPTY_ARRAY;
  }

  public enum Kind {
    MODULE,
    FILE,
    IMPORT_ELEMENT
  }
}