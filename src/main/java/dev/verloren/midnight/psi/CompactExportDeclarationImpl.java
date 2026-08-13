package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

public class CompactExportDeclarationImpl extends CompactPsiElement implements CompactExportDeclaration {
  public CompactExportDeclarationImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public void accept(@NotNull CompactVisitor visitor) {
    visitor.visitExportDeclaration(this);
  }
}