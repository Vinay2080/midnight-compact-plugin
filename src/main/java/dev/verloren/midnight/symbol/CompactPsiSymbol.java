package dev.verloren.midnight.symbol;

import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.psi.CompactModuleDefinition;
import dev.verloren.midnight.psi.CompactNamedElement;
import dev.verloren.midnight.type.CompactPrimitiveType;
import dev.verloren.midnight.type.CompactType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class CompactPsiSymbol implements CompactSymbol {
  private final CompactNamedElement declaration;
  private final CompactSymbolKind kind;
  private final CompactSymbolNamespace namespace;

  protected CompactPsiSymbol(@NotNull CompactNamedElement declaration,
                             @NotNull CompactSymbolKind kind,
                             @NotNull CompactSymbolNamespace namespace) {
    this.declaration = declaration;
    this.kind = kind;
    this.namespace = namespace;
  }

  @Override
  public @Nullable String getName() {
    return declaration.getName();
  }

  @Override
  public @NotNull CompactNamedElement getDeclaration() {
    return declaration;
  }

  @Override
  public @Nullable PsiElement getNavigationElement() {
    return declaration.getNavigationElement();
  }

  @Override
  public @NotNull CompactSymbolKind getKind() {
    return kind;
  }

  @Override
  public @NotNull CompactSymbolNamespace getNamespace() {
    return namespace;
  }

  @Override
  public @NotNull CompactVisibility getVisibility() {
    return CompactSymbols.visibilityOf(declaration);
  }

  @Override
  public @NotNull CompactType getType() {
    return declaration.isValid() ? declaration.getType() : CompactPrimitiveType.UNKNOWN;
  }

  @Override
  public @Nullable CompactSymbol getContainingSymbol() {
    CompactNamedElement parent = PsiTreeUtil.getParentOfType(declaration, CompactNamedElement.class);
    while (parent != null && parent == declaration) {
      parent = PsiTreeUtil.getParentOfType(parent, CompactNamedElement.class, true);
    }
    return parent == null ? null : CompactSymbols.from(parent);
  }

  @Override
  public @Nullable CompactModuleSymbol getContainingModule() {
    if (declaration instanceof CompactModuleDefinition) {
      return null;
    }
    CompactModuleDefinition module = PsiTreeUtil.getParentOfType(declaration, CompactModuleDefinition.class);
    CompactSymbol symbol = module == null ? null : CompactSymbols.from(module);
    return symbol instanceof CompactModuleSymbol ? (CompactModuleSymbol)symbol : null;
  }

  @Override
  public @Nullable String getQualifiedName() {
    String name = getName();
    if (name == null) {
      return null;
    }
    CompactModuleSymbol module = getContainingModule();
    String moduleName = module == null ? null : module.getQualifiedName();
    return moduleName == null ? name : moduleName + "." + name;
  }

  @Override
  public boolean canBeReferenced() {
    return namespace != CompactSymbolNamespace.UNKNOWN && getName() != null;
  }

  @Override
  public boolean canBeRenamed() {
    return declaration.getNameIdentifier() != null;
  }

  public static final class Value extends CompactPsiSymbol implements CompactValueSymbol {
    public Value(@NotNull CompactNamedElement declaration, @NotNull CompactSymbolKind kind) {
      super(declaration, kind, CompactSymbolNamespace.VALUE);
    }
  }

  public static final class Type extends CompactPsiSymbol implements CompactTypeSymbol {
    public Type(@NotNull CompactNamedElement declaration, @NotNull CompactSymbolKind kind) {
      super(declaration, kind, CompactSymbolNamespace.TYPE);
    }
  }

  public static final class Module extends CompactPsiSymbol implements CompactModuleSymbol {
    public Module(@NotNull CompactNamedElement declaration) {
      super(declaration, CompactSymbolKind.MODULE, CompactSymbolNamespace.MODULE);
    }
  }

  public static final class Unknown extends CompactPsiSymbol {
    public Unknown(@NotNull CompactNamedElement declaration) {
      super(declaration, CompactSymbolKind.UNKNOWN, CompactSymbolNamespace.UNKNOWN);
    }
  }
}
