package dev.verloren.midnight.parser;

import com.intellij.psi.tree.IElementType;
import dev.verloren.midnight.CompactLanguage;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

/**
 * Base element type for all non-terminal composite AST nodes produced by {@link CompactParser}.
 *
 * <p>Extends {@link IElementType} and associates all syntactic composite types
 * (expressions, statements, declarations) with {@link CompactLanguage#INSTANCE}.</p>
 */
public final class CompactElementType extends IElementType {
  /**
   * Constructs an element type with a debug name representing a grammar rule or PSI category.
   *
   * @param debugName human-readable name of the AST element type (e.g. "CIRCUIT_DEFINITION")
   */
  public CompactElementType(@NonNls @NotNull String debugName) {
    super(debugName, CompactLanguage.INSTANCE);
  }
}
