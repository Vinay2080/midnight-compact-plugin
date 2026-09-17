package dev.verloren.midnight.psi;

import org.jetbrains.annotations.Nullable;

/**
 * PSI representation of a ternary conditional expression ({@code condition ? thenBranch : elseBranch}).
 */
public interface CompactTernaryExpr extends CompactExpression {
  @Nullable CompactExpression getCondition();

  @Nullable CompactExpression getThenBranch();

  @Nullable CompactExpression getElseBranch();
}
