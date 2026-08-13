package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

public class CompactLedgerDeclarationImpl extends CompactNamedElementImpl implements CompactLedgerDeclaration {
  public CompactLedgerDeclarationImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public void accept(@NotNull CompactVisitor visitor) {
    visitor.visitLedgerDeclaration(this);
  }
}