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
  public void accept(@NotNull CompactVisitor visitor) {
    visitor.visitLedgerDeclaration(this);
  }
}