package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

public class CompactIncludeDeclarationImpl extends CompactPsiElement implements CompactIncludeDeclaration {
  public CompactIncludeDeclarationImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public void accept(@NotNull CompactVisitor visitor) {
    visitor.visitIncludeDeclaration(this);
  }
}