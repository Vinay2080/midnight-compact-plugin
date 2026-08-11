package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

public class CompactTypeDefinitionImpl extends CompactNamedElementImpl implements CompactTypeDefinition {
  public CompactTypeDefinitionImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public void accept(@NotNull CompactVisitor visitor) {
    visitor.visitTypeDefinition(this);
  }
}