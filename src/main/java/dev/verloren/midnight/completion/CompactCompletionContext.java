package dev.verloren.midnight.completion;

import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import dev.verloren.midnight.parser.CompactElementTypes;
import dev.verloren.midnight.psi.CompactBlock;
import dev.verloren.midnight.psi.CompactEnumDefinition;
import dev.verloren.midnight.psi.CompactMemberExprImpl;
import dev.verloren.midnight.psi.CompactStructDefinition;
import dev.verloren.midnight.psi.CompactTypeReferenceImpl;
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
    return previous.getNode().getElementType() == CompactTokenTypes.COLON
            || previous.getNode().getElementType() == CompactTokenTypes.AS
            || previous.getNode().getElementType() == CompactTokenTypes.LT
            || previous.getNode().getElementType() == CompactTokenTypes.HASH;
  }

  private static boolean isDeclarationOrStatementStart(PsiElement previous) {
    if (previous == null || previous.getNode() == null) {
      return true;
    }
    return previous.getNode().getElementType() == CompactTokenTypes.SEMICOLON
            || previous.getNode().getElementType() == CompactTokenTypes.LBRACE
            || previous.getNode().getElementType() == CompactTokenTypes.RBRACE
            || previous.getNode().getElementType() == CompactTokenTypes.ELSE
            || previous.getNode().getElementType() == CompactTokenTypes.EXPORT
            || previous.getNode().getElementType() == CompactTokenTypes.PURE
            || previous.getNode().getElementType() == CompactTokenTypes.SEALED
            || previous.getNode().getElementType() == CompactElementTypes.BLOCK;
  }

  public enum Kind {
    KEYWORD,
    STATEMENT,
    TYPE,
    VALUE,
    MEMBER,
    NONE
  }
}
