package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.util.IncorrectOperationException;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import dev.verloren.midnight.type.CompactPrimitiveType;
import dev.verloren.midnight.type.CompactType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class CompactNamedElementImpl extends CompactPsiElement implements CompactNamedElement {
  protected CompactNamedElementImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public @NotNull CompactType getType() {
    return CompactPrimitiveType.UNKNOWN;
  }

  @Override
  public @Nullable PsiElement getNameIdentifier() {
    ASTNode identifier = getNode().findChildByType(CompactTokenTypes.IDENTIFIER);
    return identifier == null ? null : identifier.getPsi();
  }

  @Override
  public @Nullable String getName() {
    PsiElement nameIdentifier = getNameIdentifier();
    return nameIdentifier == null ? null : nameIdentifier.getText();
  }

  @Override
  public PsiElement setName(@NotNull String name) throws IncorrectOperationException {
    PsiElement nameIdentifier = getNameIdentifier();
    if (nameIdentifier == null) {
      throw new IncorrectOperationException("Cannot rename Compact element without a name");
    }
    PsiElement newIdentifier = CompactElementFactory.createIdentifierLeaf(getProject(), name);
    nameIdentifier.replace(newIdentifier);
    return this;
  }

  @Override
  public int getTextOffset() {
    PsiElement nameIdentifier = getNameIdentifier();
    return nameIdentifier == null ? super.getTextOffset() : nameIdentifier.getTextOffset();
  }

  @Override
  public @NotNull com.intellij.psi.search.SearchScope getUseScope() {
    return new com.intellij.psi.search.LocalSearchScope(getContainingFile());
  }
}