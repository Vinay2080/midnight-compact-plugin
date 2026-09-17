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

public class CompactTernaryExprImpl extends CompactPsiElement implements CompactTernaryExpr {
  public CompactTernaryExprImpl(@NotNull ASTNode node) {
    super(node);
  }

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
