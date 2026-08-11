package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

public class CompactEnumMemberImpl extends CompactNamedElementImpl {
  public CompactEnumMemberImpl(@NotNull ASTNode node) {
    super(node);
  }
}