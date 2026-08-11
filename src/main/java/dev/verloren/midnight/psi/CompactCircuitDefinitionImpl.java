package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

public class CompactCircuitDefinitionImpl extends CompactNamedElementImpl implements CompactCircuitDefinition {
  public CompactCircuitDefinitionImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public void accept(@NotNull CompactVisitor visitor) {
    visitor.visitCircuitDefinition(this);
  }
}