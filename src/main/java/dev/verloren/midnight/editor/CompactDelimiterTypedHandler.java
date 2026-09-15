package dev.verloren.midnight.editor;

import com.intellij.codeInsight.CodeInsightSettings;
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.highlighter.HighlighterIterator;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import dev.verloren.midnight.psi.CompactFile;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Intercepts typed delimiters and structural punctuation symbols to step over (skip)
 * the editor caret when the matching character is already present immediately ahead.
 *
 * <p>Supports closing delimiters ({@code )}, {@code ]}, {@code }}, {@code >}),
 * structural punctuation ({@code :}, {@code ;}, {@code ,}), and closing string quotes
 * ({@code "}, {@code '}).</p>
 */
public final class CompactDelimiterTypedHandler extends TypedHandlerDelegate {

  private static final Map<Character, IElementType> DELIMITER_TOKENS = Map.of(
      ')', CompactTokenTypes.RPAREN,
      ']', CompactTokenTypes.RBRACKET,
      '}', CompactTokenTypes.RBRACE,
      '>', CompactTokenTypes.GT,
      ':', CompactTokenTypes.COLON,
      ';', CompactTokenTypes.SEMICOLON,
      ',', CompactTokenTypes.COMMA
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

    if (editor.getSelectionModel().hasSelection()) {
      return Result.CONTINUE;
    }

    if (!(editor instanceof EditorEx editorEx)) {
      return Result.CONTINUE;
    }

    int offset = editor.getCaretModel().getOffset();
    CharSequence chars = editor.getDocument().getCharsSequence();
    if (offset >= chars.length() || chars.charAt(offset) != c) {
      return Result.CONTINUE;
    }

    HighlighterIterator iterator = editorEx.getHighlighter().createIterator(offset);
    if (iterator.atEnd()) {
      return Result.CONTINUE;
    }

    IElementType tokenType = iterator.getTokenType();

    // Check closing brackets and structural punctuation: ), ], }, >, :, ;, ,
    IElementType expectedToken = DELIMITER_TOKENS.get(c);
    if (expectedToken != null) {
      if (tokenType == expectedToken) {
        if (isBracket(c) && !CodeInsightSettings.getInstance().AUTOINSERT_PAIR_BRACKET) {
          return Result.CONTINUE;
        }
        editor.getCaretModel().moveToOffset(offset + 1);
        return Result.STOP;
      }
      return Result.CONTINUE;
    }

    // Check string literal closing quotes: " and '
    if (c == '"' || c == '\'') {
      if (tokenType == CompactTokenTypes.STRING_LITERAL && offset == iterator.getEnd() - 1) {
        if (!CodeInsightSettings.getInstance().AUTOINSERT_PAIR_QUOTE) {
          return Result.CONTINUE;
        }
        editor.getCaretModel().moveToOffset(offset + 1);
        return Result.STOP;
      }
    }

    return Result.CONTINUE;
  }

  private static boolean isBracket(char c) {
    return c == ')' || c == ']' || c == '}' || c == '>';
  }
}
