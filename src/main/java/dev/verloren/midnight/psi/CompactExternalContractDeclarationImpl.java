package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

public class CompactExternalContractDeclarationImpl extends CompactNamedElementImpl implements CompactExternalContractDeclaration {
  public CompactExternalContractDeclarationImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public void accept(@NotNull CompactVisitor visitor) {
    visitor.visitExternalContractDeclaration(this);
  }
}