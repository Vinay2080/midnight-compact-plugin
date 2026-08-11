package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

public class CompactImportDeclarationImpl extends CompactPsiElement implements CompactImportDeclaration {
  public CompactImportDeclarationImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public void accept(@NotNull CompactVisitor visitor) {
    visitor.visitImportDeclaration(this);
  }
}