package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CompactExternalContractDeclarationImpl extends CompactNamedElementImpl implements CompactExternalContractDeclaration {
  public CompactExternalContractDeclarationImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public @NotNull List<CompactExternalCircuit> getCircuits() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, CompactExternalCircuit.class);
  }

  @Override
  public void accept(@NotNull CompactVisitor visitor) {
    visitor.visitExternalContractDeclaration(this);
  }
}
