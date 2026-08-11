package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

public class CompactStructFieldImpl extends CompactNamedElementImpl {
  public CompactStructFieldImpl(@NotNull ASTNode node) {
    super(node);
  }
}