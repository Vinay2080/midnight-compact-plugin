package dev.verloren.midnight.completion;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import dev.verloren.midnight.psi.CompactStructDefinition;
import dev.verloren.midnight.psi.CompactTypeDefinition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Classifier helper detecting type annotation positions and parameterized type size arguments.
 */
public final class CompactCompletionTypeClassifier {

  private CompactCompletionTypeClassifier() {}

  /**
   * Inspects preceding tokens to detect whether the cursor is located within the generic size parameter
   * of a {@code Bytes<...>} or {@code Uint<...>} type invocation.
   *
   * @param previous the nearest preceding non-comment leaf
   * @return {@link CompactCompletionContext.Kind#BYTES_SIZE}, {@link CompactCompletionContext.Kind#UINT_SIZE}, or null
   */
  public static @Nullable CompactCompletionContext.Kind checkParameterizedTypeSizeContext(@Nullable PsiElement previous) {
    if (previous == null || previous.getNode() == null) {
      return null;
    }
    PsiElement curr = previous;
    while (curr != null && curr.getNode() != null) {
      IElementType type = curr.getNode().getElementType();
      if (type == CompactTokenTypes.LT) {
        PsiElement beforeLt = CompactCompletionContext.prevNonCommentLeaf(curr);
        if (beforeLt != null) {
          String name = beforeLt.getText();
          if ("Bytes".equals(name)) {
            return CompactCompletionContext.Kind.BYTES_SIZE;
          }
          if ("Uint".equals(name)) {
            return CompactCompletionContext.Kind.UINT_SIZE;
          }
        }
        return null;
      }
      if (type == CompactTokenTypes.COMMA || type == CompactTokenTypes.GT
          || type == CompactTokenTypes.SEMICOLON || type == CompactTokenTypes.LBRACE || type == CompactTokenTypes.RBRACE
          || type == CompactTokenTypes.HASH) {
        return null;
      }
      curr = CompactCompletionContext.prevNonCommentLeaf(curr);
    }
    return null;
  }

  /**
   * Checks whether the preceding leaf is a type introduction token.
   *
   * @param previous nearest preceding non-comment leaf
   * @return true if following a type introducer
   */
  public static boolean isAfterTypeIntro(@Nullable PsiElement previous) {
    if (previous == null || previous.getNode() == null) {
      return false;
    }
    IElementType type = previous.getNode().getElementType();
    if (type == CompactTokenTypes.AS
            || type == CompactTokenTypes.LT
            || type == CompactTokenTypes.HASH) {
      return true;
    }
    if (type == CompactTokenTypes.COLON) {
      return isTypeColon(previous);
    }
    return false;
  }

  /**
   * Disambiguates whether a colon token introduces a type annotation vs a struct literal field value.
   *
   * @param colonLeaf the colon token leaf element
   * @return true if the colon represents a type annotation position
   */
  public static boolean isTypeColon(@NotNull PsiElement colonLeaf) {
    PsiElement beforeColon = CompactCompletionContext.prevNonCommentLeaf(colonLeaf);
    if (beforeColon == null || beforeColon.getNode() == null) {
      return false;
    }
    IElementType beforeType = beforeColon.getNode().getElementType();
    if (beforeType == CompactTokenTypes.RPAREN) {
      return true;
    }
    if (beforeType == CompactTokenTypes.IDENTIFIER) {
      PsiElement beforeId = CompactCompletionContext.prevNonCommentLeaf(beforeColon);
      if (beforeId != null && beforeId.getNode() != null) {
        IElementType beforeIdType = beforeId.getNode().getElementType();
        if (beforeIdType == CompactTokenTypes.LEDGER || beforeIdType == CompactTokenTypes.LPAREN || beforeIdType == CompactTokenTypes.CONST) {
          return true;
        }
        if (beforeIdType == CompactTokenTypes.COMMA) {
          for (PsiElement p = beforeId; p != null; p = CompactCompletionContext.prevNonCommentLeaf(p)) {
            if (p.getNode() == null) break;
            IElementType t = p.getNode().getElementType();
            if (t == CompactTokenTypes.LPAREN) {
              return true;
            }
            if (t == CompactTokenTypes.LBRACE || t == CompactTokenTypes.SEMICOLON) {
              break;
            }
          }
        }
      }
      if (PsiTreeUtil.getParentOfType(colonLeaf, CompactStructDefinition.class, false) != null) {
        return true;
      }
    }
    return false;
  }

  /**
   * Tests whether the current position sits within an explicit type definition rhs or a comma-separated
   * type parameter list.
   *
   * @param position current caret element
   * @param previous nearest preceding non-comment leaf
   * @return true if positioned inside a type slot
   */
  public static boolean isTypePosition(@NotNull PsiElement position, @Nullable PsiElement previous) {
    CompactTypeDefinition typeDef = PsiTreeUtil.getParentOfType(position, CompactTypeDefinition.class, false);
    if (typeDef != null) {
      ASTNode eqNode = typeDef.getNode().findChildByType(CompactTokenTypes.ASSIGN);
      if (eqNode != null && position.getTextRange().getStartOffset() >= eqNode.getTextRange().getEndOffset()) {
        return true;
      }
    }
    if (previous != null && previous.getNode() != null && previous.getNode().getElementType() == CompactTokenTypes.COMMA) {
      for (PsiElement p = previous; p != null; p = PsiTreeUtil.prevVisibleLeaf(p)) {
        if (p.getNode() == null) break;
        IElementType t = p.getNode().getElementType();
        if (t == CompactTokenTypes.LT) {
          return true;
        }
        if (t == CompactTokenTypes.GT || t == CompactTokenTypes.SEMICOLON || t == CompactTokenTypes.LBRACE) {
          break;
        }
      }
    }
    return false;
  }
}
