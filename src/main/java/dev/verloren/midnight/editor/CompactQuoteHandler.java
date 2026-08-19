package dev.verloren.midnight.editor;

import com.intellij.codeInsight.editorActions.SimpleTokenSetQuoteHandler;
import dev.verloren.midnight.lexer.CompactTokenTypes;

/**
 * Registers string literal delimiter quote handling for Compact source files.
 *
 * <p>Enables automatic insertion and smart navigation through double/single quotes during editing.</p>
 */
public class CompactQuoteHandler extends SimpleTokenSetQuoteHandler {
  public CompactQuoteHandler() {
    super(CompactTokenTypes.STRING_LITERAL);
  }
}
