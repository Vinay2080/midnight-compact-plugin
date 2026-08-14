package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.type.CompactType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CompactStructFieldImpl extends CompactNamedElementImpl {
  public CompactStructFieldImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public @Nullable com.intellij.psi.PsiElement getNameIdentifier() {
    com.intellij.psi.PsiElement direct = super.getNameIdentifier();
    if (direct != null) {
      return direct;
    }
    CompactParameterImpl typedId = PsiTreeUtil.findChildOfType(this, CompactParameterImpl.class);
    if (typedId != null) {
      return typedId.getNameIdentifier();
    }
    ASTNode idNode = getNode().findChildByType(dev.verloren.midnight.lexer.CompactTokenTypes.IDENTIFIER);
    return idNode != null ? idNode.getPsi() : null;
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