package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.type.CompactPrimitiveType;
import dev.verloren.midnight.type.CompactType;
import org.jetbrains.annotations.NotNull;

public class CompactTypedPatternImpl extends CompactPsiElement implements CompactTypeElement {
  public CompactTypedPatternImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public @NotNull CompactType getType() {
    CompactTypeElement[] typeElements = PsiTreeUtil.getChildrenOfType(this, CompactTypeElement.class);
    if (typeElements != null) {
      for (CompactTypeElement typeElement : typeElements) {
        if (!(typeElement instanceof CompactPatternImpl)) {
          return typeElement.getType();
        }
      }
    }
    return CompactPrimitiveType.UNKNOWN;
  }
}
