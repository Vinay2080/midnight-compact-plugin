package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

public class CompactWitnessDeclarationImpl extends CompactNamedElementImpl implements CompactWitnessDeclaration {
  public CompactWitnessDeclarationImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public void accept(@NotNull CompactVisitor visitor) {
    visitor.visitWitnessDeclaration(this);
  }
}