package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

public class CompactReferenceExprImpl extends CompactPsiElement implements CompactReferenceExpr {
  public CompactReferenceExprImpl(@NotNull ASTNode node) {
    super(node);
  }
}