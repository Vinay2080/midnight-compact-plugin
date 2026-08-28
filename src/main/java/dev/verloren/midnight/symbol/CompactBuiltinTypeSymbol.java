package dev.verloren.midnight.symbol;

import com.intellij.psi.PsiElement;
import dev.verloren.midnight.psi.CompactNamedElement;
import dev.verloren.midnight.type.CompactType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record CompactBuiltinTypeSymbol(String name, CompactType type) implements CompactTypeSymbol {
  public CompactBuiltinTypeSymbol(@NotNull String name, @NotNull CompactType type) {
    this.name = name;
    this.type = type;
  }

  @Override
  public @NotNull String name() {
    return name;
  }

  @Override
  public @Nullable CompactNamedElement getDeclaration() {
    return null;
  }

  @Override
  public @Nullable PsiElement getNavigationElement() {
    return null;
  }

  @Override
  public @NotNull CompactSymbolKind getKind() {
    return CompactSymbolKind.BUILTIN_TYPE;
  }

  @Override
  public @NotNull CompactSymbolNamespace getNamespace() {
    return CompactSymbolNamespace.TYPE;
  }

  @Override
  public @NotNull CompactVisibility getVisibility() {
    return CompactVisibility.BUILTIN;
  }

  @Override
  public @NotNull CompactType type() {
    return type;
  }

  @Override
  public @Nullable CompactSymbol getContainingSymbol() {
    return null;
  }

  @Override
  public @Nullable CompactModuleSymbol getContainingModule() {
    return null;
  }

  @Override
  public @NotNull String getQualifiedName() {
    return name;
  }

  @Override
  public boolean canBeReferenced() {
    return true;
  }

  @Override
  public boolean canBeRenamed() {
    return false;
  }
}
