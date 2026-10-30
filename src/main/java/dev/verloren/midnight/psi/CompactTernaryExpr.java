package dev.verloren.midnight.psi;

import org.jetbrains.annotations.Nullable;

/**
 * PSI representation of a ternary conditional expression ({@code condition ? thenBranch : elseBranch}).
 *
 * <h3>IntelliJ Platform Lifecycle &amp; Subsystem Role</h3>
 * <p>Exposes typed access to the three partitions of a conditional expression:
 * <ul>
 *   <li>The condition expression preceding the {@code ?} operator.</li>
 *   <li>The 'then' branch expression between {@code ?} and {@code :}.</li>
 *   <li>The 'else' branch expression following the {@code :} operator.</li>
 * </ul>
 * Enables type inference and flow-sensitive inspections to analyse both evaluation branches.</p>
 *
 * <h3>Calling Pipeline</h3>
 * <p>Invoked across IDE analysis components:
 * <ul>
 *   <li>{@link dev.verloren.midnight.type.CompactTypeInferenceUtil} during expression typing and branch unification.</li>
 *   <li>{@link dev.verloren.midnight.inspection.CompactTypeMismatchInspection} to verify that the condition is boolean and branch types unify.</li>
 * </ul>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Read Action:</b> Calling all query methods requires an active platform Read Action ({@code @RequiresReadLock}).</li>
 *   <li><b>Write Action:</b> Read-only interface; modifications occur via editor commands.</li>
 * </ul>
 *
 * @see CompactExpression
 * @see CompactTernaryExprImpl
 */
public interface CompactTernaryExpr extends CompactExpression {
  /**
   * Returns the condition expression preceding the {@code ?} operator.
   *
   * @return the condition {@link CompactExpression}, or {@code null} if missing or malformed
   */
  @Nullable CompactExpression getCondition();

  /**
   * Returns the expression evaluated when the condition is true (between {@code ?} and {@code :}).
   *
   * @return the then-branch {@link CompactExpression}, or {@code null} if missing or malformed
   */
  @Nullable CompactExpression getThenBranch();

  /**
   * Returns the expression evaluated when the condition is false (after {@code :}).
   *
   * @return the else-branch {@link CompactExpression}, or {@code null} if missing or malformed
   */
  @Nullable CompactExpression getElseBranch();
}
