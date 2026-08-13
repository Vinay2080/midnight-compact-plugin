package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import dev.verloren.midnight.type.CompactPrimitiveType;
import dev.verloren.midnight.type.CompactType;
import org.jetbrains.annotations.NotNull;

public class CompactEnumDefinitionImpl extends CompactNamedElementImpl implements CompactEnumDefinition {
  public CompactEnumDefinitionImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public @NotNull CompactType getType() {
    return new CompactPrimitiveType(getName() != null ? getName() : "enum");
  }

  @Override
  public void accept(@NotNull CompactVisitor visitor) {
    visitor.visitEnumDefinition(this);
  }
}