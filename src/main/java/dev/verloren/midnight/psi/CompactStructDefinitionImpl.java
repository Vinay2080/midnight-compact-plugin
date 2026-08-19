package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.type.CompactPrimitiveType;
import dev.verloren.midnight.type.CompactType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class CompactStructDefinitionImpl extends CompactNamedElementImpl implements CompactStructDefinition {
  public CompactStructDefinitionImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public @NotNull List<CompactStructFieldImpl> getFields() {
    return new ArrayList<>(PsiTreeUtil.findChildrenOfType(this, CompactStructFieldImpl.class));
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