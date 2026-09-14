package dev.verloren.midnight.completion;

import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import dev.verloren.midnight.parser.CompactElementTypes;
import dev.verloren.midnight.psi.*;
import org.jetbrains.annotations.NotNull;

/**
 * Context classifier determining the semantic role of the cursor position during code completion.
 *
 * <p>Inspects preceding AST tokens and containing PSI elements to distinguish between
 * top-level declaration keywords, block statements, type positions (after {@code :}, {@code as}, {@code <}),
 * member access (after {@code .}), and general value expressions.</p>
 */
public final class CompactCompletionContext {
  private CompactCompletionContext() {
  }

  public static @NotNull Kind classify(@NotNull PsiElement position) {
    PsiElement previous = PsiTreeUtil.prevVisibleLeaf(position);
    if (previous != null && previous.getNode() != null && previous.getNode().getElementType() == CompactTokenTypes.DOT) {
      return Kind.MEMBER;
    }
    if (PsiTreeUtil.getParentOfType(position, CompactTypeReferenceImpl.class, false) != null) {
      return Kind.TYPE;
    }
    if (isAfterTypeIntro(previous)) {
      return Kind.TYPE;
    }
    if (isDeclarationOrStatementStart(previous)) {
      if (PsiTreeUtil.getParentOfType(position, CompactBlock.class, false) != null) {
        return Kind.STATEMENT;
      }
      if (PsiTreeUtil.getParentOfType(position, CompactStructDefinition.class, false) != null
          || PsiTreeUtil.getParentOfType(position, CompactEnumDefinition.class, false) != null) {
        return Kind.NONE;
      }
      if (previous != null && previous.getNode() != null) {
        IElementType prevType = previous.getNode().getElementType();
        if (prevType == CompactTokenTypes.EXPORT) {
          return Kind.AFTER_EXPORT;
        }
        if (prevType == CompactTokenTypes.SEALED) {
          return Kind.AFTER_SEALED;
        }
        if (prevType == CompactTokenTypes.PURE) {
          return Kind.AFTER_PURE;
        }
        if (prevType == CompactTokenTypes.NEW) {
          return Kind.AFTER_NEW;
        }
      }
      return Kind.KEYWORD;
    }
    if (PsiTreeUtil.getParentOfType(position, CompactMemberExprImpl.class, false) != null) {
      return Kind.MEMBER;
    }
    return Kind.VALUE;
  }

  private static boolean isAfterTypeIntro(PsiElement previous) {
    if (previous == null || previous.getNode() == null) {
      return false;
    }
    IElementType type = previous.getNode().getElementType();
    return type == CompactTokenTypes.COLON
            || type == CompactTokenTypes.AS
            || type == CompactTokenTypes.LT
            || type == CompactTokenTypes.HASH;
  }

  private static boolean isDeclarationOrStatementStart(PsiElement previous) {
    if (previous == null || previous.getNode() == null) {
      return true;
    }
    IElementType type = previous.getNode().getElementType();
    return type == CompactTokenTypes.SEMICOLON
            || type == CompactTokenTypes.LBRACE
            || type == CompactTokenTypes.RBRACE
            || type == CompactTokenTypes.ELSE
            || type == CompactTokenTypes.EXPORT
            || type == CompactTokenTypes.PURE
            || type == CompactTokenTypes.SEALED
            || type == CompactTokenTypes.NEW
            || type == CompactElementTypes.BLOCK;
  }

  public enum Kind {
    KEYWORD,
    AFTER_EXPORT,
    AFTER_SEALED,
    AFTER_PURE,
    AFTER_NEW,
    STATEMENT,
    TYPE,
    VALUE,
    MEMBER,
    NONE
  }
}
