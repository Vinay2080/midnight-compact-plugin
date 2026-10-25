package dev.verloren.midnight.intention;

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import dev.verloren.midnight.parser.CompactElementTypes;
import dev.verloren.midnight.psi.CompactBinaryExprImpl;
import dev.verloren.midnight.psi.CompactExpression;
import dev.verloren.midnight.psi.CompactUnaryExprImpl;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * In-editor intention action to invert an {@code if} condition and swap its {@code then} and {@code else} branches.
 *
 * <h3>IntelliJ Platform Subsystem Role & Calling Pipeline</h3>
 * <p>
 * Registered under {@code <intentionAction>} in {@code plugin.xml}:
 * <ul>
 *   <li><b>Availability Context:</b> Available in the Alt+Enter context actions popup when the caret
 *       is positioned anywhere on the {@code if} condition header line before descending into body statements.</li>
 *   <li><b>Logical & Relational Inversion:</b> Automatically inverts relational operators
 *       ({@code ==} &harr; {@code !=}, {@code <} &harr; {@code >=}, {@code <=} &harr; {@code >}),
 *       unwraps negated unary expressions ({@code !expr} &harr; {@code expr}), and wraps general expressions
 *       in {@code !(...) }.</li>
 *   <li><b>Safe Offset Replacement:</b> When mutating the document, replaces the {@code else} branch first,
 *       followed by the {@code then} branch, and finally the condition. This descending text offset order
 *       ensures that document modifications do not displace or invalidate upcoming replace ranges.</li>
 * </ul>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Read Action for Availability:</b> {@link #isAvailable} runs under a Read Action ({@code @RequiresReadLock})
 *       on the EDT during lightbulb / popup evaluation.</li>
 *   <li><b>Write Action for Invocation:</b> {@link #invoke} mutates the editor {@link Document} on the EDT
 *       inside the platform's default write action.</li>
 * </ul>
 *
 * <h3>Condition Inversion Matrix</h3>
 * <pre>
 *   Original Expression       Inverted Expression
 *   ──────────────────────────────────────────────────
 *   a == b                    a != b
 *   a != b                    a == b
 *   a &lt; b                     a &gt;= b
 *   a &lt;= b                    a &gt; b
 *   a &gt; b                     a &lt;= b
 *   a &gt;= b                    a &lt; b
 *   !a                        a
 *   (cond)                    !(cond)
 * </pre>
 *
 * @see dev.verloren.midnight.parser.CompactElementTypes#IF_STATEMENT
 */
public class CompactInvertIfIntention extends PsiElementBaseIntentionAction {

  @Override
  public @NotNull @Nls(capitalization = Nls.Capitalization.Sentence) String getFamilyName() {
    return "Compact control flow";
  }

  /**
   * Helper traversing parent nodes to find the enclosing IF_STATEMENT element.
   *
   * @param element the PSI element at the caret
   * @return enclosing if statement node, or {@code null}
   */
  @Nullable
  private PsiElement findIfStatement(@NotNull PsiElement element) {
    PsiElement current = element;
    while (current != null) {
      if (current.getNode() != null && current.getNode().getElementType() == CompactElementTypes.IF_STATEMENT) {
        return current;
      }
      current = current.getParent();
    }
    return null;
  }

  /**
   * Checks whether the intention is applicable at the current editor position.
   */
  @Override
  public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
    // Step 1: Ensure caret is inside an if statement
    PsiElement ifStmt = findIfStatement(element);
    if (ifStmt == null) {
      return false;
    }

    // Step 2: Ensure if statement has both condition, then-branch, and else-branch
    IfParts parts = parseIfParts(ifStmt);
    if (parts.condition == null || parts.thenBranch == null || parts.elseBranch == null) {
      return false;
    }

    // Step 3: Bound availability to the if-header line (suppress inside nested then-body lines)
    Document doc = editor.getDocument();
    int caretLine = doc.getLineNumber(element.getTextOffset());
    int thenLine = doc.getLineNumber(parts.thenBranch.getTextOffset());
    if (caretLine > thenLine) {
      return false;
    }

    setText("Invert 'if' condition");
    return true;
  }

  /**
   * Performs the condition inversion and branch swapping.
   */
  @Override
  public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element) throws IncorrectOperationException {
    PsiElement ifStmt = findIfStatement(element);
    if (ifStmt == null) {
      return;
    }

    IfParts parts = parseIfParts(ifStmt);
    if (parts.condition == null || parts.thenBranch == null || parts.elseBranch == null) {
      return;
    }

    Document document = editor.getDocument();
    String invertedCondition = invertConditionText(parts.condition);
    String thenText = parts.thenBranch.getText();
    String elseText = parts.elseBranch.getText();

    // Step 1: Replace else branch first (at higher text offset) to prevent shifting then-branch offset
    TextRange elseRange = parts.elseBranch.getTextRange();
    document.replaceString(elseRange.getStartOffset(), elseRange.getEndOffset(), thenText);

    // Step 2: Replace then branch next
    TextRange thenRange = parts.thenBranch.getTextRange();
    document.replaceString(thenRange.getStartOffset(), thenRange.getEndOffset(), elseText);

    // Step 3: Replace condition at lowest text offset
    TextRange condRange = parts.condition.getTextRange();
    document.replaceString(condRange.getStartOffset(), condRange.getEndOffset(), invertedCondition);
  }

  /**
   * Mathematically inverts a condition expression string.
   */
  private static String invertConditionText(@NotNull PsiElement condition) {
    CompactExpression expr = condition instanceof CompactExpression
        ? (CompactExpression) condition
        : PsiTreeUtil.findChildOfType(condition, CompactExpression.class);

    // Case 1: Invert binary comparison operators
    if (expr instanceof CompactBinaryExprImpl binaryExpr) {
      CompactExpression[] operands = PsiTreeUtil.getChildrenOfType(binaryExpr, CompactExpression.class);
      ASTNode opNode = binaryExpr.getNode().findChildByType(dev.verloren.midnight.lexer.CompactTokenSets.OPERATORS);
      if (operands != null && operands.length >= 2 && opNode != null) {
        IElementType op = opNode.getElementType();
        String left = operands[0].getText();
        String right = operands[1].getText();
        if (op == CompactTokenTypes.EQEQ) return left + " != " + right;
        if (op == CompactTokenTypes.NEQ) return left + " == " + right;
        if (op == CompactTokenTypes.LT) return left + " >= " + right;
        if (op == CompactTokenTypes.LTE) return left + " > " + right;
        if (op == CompactTokenTypes.GT) return left + " <= " + right;
        if (op == CompactTokenTypes.GTE) return left + " < " + right;
      }
    } else if (expr instanceof CompactUnaryExprImpl unaryExpr) {
      // Case 2: Unwrap negated expressions (!expr -> expr)
      ASTNode opNode = unaryExpr.getNode().findChildByType(dev.verloren.midnight.lexer.CompactTokenSets.OPERATORS);
      if (opNode != null && opNode.getElementType() == CompactTokenTypes.NOT) {
        CompactExpression inner = PsiTreeUtil.findChildOfType(unaryExpr, CompactExpression.class);
        if (inner != null) {
          return inner.getText();
        }
      }
    }

    // Case 3: Wrap general expressions in ! negation
    String text = condition.getText().trim();
    if (text.startsWith("(") && text.endsWith(")")) {
      return "!" + text;
    }
    return "!(" + text + ")";
  }

  /**
   * Value container holding parsed sub-elements of an if-else statement.
   */
  private static class IfParts {
    PsiElement condition;
    PsiElement thenBranch;
    PsiElement elseBranch;
  }

  /**
   * Parses the constituent condition, then-branch, and else-branch components of an if statement.
   */
  @NotNull
  private static IfParts parseIfParts(@NotNull PsiElement ifStmt) {
    IfParts parts = new IfParts();
    boolean seenLParen = false;
    boolean seenRParen = false;
    boolean seenElse = false;

    for (PsiElement child = ifStmt.getFirstChild(); child != null; child = child.getNextSibling()) {
      if (child instanceof PsiWhiteSpace) {
        continue;
      }
      IElementType type = child.getNode() != null ? child.getNode().getElementType() : null;
      if (type == CompactTokenTypes.LPAREN) {
        seenLParen = true;
        continue;
      }
      if (type == CompactTokenTypes.RPAREN) {
        seenRParen = true;
        continue;
      }
      if (type == CompactTokenTypes.ELSE) {
        seenElse = true;
        continue;
      }

      if (seenLParen && !seenRParen) {
        parts.condition = child;
      } else if (seenRParen && !seenElse) {
        parts.thenBranch = child;
      } else if (seenElse) {
        parts.elseBranch = child;
      }
    }

    return parts;
  }
}
