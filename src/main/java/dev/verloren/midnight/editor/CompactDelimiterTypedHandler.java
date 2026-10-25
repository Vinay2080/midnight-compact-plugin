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
 * <h3>Subsystem Role &amp; Extension Point</h3>
 * <p>Registered in {@code plugin.xml} via the {@code com.intellij.typedHandler} extension point.
 * Implements smooth "over-typing" ergonomics for Compact smart contract code:
 * <ul>
 *   <li><b>Closing Delimiters:</b> Intercepts {@code )}, {@code ]}, {@code }}, and {@code >}. If the user
 *       types a closing bracket that was already auto-inserted by pair-matching, moves the caret past
 *       the bracket instead of inserting a redundant character.</li>
 *   <li><b>Structural Punctuation:</b> Intercepts colons ({@code :}), semicolons ({@code ;}), and commas ({@code ,}).
 *       Prevents duplicate punctuation when typing at the boundary of generated templates or completion insertions.</li>
 *   <li><b>Closing String Quotes:</b> Intercepts single and double quotes ({@code "} and {@code '}) at the trailing
 *       boundary of string literals, stepping the caret forward.</li>
 * </ul>
 * </p>
 *
 * <h3>Calling Pipeline &amp; Lifecycle</h3>
 * <p>Invoked by IntelliJ's typing engine in {@link #beforeCharTyped} prior to character insertion.
 * If the typed character matches the document text and lexer token directly under the caret,
 * the caret is moved forward by 1 position and {@link Result#STOP} is returned to halt default insertion.</p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Execution Thread:</b> Always executed on the Event Dispatch Thread (EDT) within an active
 *       editor keystroke event.</li>
 *   <li><b>Read Action:</b> Uses {@link HighlighterIterator} to inspect the lexer token under the caret
 *       without requiring a full PSI read lock.</li>
 * </ul>
 *
 * @see dev.verloren.midnight.editor.CompactAngleBraceTypedHandler
 * @see dev.verloren.midnight.editor.CompactQuoteHandler
 * @see com.intellij.codeInsight.editorActions.TypedHandlerDelegate
 */
public final class CompactDelimiterTypedHandler extends TypedHandlerDelegate {

  /**
   * Mapping from punctuation and delimiter characters to their corresponding lexical token types.
   */
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
