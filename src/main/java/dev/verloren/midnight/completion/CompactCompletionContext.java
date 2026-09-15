package dev.verloren.midnight.completion;

import com.intellij.lang.ASTNode;
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
    if (isTypePosition(position, previous)) {
      return Kind.TYPE;
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
    if (PsiTreeUtil.getParentOfType(position, CompactStructDefinition.class, false) != null
        || PsiTreeUtil.getParentOfType(position, CompactEnumDefinition.class, false) != null) {
      return Kind.NONE;
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
    // 1. Walk backward through visible AST leaves until statement/block boundary or declaration keyword
    for (PsiElement p = PsiTreeUtil.prevVisibleLeaf(position); p != null; p = PsiTreeUtil.prevVisibleLeaf(p)) {
      if (p.getNode() == null) break;
      IElementType tt = p.getNode().getElementType();
      if (tt == CompactTokenTypes.EXPORT) {
        return true;
      }
      if (tt == CompactTokenTypes.SEMICOLON || tt == CompactTokenTypes.LBRACE || tt == CompactTokenTypes.RBRACE
          || tt == CompactTokenTypes.COLON || tt == CompactTokenTypes.ASSIGN
          || tt == CompactTokenTypes.LPAREN || tt == CompactTokenTypes.RPAREN
          || tt == CompactTokenTypes.COMMA
          || tt == CompactTokenTypes.LEDGER || tt == CompactTokenTypes.CIRCUIT
          || tt == CompactTokenTypes.WITNESS || tt == CompactTokenTypes.STRUCT
          || tt == CompactTokenTypes.ENUM || tt == CompactTokenTypes.TYPE
          || tt == CompactTokenTypes.MODULE || tt == CompactTokenTypes.CONTRACT) {
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
    } catch (Exception ignored) {
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
    return stmtBefore.matches("^export(\\s+(sealed|pure|new))?(\\s+\\w*)?$");
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

  private static boolean isTypePosition(@NotNull PsiElement position, PsiElement previous) {
    // 1. Inside ledger declaration type slot: ledger foo: <caret>
    CompactLedgerDeclaration ledger = PsiTreeUtil.getParentOfType(position, CompactLedgerDeclaration.class, false);
    if (ledger != null) {
      ASTNode colonNode = ledger.getNode().findChildByType(CompactTokenTypes.COLON);
      if (colonNode != null && position.getTextRange().getStartOffset() >= colonNode.getTextRange().getEndOffset()) {
        return true;
      }
    }

    // 2. Inside struct field type slot: struct Foo { x: <caret> }
    CompactStructFieldImpl structField = PsiTreeUtil.getParentOfType(position, CompactStructFieldImpl.class, false);
    if (structField != null) {
      ASTNode colonNode = structField.getNode().findChildByType(CompactTokenTypes.COLON);
      if (colonNode != null && position.getTextRange().getStartOffset() >= colonNode.getTextRange().getEndOffset()) {
        return true;
      }
    }

    // 3. Inside type alias definition: type Foo = <caret>
    CompactTypeDefinition typeDef = PsiTreeUtil.getParentOfType(position, CompactTypeDefinition.class, false);
    if (typeDef != null) {
      ASTNode eqNode = typeDef.getNode().findChildByType(CompactTokenTypes.ASSIGN);
      if (eqNode != null && position.getTextRange().getStartOffset() >= eqNode.getTextRange().getEndOffset()) {
        return true;
      }
    }

    // 4. Inside const declaration type slot: const x: <caret> = 0;
    CompactConstBindingImpl constBinding = PsiTreeUtil.getParentOfType(position, CompactConstBindingImpl.class, false);
    if (constBinding != null) {
      ASTNode colonNode = constBinding.getNode().findChildByType(CompactTokenTypes.COLON);
      ASTNode eqNode = constBinding.getNode().findChildByType(CompactTokenTypes.ASSIGN);
      if (colonNode != null && position.getTextRange().getStartOffset() >= colonNode.getTextRange().getEndOffset()) {
        if (eqNode == null || position.getTextRange().getStartOffset() <= eqNode.getTextRange().getStartOffset()) {
          return true;
        }
      }
    }

    // 5. After comma inside type arguments, e.g. Vector<#32, <caret>>
    if (previous != null && previous.getNode() != null && previous.getNode().getElementType() == CompactTokenTypes.COMMA) {
      for (PsiElement p = previous; p != null; p = PsiTreeUtil.prevVisibleLeaf(p)) {
        if (p.getNode() == null) break;
        IElementType tt = p.getNode().getElementType();
        if (tt == CompactTokenTypes.LT) {
          return true;
        }
        if (tt == CompactTokenTypes.SEMICOLON || tt == CompactTokenTypes.LBRACE || tt == CompactTokenTypes.RBRACE || tt == CompactTokenTypes.GT) {
          break;
        }
      }
    }

    return false;
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
