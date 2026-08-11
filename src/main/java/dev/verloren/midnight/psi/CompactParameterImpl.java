package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

public class CompactParameterImpl extends CompactNamedElementImpl {
  public CompactParameterImpl(@NotNull ASTNode node) {
    super(node);
  }
}