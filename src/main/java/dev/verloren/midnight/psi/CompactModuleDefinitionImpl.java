package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

public class CompactModuleDefinitionImpl extends CompactNamedElementImpl implements CompactModuleDefinition {
  public CompactModuleDefinitionImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public void accept(@NotNull CompactVisitor visitor) {
    visitor.visitModuleDefinition(this);
  }
}