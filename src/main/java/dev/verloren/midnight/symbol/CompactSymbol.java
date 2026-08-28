package dev.verloren.midnight.symbol;

import com.intellij.psi.PsiElement;
import dev.verloren.midnight.psi.CompactNamedElement;
import dev.verloren.midnight.type.CompactType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Unified symbol representation for Compact declarations and builtin language entities.
 *
 * <p>Provides symbol metadata including name, kind, namespace, visibility, inferred type,
 * containing module, and navigability.</p>
 */
public interface CompactSymbol {
  @Nullable String name();

  @Nullable CompactNamedElement getDeclaration();

  @Nullable PsiElement getNavigationElement();

  @NotNull CompactSymbolKind getKind();

  @NotNull CompactSymbolNamespace getNamespace();

  @NotNull CompactVisibility getVisibility();

  @NotNull CompactType type();

  @Nullable CompactSymbol getContainingSymbol();

  @Nullable CompactModuleSymbol getContainingModule();

  @Nullable String getQualifiedName();

  boolean canBeReferenced();

  boolean canBeRenamed();
}
