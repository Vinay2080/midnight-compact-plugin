package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CompactLedgerDeclarationImpl extends CompactNamedElementImpl implements CompactLedgerDeclaration {
  public CompactLedgerDeclarationImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public @Nullable CompactTypeElement getTypeElement() {
    return PsiTreeUtil.findChildOfType(this, CompactTypeElement.class);
  }

  @Override
  public @NotNull dev.verloren.midnight.type.CompactType getType() {
    CompactTypeElement typeElement = getTypeElement();
    if (typeElement != null) {
      return typeElement.getType();
    }
    return super.getType();
  }

  @Override
  public boolean isSealed() {
    return getNode().findChildByType(dev.verloren.midnight.lexer.CompactTokenTypes.SEALED) != null;
  }

  @Override
  public boolean isExported() {
    return getNode().findChildByType(dev.verloren.midnight.lexer.CompactTokenTypes.EXPORT) != null;
  }

  @Override
  public void accept(@NotNull CompactVisitor visitor) {
    visitor.visitLedgerDeclaration(this);
  }
}
