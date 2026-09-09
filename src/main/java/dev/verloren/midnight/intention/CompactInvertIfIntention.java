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
 * In-editor intention to invert an {@code if} condition and swap its {@code then} and {@code else} branches.
 *
 * <p>Available anywhere on the {@code if} statement header line.</p>
 */
public class CompactInvertIfIntention extends PsiElementBaseIntentionAction {

  @Override
  public @NotNull @Nls(capitalization = Nls.Capitalization.Sentence) String getFamilyName() {
    return "Compact control flow";
  }

  @Override
  public boolean startInWriteAction() {
    return true;
  }

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

  @Override
  public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
    PsiElement ifStmt = findIfStatement(element);
    if (ifStmt == null) {
      return false;
    }

    IfParts parts = parseIfParts(ifStmt);
    if (parts == null || parts.condition == null || parts.thenBranch == null || parts.elseBranch == null) {
      return false;
    }

    // Available anywhere on the if condition header line before descending into then-branch lines
    Document doc = editor.getDocument();
    int caretLine = doc.getLineNumber(element.getTextOffset());
    int thenLine = doc.getLineNumber(parts.thenBranch.getTextOffset());
    if (caretLine > thenLine) {
      return false;
    }

    setText("Invert 'if' condition");
    return true;
  }

  @Override
  public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element) throws IncorrectOperationException {
    PsiElement ifStmt = findIfStatement(element);
    if (ifStmt == null) {
      return;
    }

    IfParts parts = parseIfParts(ifStmt);
    if (parts == null || parts.condition == null || parts.thenBranch == null || parts.elseBranch == null) {
      return;
    }

    Document document = editor.getDocument();
    String invertedCondition = invertConditionText(parts.condition);
    String thenText = parts.thenBranch.getText();
    String elseText = parts.elseBranch.getText();

    // Replace else branch first (higher offset)
    TextRange elseRange = parts.elseBranch.getTextRange();
    document.replaceString(elseRange.getStartOffset(), elseRange.getEndOffset(), thenText);

    // Replace then branch next
    TextRange thenRange = parts.thenBranch.getTextRange();
    document.replaceString(thenRange.getStartOffset(), thenRange.getEndOffset(), elseText);

    // Replace condition
    TextRange condRange = parts.condition.getTextRange();
    document.replaceString(condRange.getStartOffset(), condRange.getEndOffset(), invertedCondition);
  }

  private static String invertConditionText(@NotNull PsiElement condition) {
    CompactExpression expr = condition instanceof CompactExpression
        ? (CompactExpression) condition
        : PsiTreeUtil.findChildOfType(condition, CompactExpression.class);

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
      ASTNode opNode = unaryExpr.getNode().findChildByType(dev.verloren.midnight.lexer.CompactTokenSets.OPERATORS);
      if (opNode != null && opNode.getElementType() == CompactTokenTypes.NOT) {
        CompactExpression inner = PsiTreeUtil.findChildOfType(unaryExpr, CompactExpression.class);
        if (inner != null) {
          return inner.getText();
        }
      }
    }

    String text = condition.getText().trim();
    if (text.startsWith("(") && text.endsWith(")")) {
      return "!" + text;
    }
    return "!(" + text + ")";
  }

  private static class IfParts {
    PsiElement condition;
    PsiElement thenBranch;
    PsiElement elseBranch;
  }

  @Nullable
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
