package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

public class CompactConstructorDeclarationImpl extends CompactPsiElement implements CompactConstructorDeclaration {
  public CompactConstructorDeclarationImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public void accept(@NotNull CompactVisitor visitor) {
    visitor.visitConstructorDeclaration(this);
  }
}