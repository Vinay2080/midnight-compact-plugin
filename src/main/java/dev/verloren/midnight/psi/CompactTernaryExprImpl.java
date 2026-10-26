package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import dev.verloren.midnight.type.CompactNumericLiteralType;
import dev.verloren.midnight.type.CompactPrimitiveType;
import dev.verloren.midnight.type.CompactType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * PSI implementation of a ternary conditional expression wrapping an AST composite node.
 *
 * <h3>IntelliJ Platform Lifecycle &amp; Subsystem Role</h3>
 * <p>Instantiated by {@link CompactElementFactory#createElement(ASTNode)} when parsing
 * {@link dev.verloren.midnight.parser.CompactElementTypes#TERNARY_EXPR}.
 * Implements {@link CompactTernaryExpr} by scanning child nodes bounded by {@link CompactTokenTypes#QUESTION}
 * and {@link CompactTokenTypes#COLON} tokens.</p>
 *
 * <h3>Branch Type Unification</h3>
 * <p>Computes {@link #getType()} by inspecting both the 'then' and 'else' branch expressions:
 * <ul>
 *   <li>If one branch is an untyped polymorphic numeric literal ({@link CompactNumericLiteralType}) and the other
 *       is a concrete type, the concrete type is adopted for bidirectional inference.</li>
 *   <li>Otherwise, the first non-{@link CompactPrimitiveType#UNKNOWN} branch type is returned.</li>
 * </ul>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Read Action:</b> Calling {@link #getCondition()}, {@link #getThenBranch()}, {@link #getElseBranch()},
 *       and {@link #getType()} requires an active platform Read Action ({@code @RequiresReadLock}).</li>
 *   <li><b>Write Action:</b> Read-only expression; modifications occur via editor commands.</li>
 * </ul>
 *
 * @see CompactTernaryExpr
 * @see CompactExpression
 */
public class CompactTernaryExprImpl extends CompactPsiElement implements CompactTernaryExpr {
  /**
   * Constructs a ternary expression PSI node wrapping the AST composite node.
   *
   * @param node the AST node representing the ternary expression
   */
  public CompactTernaryExprImpl(@NotNull ASTNode node) {
    super(node);
  }

  /**
   * Scans child nodes located before the {@code ?} token to identify the condition expression.
   *
   * @return the condition {@link CompactExpression}, or {@code null} if absent
   */
  @Override
  public @Nullable CompactExpression getCondition() {
    ASTNode question = getNode().findChildByType(CompactTokenTypes.QUESTION);
    int questionOffset = question != null ? question.getStartOffset() : Integer.MAX_VALUE;
    for (PsiElement child : getChildren()) {
      if (child.getNode().getStartOffset() >= questionOffset) {
        break;
      }
      if (child instanceof CompactExpression expr) {
        return expr;
      }
      CompactExpression descendant = PsiTreeUtil.findChildOfType(child, CompactExpression.class);
      if (descendant != null) {
        return descendant;
      }
    }
    return null;
  }

  /**
   * Scans child nodes located strictly between the {@code ?} and {@code :} tokens to identify
   * the 'then' branch expression.
   *
   * @return the then-branch {@link CompactExpression}, or {@code null} if absent
   */
  @Override
  public @Nullable CompactExpression getThenBranch() {
    ASTNode question = getNode().findChildByType(CompactTokenTypes.QUESTION);
    ASTNode colon = getNode().findChildByType(CompactTokenTypes.COLON);
    if (question == null) {
      return null;
    }
    int start = question.getStartOffset();
    int end = colon != null ? colon.getStartOffset() : Integer.MAX_VALUE;

    for (PsiElement child : getChildren()) {
      int offset = child.getNode().getStartOffset();
      if (offset > start && offset < end) {
        if (child instanceof CompactExpression expr) {
          return expr;
        }
        CompactExpression descendant = PsiTreeUtil.findChildOfType(child, CompactExpression.class);
        if (descendant != null) {
          return descendant;
        }
      }
    }
    return null;
  }

  /**
   * Scans child nodes located strictly after the {@code :} token to identify the 'else' branch expression.
   *
   * @return the else-branch {@link CompactExpression}, or {@code null} if absent
   */
  @Override
  public @Nullable CompactExpression getElseBranch() {
    ASTNode colon = getNode().findChildByType(CompactTokenTypes.COLON);
    if (colon == null) {
      return null;
    }
    int start = colon.getStartOffset();

    for (PsiElement child : getChildren()) {
      int offset = child.getNode().getStartOffset();
      if (offset > start) {
        if (child instanceof CompactExpression expr) {
          return expr;
        }
        CompactExpression descendant = PsiTreeUtil.findChildOfType(child, CompactExpression.class);
        if (descendant != null) {
          return descendant;
        }
      }
    }
    return null;
  }

  /**
   * Computes the unified static {@link CompactType} of this ternary expression.
   *
   * <p>Inspects both branch types:
   * <ul>
   *   <li>If the 'then' branch is an untyped numeric literal and the 'else' branch is concrete,
   *       returns the concrete 'else' type.</li>
   *   <li>Otherwise returns the 'then' branch type if known, or the 'else' branch type if known.</li>
   *   <li>Falls back to {@link CompactPrimitiveType#UNKNOWN}.</li>
   * </ul>
   * </p>
   *
   * @return the inferred branch type or {@link CompactPrimitiveType#UNKNOWN}
   */
  @Override
  public @NotNull CompactType getType() {
    CompactExpression thenBranch = getThenBranch();
    CompactType thenType = thenBranch != null ? thenBranch.getType() : CompactPrimitiveType.UNKNOWN;

    CompactExpression elseBranch = getElseBranch();
    CompactType elseType = elseBranch != null ? elseBranch.getType() : CompactPrimitiveType.UNKNOWN;

    if (thenType instanceof CompactNumericLiteralType && !(elseType instanceof CompactNumericLiteralType) && !CompactPrimitiveType.UNKNOWN.equals(elseType)) {
      return elseType;
    }
    if (!CompactPrimitiveType.UNKNOWN.equals(thenType)) {
      return thenType;
    }
    if (!CompactPrimitiveType.UNKNOWN.equals(elseType)) {
      return elseType;
    }
    return CompactPrimitiveType.UNKNOWN;
  }
}
