package dev.verloren.midnight.editor;

import com.intellij.openapi.util.NlsSafe;
import org.jetbrains.annotations.NotNull;

/**
 * Statement surrounder that wraps selected Compact statements within a scoped block ({@code { ... }}).
 *
 * <h3>Architectural Role</h3>
 * <p>Registered in {@link CompactSurroundDescriptor} as one of the standard surround actions. When invoked,
 * it introduces a new lexical block scope around the selected statements, allowing local variable scoping
 * or code grouping within circuits, constructors, and functions.</p>
 *
 * <h3>Transformation Example</h3>
 * <pre>
 *   Before:
 *     const a = 1;
 *     const b = 2;
 *
 *   After:
 *     {
 *       const a = 1;
 *       const b = 2;
 *     }
 * </pre>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>EDT:</b> Invoked on EDT within a WriteCommandAction via {@link CompactSurrounderBase}.</li>
 * </ul>
 *
 * @see CompactSurroundDescriptor
 * @see CompactSurrounderBase
 * @see CompactIfSurrounder
 */
public class CompactBlockSurrounder extends CompactSurrounderBase {

  /**
   * Returns the user-facing label displayed in the "Surround With..." popup menu.
   *
   * @return {@code "{ ... }"}
   */
  @Override
  public @NlsSafe String getTemplateDescription() {
    return "{ ... }";
  }

  /**
   * Generates a block construct containing the selected text indented between curly braces.
   *
   * @param selectedText the statements to enclose
   * @return the formatted block string
   */
  @Override
  protected @NotNull String generateReplacement(@NotNull String selectedText) {
    return "{\n" + selectedText + "\n}";
  }
}
