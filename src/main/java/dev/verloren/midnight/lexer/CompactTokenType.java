package dev.verloren.midnight.lexer;

import com.intellij.psi.tree.IElementType;
import dev.verloren.midnight.CompactLanguage;

/**
 * Base element type for all lexical tokens produced by {@link CompactLexer}.
 *
 * <p>Extends IntelliJ's {@link IElementType} and associates all token instances
 * with {@link CompactLanguage#INSTANCE}.</p>
 */
public class CompactTokenType extends IElementType {
  /**
   * Constructs a Compact token type with a debug name for AST and logging representation.
   *
   * @param debugName the human-readable name of the token (e.g., "CIRCUIT", "PLUS")
   */
  public CompactTokenType(String debugName) {
    super(debugName, CompactLanguage.INSTANCE);
  }
}
