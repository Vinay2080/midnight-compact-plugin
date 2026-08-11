package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

public class CompactStructDefinitionImpl extends CompactNamedElementImpl implements CompactStructDefinition {
  public CompactStructDefinitionImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public void accept(@NotNull CompactVisitor visitor) {
    visitor.visitStructDefinition(this);
  }
}