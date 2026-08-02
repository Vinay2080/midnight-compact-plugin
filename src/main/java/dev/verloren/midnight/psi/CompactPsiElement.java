package dev.verloren.midnight.psi;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

public class CompactPsiElement extends ASTWrapperPsiElement {
  public CompactPsiElement(@NotNull ASTNode node) {
    super(node);
  }
}
