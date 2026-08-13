package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;

import com.intellij.psi.PsiElement;

import com.intellij.psi.tree.TokenSet;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CompactImportElementImpl extends CompactNamedElementImpl {
  public CompactImportElementImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public @Nullable PsiElement getNameIdentifier() {
    PsiElement[] identifiers = findIdentifierChildren();
    if (identifiers.length == 0) {
      return null;
    }
    return identifiers[identifiers.length - 1];
  }

  private PsiElement @NotNull [] findIdentifierChildren() {
    ASTNode[] nodes = getNode().getChildren(TokenSet.create(CompactTokenTypes.IDENTIFIER));
    PsiElement[] elements = new PsiElement[nodes.length];
    for (int i = 0; i < nodes.length; i++) {
      elements[i] = nodes[i].getPsi();
    }
    return elements;
  }

  public @Nullable PsiElement getSourceIdentifier() {
    PsiElement[] identifiers = findIdentifierChildren();
    return identifiers.length == 0 ? null : identifiers[0];
  }
}