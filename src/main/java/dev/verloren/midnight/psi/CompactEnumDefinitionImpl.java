package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

public class CompactEnumDefinitionImpl extends CompactNamedElementImpl implements CompactEnumDefinition {
  public CompactEnumDefinitionImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public void accept(@NotNull CompactVisitor visitor) {
    visitor.visitEnumDefinition(this);
  }
}