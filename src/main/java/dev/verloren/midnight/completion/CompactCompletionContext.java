package dev.verloren.midnight.completion;

import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.lexer.CompactTokenSets;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import dev.verloren.midnight.parser.CompactElementTypes;
import dev.verloren.midnight.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

  public static boolean isComment(@NotNull PsiElement position) {
    if (position instanceof PsiComment) {
      return true;
    }
    if (PsiTreeUtil.getParentOfType(position, PsiComment.class, false) != null) {
      return true;
    }
    return position.getNode() != null && CompactTokenSets.COMMENTS.contains(position.getNode().getElementType());
  }

  public static @Nullable PsiElement prevNonCommentLeaf(@NotNull PsiElement position) {
    for (PsiElement p = PsiTreeUtil.prevVisibleLeaf(position); p != null; p = PsiTreeUtil.prevVisibleLeaf(p)) {
      if (!isComment(p)) {
        return p;
      }
    }
    return null;
  }

  public static @NotNull Kind classify(@NotNull PsiElement position) {
    if (isComment(position)) {
      return Kind.NONE;
    }
    PsiElement previous = prevNonCommentLeaf(position);
    if (previous != null && previous.getNode() != null && previous.getNode().getElementType() == CompactTokenTypes.DOT) {
      return Kind.MEMBER;
    }
    if (PsiTreeUtil.getParentOfType(position, CompactTypeReferenceImpl.class, false) != null) {
      return Kind.TYPE;
    }
    if (isAfterTypeIntro(previous)) {
      return Kind.TYPE;
    }
    if (PsiTreeUtil.getParentOfType(position, CompactStructDefinition.class, false) != null
        || PsiTreeUtil.getParentOfType(position, CompactEnumDefinition.class, false) != null) {
      return Kind.NONE;
    }
    if (isExportPreceding(position)) {
      if (previous != null && previous.getNode() != null) {
        IElementType prevType = previous.getNode().getElementType();
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
      return Kind.AFTER_EXPORT;
    }
    if (isDeclarationOrStatementStart(previous)) {
      if (PsiTreeUtil.getParentOfType(position, CompactBlock.class, false) != null) {
        return Kind.STATEMENT;
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

  public static boolean isExportPreceding(@NotNull PsiElement position) {
    if (isComment(position)) {
      return false;
    }
    // 1. Walk backward through visible non-comment AST leaves until statement/block boundary
    for (PsiElement p = prevNonCommentLeaf(position); p != null; p = prevNonCommentLeaf(p)) {
      if (p.getNode() == null) break;
      IElementType tt = p.getNode().getElementType();
      if (tt == CompactTokenTypes.EXPORT) {
        return true;
      }
      if (tt == CompactTokenTypes.SEMICOLON || tt == CompactTokenTypes.LBRACE || tt == CompactTokenTypes.RBRACE) {
        break;
      }
    }

    // 2. Check document text on the current line before the caret / element
    try {
      com.intellij.openapi.editor.Document doc = position.getContainingFile().getViewProvider().getDocument();
      if (doc != null) {
        int offset = position.getTextRange().getStartOffset();
        int lineStart = doc.getLineStartOffset(doc.getLineNumber(offset));
        CharSequence chars = doc.getCharsSequence();
        return hasPrecedingExportOnLine(chars, lineStart, offset);
      }
    } catch (Exception _) {
    }

    return false;
  }

  public static boolean hasPrecedingExportOnLine(@NotNull CharSequence chars, int lineStart, int offset) {
    int stmtStart = lineStart;
    for (int i = offset - 1; i >= lineStart; i--) {
      char c = chars.charAt(i);
      if (c == ';' || c == '{' || c == '}') {
        stmtStart = i + 1;
        break;
      }
    }
    String stmtBefore = chars.subSequence(stmtStart, offset).toString().trim();
    return stmtBefore.matches(".*\\bexport\\b.*");
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
