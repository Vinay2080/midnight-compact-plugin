package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import org.jetbrains.annotations.NotNull;

/**
 * PSI implementation of {@link CompactExternalCircuit}.
 */
public class CompactExternalCircuitImpl extends CompactNamedElementImpl implements CompactExternalCircuit {

  public CompactExternalCircuitImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public boolean isPure() {
    return getNode().findChildByType(CompactTokenTypes.PURE) != null;
  }

  @Override
  public void accept(@NotNull CompactVisitor visitor) {
    visitor.visitExternalCircuit(this);
  }
}
