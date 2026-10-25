package dev.verloren.midnight.editor;

import com.intellij.lang.BracePair;
import com.intellij.lang.PairedBraceMatcher;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Registers paired bracket and delimiter token definitions for Compact source code.
 *
 * <h3>Architectural Role &amp; Extension Point</h3>
 * <p>Registered as a {@code pairedBraceMatcher} extension for language {@code Compact} in {@code plugin.xml}.
 * Implements {@link PairedBraceMatcher} to integrate with IntelliJ's editor delimiter engine, powering:</p>
 * <ul>
 *   <li><b>Matched Brace Highlighting:</b> Visually highlighting matching opening and closing delimiters when
 *       the caret rests adjacent to any boundary symbol.</li>
 *   <li><b>Automatic Delimiter Pairing:</b> Automatically typing the complementary closing symbol when
 *       the opening character is typed in the editor.</li>
 *   <li><b>Code Block Highlighting:</b> Determining structural blocks (indicated by {@code structural = true}
 *       on curly braces {@code { ... }}).</li>
 *   <li><b>Structural Navigation:</b> Moving the caret between opening and closing delimiters via standard
 *       IDE shortcuts (e.g., Navigate to Block Start/End, Ctrl+[ and Ctrl+]).</li>
 * </ul>
 *
 * <h3>Registered Delimiter Pairs</h3>
 * <table border="1">
 *   <caption>Delimiter Pairs in Compact</caption>
 *   <tr><th>Opening</th><th>Closing</th><th>Structural</th><th>Usage Context</th></tr>
 *   <tr><td><code>{</code></td><td><code>}</code></td><td>true</td><td>Contract, circuit, ledger, and statement code blocks</td></tr>
 *   <tr><td><code>(</code></td><td><code>)</code></td><td>false</td><td>Function parameter lists, call arguments, parenthesized expressions</td></tr>
 *   <tr><td><code>[</code></td><td><code>]</code></td><td>false</td><td>Index expressions, array/vector literals</td></tr>
 *   <tr><td><code>&lt;</code></td><td><code>&gt;</code></td><td>false</td><td>Generic type parameter lists (e.g. {@code Bytes<32>}, {@code Vector<N, T>})</td></tr>
 * </table>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Thread Safety:</b> Completely stateless and immutable. Methods are called on EDT or background
 *       read threads during editor rendering and keystroke handling.</li>
 * </ul>
 */
public class CompactPairedBraceMatcher implements PairedBraceMatcher {

  /**
   * Pre-allocated array of delimiter pairs configured for Compact syntax.
   */
  private static final BracePair[] PAIRS = new BracePair[]{
      new BracePair(CompactTokenTypes.LBRACE, CompactTokenTypes.RBRACE, true),
      new BracePair(CompactTokenTypes.LPAREN, CompactTokenTypes.RPAREN, false),
      new BracePair(CompactTokenTypes.LBRACKET, CompactTokenTypes.RBRACKET, false),
      new BracePair(CompactTokenTypes.LT, CompactTokenTypes.GT, false)
  };

  /**
   * Returns the array of delimiter token pairs recognized by this brace matcher.
   *
   * @return immutable array of {@link BracePair} definitions
   */
  @Override
  public BracePair @NotNull [] getPairs() {
    return PAIRS;
  }

  /**
   * Determines whether an opening brace is allowed to be automatically paired when typed
   * directly before a token of type {@code contextType}.
   *
   * @param lbraceType element type of the opening delimiter being typed
   * @param contextType element type of the token immediately following the caret, or {@code null} if at EOF
   * @return {@code true} to allow automatic brace insertion across all contexts
   */
  @Override
  public boolean isPairedBracesAllowedBeforeType(@NotNull IElementType lbraceType, @Nullable IElementType contextType) {
    return true;
  }

  /**
   * Computes the starting offset of the code construct associated with the opening brace.
   *
   * <p>Returning {@code openingBraceOffset} defaults the construct boundary to the brace itself.</p>
   *
   * @param file the PSI file containing the opening brace
   * @param openingBraceOffset document offset of the opening delimiter
   * @return the document offset where the construct starts
   */
  @Override
  public int getCodeConstructStart(PsiFile file, int openingBraceOffset) {
    return openingBraceOffset;
  }
}
