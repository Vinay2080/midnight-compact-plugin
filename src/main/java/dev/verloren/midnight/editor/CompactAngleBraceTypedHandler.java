package dev.verloren.midnight.editor;

import com.intellij.codeInsight.CodeInsightSettings;
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.highlighter.HighlighterIterator;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import dev.verloren.midnight.psi.CompactFile;
import org.jetbrains.annotations.NotNull;

/**
 * Handles automatic pairing of angle brackets ({@code <...>}) in Compact code.
 *
 * <p>When a user types {@code <} after a generic type name, built-in type, or generic
 * declaration keyword, this handler automatically inserts the closing {@code >} and
 * places the caret between them ({@code <|>}), mirroring the behavior of {@code ()},
 * {@code {}}, and {@code []}.</p>
 *
 * <p>When a user types {@code >} immediately before an existing closing angle bracket,
 * this handler moves the caret forward without inserting a duplicate bracket.</p>
 */
public class CompactAngleBraceTypedHandler extends TypedHandlerDelegate {

  private static final Key<Boolean> OPENING_TYPED_KEY =
      Key.create("dev.verloren.midnight.editor.COMPACT_ANGLE_OPENING_TYPED");

  private static final TokenSet GENERIC_DECLARATION_KEYWORDS = TokenSet.create(
      CompactTokenTypes.CIRCUIT,
      CompactTokenTypes.WITNESS,
      CompactTokenTypes.STRUCT,
      CompactTokenTypes.ENUM,
      CompactTokenTypes.TYPE,
      CompactTokenTypes.MODULE,
      CompactTokenTypes.CONTRACT
  );

  private static final TokenSet INVALID_INSIDE_TOKENS = TokenSet.create(
      CompactTokenTypes.LBRACE,
      CompactTokenTypes.RBRACE,
      CompactTokenTypes.SEMICOLON
  );

  @Override
  public @NotNull Result beforeCharTyped(
      char c,
      @NotNull Project project,
      @NotNull Editor editor,
      @NotNull PsiFile file,
      @NotNull FileType fileType
  ) {
    if (!(file instanceof CompactFile)) {
      return Result.CONTINUE;
    }

    if (!CodeInsightSettings.getInstance().AUTOINSERT_PAIR_BRACKET) {
      return Result.CONTINUE;
    }

    if (c == '<') {
      if (shouldComplete(editor)) {
        editor.putUserData(OPENING_TYPED_KEY, Boolean.TRUE);
      }
    } else if (c == '>') {
      int offset = editor.getCaretModel().getOffset();
      if (offset < editor.getDocument().getTextLength() && editor instanceof EditorEx editorEx) {
        HighlighterIterator iterator = editorEx.getHighlighter().createIterator(offset);
        if (!iterator.atEnd()
            && iterator.getTokenType() == CompactTokenTypes.GT
            && calculateBalance(editor) == 0) {
          editor.getCaretModel().moveToOffset(offset + 1);
          return Result.STOP;
        }
      }
    }

    return Result.CONTINUE;
  }

  @Override
  public @NotNull Result charTyped(
      char c,
      @NotNull Project project,
      @NotNull Editor editor,
      @NotNull PsiFile file
  ) {
    if (!(file instanceof CompactFile)) {
      return Result.CONTINUE;
    }

    Boolean openingTyped = editor.getUserData(OPENING_TYPED_KEY);
    if (Boolean.TRUE.equals(openingTyped)) {
      editor.putUserData(OPENING_TYPED_KEY, null);
      int balance = calculateBalance(editor);
      if (balance == 1) {
        int offset = editor.getCaretModel().getOffset();
        editor.getDocument().insertString(offset, ">");
      }
    }

    return super.charTyped(c, project, editor, file);
  }

