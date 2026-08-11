package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

public class CompactContractImplementsDeclarationImpl extends CompactPsiElement implements CompactContractImplementsDeclaration {
  public CompactContractImplementsDeclarationImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public void accept(@NotNull CompactVisitor visitor) {
    visitor.visitContractImplementsDeclaration(this);
  }
}