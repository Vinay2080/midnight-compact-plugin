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
import dev.verloren.midnight.parser.CompactElementTypes;
import dev.verloren.midnight.psi.*;
import dev.verloren.midnight.type.CompactPrimitiveType;
import dev.verloren.midnight.type.CompactType;
import org.jetbrains.annotations.NotNull;

public class CompactTypeMismatchInspection extends LocalInspectionTool {

  @Override
  public String getStaticDescription() {
    return "Reports basic type mismatches in Compact expressions, conditions, and declarations.";
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
        } else if (element.getNode().getElementType() == CompactElementTypes.IF_STATEMENT) {
          checkIfStatement(element, holder);
        } else if (element instanceof CompactConstBindingImpl constBinding) {
          checkConstBinding(constBinding, holder);
        }
      }
    };
  }

  private static void checkIfStatement(@NotNull PsiElement ifStatement, @NotNull ProblemsHolder holder) {
    CompactExpression condition = PsiTreeUtil.findChildOfType(ifStatement, CompactExpression.class);
    if (condition == null) {
      return;
    }

    CompactBlock block = PsiTreeUtil.findChildOfType(ifStatement, CompactBlock.class);
    if (PsiTreeUtil.isAncestor(block, condition, false)) {
      return;
    }

    CompactType condType = condition.getType();
    if (!CompactPrimitiveType.UNKNOWN.equals(condType) && !CompactPrimitiveType.BOOLEAN.equals(condType)) {
      holder.registerProblem(
          condition,
          "Boolean expected in 'if' condition, got '" + condType.name() + "'",
          ProblemHighlightType.GENERIC_ERROR_OR_WARNING
      );
    }
  }

  private static void checkConstBinding(@NotNull CompactConstBindingImpl constBinding, @NotNull ProblemsHolder holder) {
    CompactTypeElement declaredTypeElement = null;
    for (PsiElement child : constBinding.getChildren()) {
      if (child.getNode().getElementType() == CompactElementTypes.OPTIONALLY_TYPED_PATTERN
          || child.getNode().getElementType() == CompactElementTypes.TYPED_PATTERN) {
        for (PsiElement sub : child.getChildren()) {
          if (sub instanceof CompactTypeElement && !(sub instanceof CompactPatternImpl)) {
            declaredTypeElement = (CompactTypeElement) sub;
            break;
          }
        }
      }
    }

    CompactExpression initializer = constBinding.getInitializer();
    if (declaredTypeElement == null || initializer == null) {
      return;
    }

    CompactType declaredType = declaredTypeElement.getType();
    CompactType initType = initializer.getType();

    if (!CompactPrimitiveType.UNKNOWN.equals(declaredType)
        && !CompactPrimitiveType.UNKNOWN.equals(initType)
        && !initType.isAssignableTo(declaredType)) {
      holder.registerProblem(
          initializer,
          "Type mismatch: expected '" + declaredType.name() + "', got '" + initType.name() + "'",
          ProblemHighlightType.GENERIC_ERROR_OR_WARNING
      );
    }
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
    } else if (operator == CompactTokenTypes.LT || operator == CompactTokenTypes.LTE ||
               operator == CompactTokenTypes.GT || operator == CompactTokenTypes.GTE) {
      if (CompactPrimitiveType.BOOLEAN.equals(leftType)) {
        holder.registerProblem(
            children[0],
            "Relational operator not applicable to 'Boolean'",
            ProblemHighlightType.GENERIC_ERROR_OR_WARNING
        );
      }
      if (CompactPrimitiveType.BOOLEAN.equals(rightType)) {
        holder.registerProblem(
            children[1],
            "Relational operator not applicable to 'Boolean'",
            ProblemHighlightType.GENERIC_ERROR_OR_WARNING
        );
      }
      if (!CompactPrimitiveType.UNKNOWN.equals(leftType)
          && !CompactPrimitiveType.UNKNOWN.equals(rightType)
          && !CompactPrimitiveType.BOOLEAN.equals(leftType)
          && !CompactPrimitiveType.BOOLEAN.equals(rightType)
          && !leftType.isAssignableTo(rightType)) {
        holder.registerProblem(
            binaryExpr,
            "Cannot compare '" + leftType.name() + "' with '" + rightType.name() + "'",
            ProblemHighlightType.GENERIC_ERROR_OR_WARNING
        );
      }
    } else if (operator == CompactTokenTypes.PLUS || operator == CompactTokenTypes.MINUS ||
               operator == CompactTokenTypes.STAR || operator == CompactTokenTypes.SLASH ||
               operator == CompactTokenTypes.PERCENT) {
      if (CompactPrimitiveType.BOOLEAN.equals(leftType)) {
        holder.registerProblem(
            children[0],
            "Arithmetic operator not applicable to 'Boolean'",
            ProblemHighlightType.GENERIC_ERROR_OR_WARNING
        );
      }
      if (CompactPrimitiveType.BOOLEAN.equals(rightType)) {
        holder.registerProblem(
            children[1],
            "Arithmetic operator not applicable to 'Boolean'",
            ProblemHighlightType.GENERIC_ERROR_OR_WARNING
        );
      }
    }
  }

  private static void checkUnaryExpr(@NotNull CompactUnaryExprImpl unaryExpr, @NotNull ProblemsHolder holder) {
    ASTNode operatorNode = unaryExpr.getNode().findChildByType(CompactTokenSets.OPERATORS);
    if (operatorNode == null) {
      return;
    }

    IElementType operator = operatorNode.getElementType();
    CompactExpression operand = null;
    for (PsiElement child : unaryExpr.getChildren()) {
      if (child instanceof CompactExpression) {
        operand = (CompactExpression) child;
        break;
      }
    }

    if (operand == null) {
      return;
    }

    CompactType type = operand.getType();
    if (operator == CompactTokenTypes.NOT) {
      if (!CompactPrimitiveType.UNKNOWN.equals(type) && !CompactPrimitiveType.BOOLEAN.equals(type)) {
        holder.registerProblem(
            operand,
            "Boolean expected, got '" + type.name() + "'",
            ProblemHighlightType.GENERIC_ERROR_OR_WARNING
        );
      }
    } else if (operator == CompactTokenTypes.MINUS) {
      if (CompactPrimitiveType.BOOLEAN.equals(type)) {
        holder.registerProblem(
            operand,
            "Unary minus not applicable to 'Boolean'",
            ProblemHighlightType.GENERIC_ERROR_OR_WARNING
        );
      }
    }
  }
}
