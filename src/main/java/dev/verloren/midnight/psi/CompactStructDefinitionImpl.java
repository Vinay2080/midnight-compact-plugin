package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import dev.verloren.midnight.type.CompactPrimitiveType;
import dev.verloren.midnight.type.CompactType;
import org.jetbrains.annotations.NotNull;

public class CompactStructDefinitionImpl extends CompactNamedElementImpl implements CompactStructDefinition {
  public CompactStructDefinitionImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public @NotNull CompactType getType() {
    // In a full implementation, this would return a CompactStructType
    return new CompactPrimitiveType(getName() != null ? getName() : "struct");
  }

  @Override
  public void accept(@NotNull CompactVisitor visitor) {
    visitor.visitStructDefinition(this);
  }
}