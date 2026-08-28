package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;

import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CompactWitnessDeclarationImpl extends CompactNamedElementImpl implements CompactWitnessDeclaration {
  public CompactWitnessDeclarationImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public @NotNull List<CompactNamedElement> getParameters() {
    return CompactPsiUtil.getParameters(this);
  }

  @Override
  public @Nullable CompactTypeElement getReturnTypeElement() {
    CompactTypeElement[] typeElements = PsiTreeUtil.getChildrenOfType(this, CompactTypeElement.class);
    if (typeElements != null && typeElements.length > 0) {
      return typeElements[typeElements.length - 1];
    }
    return PsiTreeUtil.getChildOfType(this, CompactTypeElement.class);
  }

  @Override
  public @NotNull dev.verloren.midnight.type.CompactType getType() {
    CompactTypeElement returnType = getReturnTypeElement();
    if (returnType != null) {
      return returnType.getType();
    }
    return super.getType();
  }

  @Override
  public boolean isExported() {
    return getNode().findChildByType(dev.verloren.midnight.lexer.CompactTokenTypes.EXPORT) != null;
  }

  @Override
  public void accept(@NotNull CompactVisitor visitor) {
    visitor.visitWitnessDeclaration(this);
  }
}

