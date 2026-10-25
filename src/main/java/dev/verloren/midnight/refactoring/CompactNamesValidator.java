package dev.verloren.midnight.refactoring;

import com.intellij.lang.refactoring.NamesValidator;
import com.intellij.openapi.project.Project;
import com.intellij.psi.tree.IElementType;
import dev.verloren.midnight.lexer.CompactLexer;
import dev.verloren.midnight.lexer.CompactTokenSets;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Validates candidate identifier names during rename refactoring in Compact source files.
 *
 * <h3>Subsystem Role &amp; Extension Point</h3>
 * <p>Registered in {@code plugin.xml} via the {@code com.intellij.lang.namesValidator} extension point
 * for {@link dev.verloren.midnight.CompactLanguage}. Provides identifier and keyword syntax validation
 * across all refactoring workflows:
 * <ul>
 *   <li><b>Keyword Protection:</b> Prevents renaming declarations to reserved language keywords (such as
 *       {@code circuit}, {@code witness}, {@code ledger}, {@code constructor}, {@code struct}, {@code enum},
 *       {@code pragma}, {@code import}, {@code export}) via {@link #isKeyword}.</li>
 *   <li><b>Identifier Well-Formedness:</b> Ensures that candidate names conform strictly to the Compact
 *       lexical grammar for identifiers (starting with a letter or underscore, followed by alphanumeric
 *       characters or underscores) via {@link #isIdentifier}.</li>
 *   <li><b>Compound Token Rejection:</b> Employs an exact single-token boundary verification algorithm
 *       ({@link #singleToken}) ensuring that expressions with whitespace, punctuation, operators, or multiple
 *       tokens are rejected as invalid identifiers.</li>
 * </ul>
 * </p>
 *
 * <h3>Calling Pipeline &amp; Lifecycle</h3>
 * <p>Invoked interactively by IntelliJ's refactoring infrastructure:
 * <ul>
 *   <li><b>In-Place Rename:</b> Invoked on every keystroke in {@code MemberInplaceRenamer} and
 *       {@code VariableInplaceRenamer} to validate the in-editor input box.</li>
 *   <li><b>Modal Rename Dialog:</b> Invoked by {@code RenameDialog} to enable or disable the "Refactor" button
 *       and display error tooltips if an invalid name or keyword is entered.</li>
 * </ul>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Execution Thread:</b> Called on the Event Dispatch Thread (EDT) during interactive keystrokes,
 *       or on pooled background threads during automated refactoring checks.</li>
 *   <li><b>Read Action:</b> Does not require a Read Action because validation operates strictly on pure text
 *       using a standalone {@link CompactLexer} without accessing the PSI tree or document model.</li>
 *   <li><b>Statelessness:</b> The validator maintains zero mutable state, making it thread-safe and allocation-light.</li>
 * </ul>
 *
 * <h3>Lexical Verification Flow</h3>
 * <pre>
 *   [User types candidate name in Rename input]
 *                        │
 *                        ▼
 *             [CompactNamesValidator]
 *                        │
 *               [singleToken(text)]
 *                        │
 *          Runs CompactLexer across text
 *                        │
 *          ┌─────────────┴─────────────┐
 *          ▼                           ▼
 *     [isKeyword]                 [isIdentifier]
 *          │                           │
 *    Checks if token ∈           Checks if token ==
 *    CompactTokenSets.KEYWORDS    CompactTokenTypes.IDENTIFIER
 * </pre>
 *
 * @see dev.verloren.midnight.refactoring.CompactRefactoringSupportProvider
 * @see dev.verloren.midnight.lexer.CompactLexer
 * @see dev.verloren.midnight.lexer.CompactTokenSets
 * @see com.intellij.lang.refactoring.NamesValidator
 */
public class CompactNamesValidator implements NamesValidator {

  /**
   * Checks whether the specified candidate string matches a reserved Compact keyword.
   *
   * @param name    the text string to evaluate
   * @param project the current project context, or null
   * @return true if the string is a reserved keyword; false otherwise
   */
  @Override
  public boolean isKeyword(@NotNull String name, @Nullable Project project) {
    // Step 1: Lex text into a single candidate token
    IElementType tokenType = singleToken(name);
    // Step 2: Test membership in the language's reserved keyword token set
    return CompactTokenSets.KEYWORDS.contains(tokenType);
  }

  /**
   * Checks whether the specified candidate string forms a syntactically valid Compact identifier.
   *
   * @param name    the text string to evaluate
   * @param project the current project context, or null
   * @return true if the string forms exactly one valid identifier token; false otherwise
   */
  @Override
  public boolean isIdentifier(@NotNull String name, @Nullable Project project) {
    // Step 1: Lex text into a single candidate token
    // Step 2: Verify that the single token is an IDENTIFIER
    return singleToken(name) == CompactTokenTypes.IDENTIFIER;
  }

  /**
   * Verifies that the given input string tokenizes into exactly one contiguous token spanning the entire string.
   *
   * <p>Mitigates subtle refactoring injection bugs where a user enters compound tokens (e.g. {@code "a + b"},
   * {@code "foo bar"}, or {@code "int x;"}) which would otherwise cause syntax corruption if accepted as an identifier.</p>
   *
   * @param text the input string to lex
   * @return the {@link IElementType} of the single token spanning the entire input, or null if the string
   *         is empty, produces multiple tokens, or does not consume the entire input
   */
  private static @Nullable IElementType singleToken(@NotNull String text) {
    // Step 1: Start lexer on the input text buffer
    CompactLexer lexer = new CompactLexer();
    lexer.start(text);

    // Step 2: Verify the initial token starts at offset 0 and spans the full length of the text
    IElementType tokenType = lexer.getTokenType();
    if (tokenType == null || lexer.getTokenStart() != 0 || lexer.getTokenEnd() != text.length()) {
      return null;
    }

    // Step 3: Advance past the first token and ensure no trailing tokens or garbage characters remain
    lexer.advance();
    return lexer.getTokenType() == null ? tokenType : null;
  }
}