  /**
   * Determines whether typing {@code <} at the current editor position should automatically
   * insert a matching {@code >}.
   *
   * @param editor the current editor
   * @return {@code true} if {@code <} should be auto-paired
   */
  public static boolean shouldComplete(@NotNull Editor editor) {
    if (editor.getSelectionModel().hasSelection()) {
      return false;
    }

    int offset = editor.getCaretModel().getOffset();
    if (offset <= 0 || !(editor instanceof EditorEx editorEx)) {
      return false;
    }

    HighlighterIterator iterator = editorEx.getHighlighter().createIterator(offset - 1);
    if (iterator.atEnd()) {
      return false;
    }

    IElementType tokenType = iterator.getTokenType();

    // Do not complete inside comments or strings
    if (tokenType == CompactTokenTypes.LINE_COMMENT
        || tokenType == CompactTokenTypes.BLOCK_COMMENT
        || tokenType == CompactTokenTypes.UNTERMINATED_BLOCK_COMMENT
        || tokenType == CompactTokenTypes.STRING_LITERAL
        || tokenType == CompactTokenTypes.UNTERMINATED_STRING) {
      return false;
    }

    // Built-in types that take generic/size parameters: Vector, Uint, Bytes, Opaque, Field, Boolean
    if (tokenType == CompactTokenTypes.VECTOR_TYPE
        || tokenType == CompactTokenTypes.UINT_TYPE
        || tokenType == CompactTokenTypes.BYTES_TYPE
        || tokenType == CompactTokenTypes.OPAQUE_TYPE
        || tokenType == CompactTokenTypes.FIELD_TYPE
        || tokenType == CompactTokenTypes.BOOLEAN_TYPE) {
      return iterator.getEnd() == offset;
    }

    // Built-in expressions taking generic arguments: default<Type>, slice<tsize>
    if (tokenType == CompactTokenTypes.DEFAULT || tokenType == CompactTokenTypes.SLICE) {
      return iterator.getEnd() == offset;
    }

    // Identifiers: user-defined types, stdlib types (Map, Set, List, Cell), generic type parameters (T, K, V),
    // or generic entity declaration names (circuit foo<, struct Bar<, type Baz<, witness qux<)
    if (tokenType == CompactTokenTypes.IDENTIFIER) {
      if (iterator.getEnd() != offset) {
        return false;
      }

      if (isPrecededByGenericDeclarationKeyword(iterator)) {
        return true;
      }

      return isTypeLikeIdentifier(offset, editor, iterator);
    }

    return false;
  }

  private static boolean isPrecededByGenericDeclarationKeyword(@NotNull HighlighterIterator iterator) {
    int steps = 0;
    try {
      while (iterator.getStart() > 0) {
        iterator.retreat();
        steps++;
        IElementType prevType = iterator.getTokenType();
        if (prevType == CompactTokenTypes.WHITE_SPACE
            || prevType == CompactTokenTypes.LINE_COMMENT
            || prevType == CompactTokenTypes.BLOCK_COMMENT) {
          continue;
        }
        return GENERIC_DECLARATION_KEYWORDS.contains(prevType);
      }
      return false;
    } finally {
      while (steps > 0) {
        iterator.advance();
        steps--;
      }
    }
  }

  private static boolean isTypeLikeIdentifier(
      int offset,
      @NotNull Editor editor,
      @NotNull HighlighterIterator iterator
  ) {
    if (iterator.getEnd() != offset) {
      return false;
    }
    CharSequence chars = editor.getDocument().getCharsSequence();
    int start = iterator.getStart();
    int end = iterator.getEnd();
    if (start >= end || start >= chars.length()) {
      return false;
    }

    char first = chars.charAt(start);
    if (!Character.isUpperCase(first)) {
      return false;
    }

    // Single uppercase letter (e.g., T, K, V, E, N)
    if (end == start + 1) {
      return true;
    }

    // For multi-character identifiers starting with uppercase (e.g., Vector, Map, Set, MyType),
    // require at least one lowercase character to avoid ALL_CAPS constants like MAX_VALUE.
    for (int i = start + 1; i < end; i++) {
      if (Character.isLowerCase(chars.charAt(i))) {
        return true;
      }
    }

    return false;
  }

  /**
   * Calculates the balance of {@code <} and {@code >} within the enclosing statement or block.
   *
   * @param editor the current editor
   * @return positive if unclosed {@code <}, negative if excess {@code >}, 0 if balanced
   */
  public static int calculateBalance(@NotNull Editor editor) {
    int offset = editor.getCaretModel().getOffset() - 1;
    if (offset < 0 || !(editor instanceof EditorEx editorEx)) {
      return 0;
    }

    if (offset >= editor.getDocument().getTextLength()) {
      offset = editor.getDocument().getTextLength() - 1;
    }
    if (offset < 0) {
      return 0;
    }

    HighlighterIterator iterator = editorEx.getHighlighter().createIterator(offset);
    while (iterator.getStart() > 0 && !INVALID_INSIDE_TOKENS.contains(iterator.getTokenType())) {
      iterator.retreat();
    }

    if (INVALID_INSIDE_TOKENS.contains(iterator.getTokenType())) {
      iterator.advance();
    }

    int balance = 0;
    while (!iterator.atEnd() && balance >= 0 && !INVALID_INSIDE_TOKENS.contains(iterator.getTokenType())) {
      IElementType tokenType = iterator.getTokenType();
      if (tokenType == CompactTokenTypes.LT) {
        balance++;
      } else if (tokenType == CompactTokenTypes.GT) {
        balance--;
      }
      iterator.advance();
    }

    return balance;
  }
}
