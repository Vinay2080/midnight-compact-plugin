package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

public class CompactConstBindingImpl extends CompactPsiElement {
  public CompactConstBindingImpl(@NotNull ASTNode node) {
    super(node);
  }
}