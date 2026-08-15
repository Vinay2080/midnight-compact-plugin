package dev.verloren.midnight.inspection;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.lexer.CompactTokenSets;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import dev.verloren.midnight.psi.CompactBinaryExprImpl;
import dev.verloren.midnight.psi.CompactExpression;
import dev.verloren.midnight.psi.CompactUnaryExprImpl;
import dev.verloren.midnight.type.CompactPrimitiveType;
import dev.verloren.midnight.type.CompactType;
import org.jetbrains.annotations.NotNull;

public class CompactTypeMismatchInspection extends LocalInspectionTool {

  @Override
  public String getStaticDescription() {
    return "Reports basic type mismatches in Compact expressions.";
  }

  @Override
  public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
    return new PsiElementVisitor() {
      @Override
      public void visitElement(@NotNull PsiElement element) {
        if (element instanceof PsiErrorElement || PsiTreeUtil.getParentOfType(element, PsiErrorElement.class) != null) {
          return;
        }

        if (element instanceof CompactBinaryExprImpl binaryExpr) {
          checkBinaryExpr(binaryExpr, holder);
        } else if (element instanceof CompactUnaryExprImpl unaryExpr) {
          checkUnaryExpr(unaryExpr, holder);
        }
      }
    };
  }

  private static void checkBinaryExpr(@NotNull CompactBinaryExprImpl binaryExpr, @NotNull ProblemsHolder holder) {
    CompactExpression[] children = PsiTreeUtil.getChildrenOfType(binaryExpr, CompactExpression.class);
    if (children == null || children.length < 2) {
      return;
    }

    ASTNode operatorNode = binaryExpr.getNode().findChildByType(CompactTokenSets.OPERATORS);
    if (operatorNode == null) {
      return;
    }

    IElementType operator = operatorNode.getElementType();
    CompactType leftType = children[0].getType();
    CompactType rightType = children[1].getType();

    if (operator == CompactTokenTypes.ANDAND || operator == CompactTokenTypes.OROR) {
      if (!CompactPrimitiveType.UNKNOWN.equals(leftType) && !CompactPrimitiveType.BOOLEAN.equals(leftType)) {
        holder.registerProblem(
            children[0],
            "Boolean expected, got '" + leftType.name() + "'",
            ProblemHighlightType.GENERIC_ERROR_OR_WARNING
        );
      }
      if (!CompactPrimitiveType.UNKNOWN.equals(rightType) && !CompactPrimitiveType.BOOLEAN.equals(rightType)) {
        holder.registerProblem(
            children[1],
            "Boolean expected, got '" + rightType.name() + "'",
            ProblemHighlightType.GENERIC_ERROR_OR_WARNING
        );
      }
    } else if (operator == CompactTokenTypes.EQEQ || operator == CompactTokenTypes.NEQ) {
      if (!CompactPrimitiveType.UNKNOWN.equals(leftType)
          && !CompactPrimitiveType.UNKNOWN.equals(rightType)
          && !leftType.isAssignableTo(rightType)) {
        holder.registerProblem(
            binaryExpr,
            "Cannot compare '" + leftType.name() + "' with '" + rightType.name() + "'",
            ProblemHighlightType.GENERIC_ERROR_OR_WARNING
        );
      }
    }
  }

  private static void checkUnaryExpr(@NotNull CompactUnaryExprImpl unaryExpr, @NotNull ProblemsHolder holder) {
    ASTNode operatorNode = unaryExpr.getNode().findChildByType(CompactTokenSets.OPERATORS);
    if (operatorNode == null || operatorNode.getElementType() != CompactTokenTypes.NOT) {
      return;
    }

    CompactExpression operand = null;
    for (PsiElement child : unaryExpr.getChildren()) {
      if (child instanceof CompactExpression) {
        operand = (CompactExpression) child;
        break;
      }
    }

    if (operand != null) {
      CompactType type = operand.getType();
      if (!CompactPrimitiveType.UNKNOWN.equals(type) && !CompactPrimitiveType.BOOLEAN.equals(type)) {
        holder.registerProblem(
            operand,
            "Boolean expected, got '" + type.name() + "'",
            ProblemHighlightType.GENERIC_ERROR_OR_WARNING
        );
      }
    }
  }
}
