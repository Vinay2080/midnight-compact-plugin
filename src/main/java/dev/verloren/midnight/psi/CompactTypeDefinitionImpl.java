package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.type.CompactPrimitiveType;
import dev.verloren.midnight.type.CompactType;
import org.jetbrains.annotations.NotNull;

public class CompactTypeDefinitionImpl extends CompactNamedElementImpl implements CompactTypeDefinition {
  public CompactTypeDefinitionImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public @NotNull CompactType getType() {
    CompactTypeReferenceImpl typeRef = PsiTreeUtil.findChildOfType(this, CompactTypeReferenceImpl.class);
    if (typeRef != null) {
      return typeRef.getType();
    }
    return CompactPrimitiveType.UNKNOWN;
  }

  @Override
  public void accept(@NotNull CompactVisitor visitor) {
    visitor.visitTypeDefinition(this);
  }
}