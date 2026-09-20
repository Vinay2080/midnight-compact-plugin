package dev.verloren.midnight.completion;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.lexer.CompactTokenSets;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import dev.verloren.midnight.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Context classifier determining the semantic role of the cursor position during code completion.
 *
 * <p>Inspects preceding AST tokens and containing PSI elements to distinguish between
 * top-level declaration keywords, block statements, type positions (after {@code :}, {@code as}, {@code <}),
 * parameterized type sizes (inside {@code Bytes<...>} or {@code Uint<...>}), member access (after {@code .}),
 * pragma directives (after {@code pragma}), and general value expressions.</p>
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

    if (isAfterPragma(position, previous)) {
      return Kind.AFTER_PRAGMA;
    }

    Kind sizeKind = checkParameterizedTypeSizeContext(previous);
    if (sizeKind != null) {
      return sizeKind;
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

  public static boolean isAfterPragma(@NotNull PsiElement position, @Nullable PsiElement previous) {
    if (previous != null && previous.getNode() != null && previous.getNode().getElementType() == CompactTokenTypes.PRAGMA) {
      return true;
    }
    CompactPragmaForm pragma = PsiTreeUtil.getParentOfType(position, CompactPragmaForm.class, false);
    if (pragma != null) {
      PsiElement id = pragma.getPragmaIdentifier();
      return id == null || id == position || PsiTreeUtil.isAncestor(id, position, false);
    }
    return false;
  }

  private static @Nullable Kind checkParameterizedTypeSizeContext(@Nullable PsiElement previous) {
    if (previous == null || previous.getNode() == null) {
      return null;
    }

    // Walk backward across numbers, identifiers (or dummy completion tokens) up to '<'
    PsiElement curr = previous;
    while (curr != null && curr.getNode() != null) {
      IElementType type = curr.getNode().getElementType();
      if (type == CompactTokenTypes.LT) {
        PsiElement beforeLt = prevNonCommentLeaf(curr);
        if (beforeLt != null) {
          String name = beforeLt.getText();
          if ("Bytes".equals(name)) {
            return Kind.BYTES_SIZE;
          }
          if ("Uint".equals(name)) {
            return Kind.UINT_SIZE;
          }
        }
        return null;
      }
      if (type == CompactTokenTypes.COMMA || type == CompactTokenTypes.GT
          || type == CompactTokenTypes.SEMICOLON || type == CompactTokenTypes.LBRACE || type == CompactTokenTypes.RBRACE
          || type == CompactTokenTypes.HASH) {
        return null;
      }
      curr = prevNonCommentLeaf(curr);
    }
    return null;
  }

  public static boolean isExportPreceding(@NotNull PsiElement position) {
    if (isComment(position)) {
      return false;
    }
    // 1. Walk backward through visible non-comment AST leaves until statement/block boundary or declaration keyword
    for (PsiElement p = prevNonCommentLeaf(position); p != null; p = prevNonCommentLeaf(p)) {
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
    return stmtBefore.matches("^export(\\s+(sealed|pure|new))?(\\s+\\w*)?$");
  }

  private static boolean isAfterTypeIntro(PsiElement previous) {
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

  private static boolean isTypeColon(@NotNull PsiElement colonLeaf) {
    PsiElement beforeColon = prevNonCommentLeaf(colonLeaf);
    if (beforeColon == null || beforeColon.getNode() == null) {
      return false;
    }
    IElementType beforeType = beforeColon.getNode().getElementType();
    // 1. After callable parameter list: circuit foo(): <caret>
    if (beforeType == CompactTokenTypes.RPAREN) {
      return true;
    }
    // 2. After identifier: could be ledger, const, parameter, struct field, or struct literal
    if (beforeType == CompactTokenTypes.IDENTIFIER) {
      PsiElement beforeId = prevNonCommentLeaf(beforeColon);
      if (beforeId != null && beforeId.getNode() != null) {
        IElementType beforeIdType = beforeId.getNode().getElementType();
        if (beforeIdType == CompactTokenTypes.LEDGER) {
          return true; // ledger foo: <caret>
        }
        if (beforeIdType == CompactTokenTypes.LPAREN) {
          return true; // (foo: <caret>)
        }
        if (beforeIdType == CompactTokenTypes.CONST) {
          return true; // const foo: <caret>
        }
        if (beforeIdType == CompactTokenTypes.COMMA) {
          // Walk back to find if we're inside '(' (parameter list) vs '{' (struct literal)
          for (PsiElement p = beforeId; p != null; p = prevNonCommentLeaf(p)) {
            if (p.getNode() == null) break;
            IElementType t = p.getNode().getElementType();
            if (t == CompactTokenTypes.LPAREN) {
              return true; // in parameter list
            }
            if (t == CompactTokenTypes.LBRACE || t == CompactTokenTypes.SEMICOLON) {
              break;
            }
          }
        }
      }
      // Check if inside struct definition: struct Foo { field: <caret> }
      if (PsiTreeUtil.getParentOfType(colonLeaf, CompactStructDefinition.class, false) != null) {
        return true;
      }
    }
    return false;
  }

  private static boolean isTypePosition(@NotNull PsiElement position, PsiElement previous) {
    // 1. Inside type alias definition: type Foo = <caret>
    CompactTypeDefinition typeDef = PsiTreeUtil.getParentOfType(position, CompactTypeDefinition.class, false);
    if (typeDef != null) {
      ASTNode eqNode = typeDef.getNode().findChildByType(CompactTokenTypes.ASSIGN);
      if (eqNode != null && position.getTextRange().getStartOffset() >= eqNode.getTextRange().getEndOffset()) {
        return true;
      }
    }

    // 2. After comma inside type arguments, e.g. Vector<#32, <caret>> or Either<Field, <caret>>
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

  private static boolean isDeclarationOrStatementStart(PsiElement previous) {
    if (previous == null) {
      return true;
    }
    if (previous.getNode() == null) {
      return false;
    }
    IElementType type = previous.getNode().getElementType();
    return type == CompactTokenTypes.SEMICOLON
            || type == CompactTokenTypes.LBRACE
            || type == CompactTokenTypes.RBRACE
            || type == CompactTokenTypes.EXPORT
            || type == CompactTokenTypes.SEALED
            || type == CompactTokenTypes.PURE
            || type == CompactTokenTypes.NEW;
  }

  public enum Kind {
    KEYWORD,
    STATEMENT,
    AFTER_EXPORT,
    AFTER_SEALED,
    AFTER_PURE,
    AFTER_NEW,
    AFTER_PRAGMA,
    TYPE,
    BYTES_SIZE,
    UINT_SIZE,
    VALUE,
    MEMBER,
    NONE
  }
}
