package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.type.CompactPrimitiveType;
import dev.verloren.midnight.type.CompactType;
import org.jetbrains.annotations.NotNull;

public class CompactEnumMemberImpl extends CompactNamedElementImpl {
  public CompactEnumMemberImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public @NotNull CompactType getType() {
    CompactEnumDefinition parent = PsiTreeUtil.getParentOfType(this, CompactEnumDefinition.class);
    if (parent != null) {
      return parent.getType();
    }
    return CompactPrimitiveType.UNKNOWN;
  }
}