package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.type.CompactType;
import org.jetbrains.annotations.NotNull;

public class CompactStructFieldImpl extends CompactNamedElementImpl {
  public CompactStructFieldImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public @NotNull CompactType getType() {
    CompactTypeElement typeElement = PsiTreeUtil.findChildOfType(this, CompactTypeElement.class);
    if (typeElement != null) {
      return typeElement.getType();
    }
    return super.getType();
  }
}