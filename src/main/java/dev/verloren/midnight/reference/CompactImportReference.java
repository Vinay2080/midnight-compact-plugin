package dev.verloren.midnight.reference;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.ResolveResult;
import dev.verloren.midnight.psi.CompactImportDeclarationImpl;
import dev.verloren.midnight.psi.CompactImportElementImpl;
import dev.verloren.midnight.psi.CompactModuleDefinition;
import dev.verloren.midnight.psi.CompactNamedElement;
import dev.verloren.midnight.resolve.CompactResolveUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CompactImportReference extends CompactReferenceBase {
  private final Kind kind;

  public CompactImportReference(@NotNull PsiElement element, @NotNull TextRange rangeInElement, @NotNull Kind kind) {
    super(element, rangeInElement);
    this.kind = kind;
  }

  @Override
  protected ResolveResult @NotNull [] resolveInner() {
    if (kind == Kind.MODULE && getElement() instanceof CompactImportDeclarationImpl) {
      CompactModuleDefinition module = CompactResolveUtil.findModule(getElement(), getValue());
      return module == null ? ResolveResult.EMPTY_ARRAY : toResults(List.of(module));
    }
    if (kind == Kind.IMPORT_ELEMENT && getElement() instanceof CompactImportElementImpl) {
      CompactNamedElement target = CompactResolveUtil.resolveImportElementSource((CompactImportElementImpl) getElement());
      return target == null ? ResolveResult.EMPTY_ARRAY : toResults(List.of(target));
    }
    return ResolveResult.EMPTY_ARRAY;
  }

  public enum Kind {
    MODULE,
    IMPORT_ELEMENT
  }
}