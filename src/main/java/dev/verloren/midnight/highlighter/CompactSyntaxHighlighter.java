package dev.verloren.midnight.highlighter;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import dev.verloren.midnight.lexer.CompactLexer;
import dev.verloren.midnight.lexer.CompactTokenSets;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import org.jspecify.annotations.NonNull;

/**
 * High-speed lexical syntax highlighter for Compact source code.
 *
 * <h3>Subsystem Role &amp; Architecture</h3>
 * <p>Extends {@link SyntaxHighlighterBase}. Serves as Tier 1 of IntelliJ's two-tiered syntax highlighting engine:
 * <ul>
 *   <li><b>Tier 1 (Lexical Highlighting - This Class):</b> Operates instantaneously during editor opening
 *       and raw typing by tokenizing document character streams with {@link CompactLexer}. Does not require
 *       an abstract syntax tree (AST) or symbol resolution, ensuring zero typing latency.</li>
 *   <li><b>Tier 2 (Semantic Highlighting - {@link CompactHighlightingAnnotator}):</b> Runs asynchronously
 *       in the background once the PSI tree is built, enriching the basic colours with declaration roles,
 *       call targets, variable mutability, doc tags, and string escapes.</li>
 * </ul>
 * </p>
 *
 * <h3>Calling Pipeline &amp; Lifecycle</h3>
 * <p>Instantiated by {@link CompactSyntaxHighlighterFactory} whenever an editor, preview pane,
 * or colour settings page opens a Compact source file. IntelliJ invokes {@link #getHighlightingLexer()}
 * to get a fresh lexer instance, followed by repeated calls to {@link #getTokenHighlights(IElementType)}
 * for each token in the viewport.</p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Execution Thread:</b> Invoked on the Event Dispatch Thread (EDT) or pooled rendering threads
 *       during syntax highlighting and editor painting passes.</li>
 *   <li><b>Locking:</b> Does not require any Read or Write actions because it operates strictly on lexical token
 *       types without accessing PSI trees or document models.</li>
 *   <li><b>Reentrancy:</b> The returned {@link CompactLexer} is an independent instance per highlighter,
 *       ensuring safe concurrent lexing across multiple editor tabs.</li>
 * </ul>
 *
 * <h3>Highlighting Tier Flow</h3>
 * <pre>
 *   [Raw Document Keystroke]
 *              │
 *              ▼
 *   [CompactSyntaxHighlighter (Tier 1: Lexical)] ──► Instant basic colours (Keywords, Literals, Punctuation)
 *              │
 *              ▼ (Background Parser Daemon)
 *   [CompactHighlightingAnnotator (Tier 2: Semantic)] ──► Deep contextual colours (Circuits, Witnesses, Ledgers)
 * </pre>
 *
 * @see dev.verloren.midnight.highlighter.CompactHighlighterColors
 * @see dev.verloren.midnight.highlighter.CompactHighlightingAnnotator
 * @see dev.verloren.midnight.highlighter.CompactSyntaxHighlighterFactory
 * @see com.intellij.openapi.fileTypes.SyntaxHighlighterBase
 */
public class CompactSyntaxHighlighter extends SyntaxHighlighterBase {

  /** Text attributes for Compact language reserved keywords. */
  public static final TextAttributesKey KEYWORD = CompactHighlighterColors.KEYWORD;

  /** Text attributes for built-in primitive and container types. */
  public static final TextAttributesKey TYPE = CompactHighlighterColors.BUILTIN_TYPE;

  /** Text attributes for string literals. */
  public static final TextAttributesKey STRING = CompactHighlighterColors.STRING;

  /** Text attributes for numeric literals (decimal, hex, binary). */
  public static final TextAttributesKey NUMBER = CompactHighlighterColors.NUMBER;

  /** Text attributes for single-line comments. */
  public static final TextAttributesKey COMMENT = CompactHighlighterColors.LINE_COMMENT;

  /** Text attributes for bad or unrecognized characters. */
  public static final TextAttributesKey BAD_CHARACTER = CompactHighlighterColors.BAD_CHARACTER;

  /** Text attributes for boolean constants (true, false). */
  public static final TextAttributesKey CONSTANT = CompactHighlighterColors.BOOLEAN;

  /** Text attributes for compiler and language version literals. */
  public static final TextAttributesKey VERSION = CompactHighlighterColors.VERSION;

  /** Text attributes for mathematical, logical, and structural operators. */
  public static final TextAttributesKey OPERATOR = CompactHighlighterColors.OPERATOR;

  /** Text attributes for parentheses delimiters {@code ()}. */
  public static final TextAttributesKey PARENTHESES = CompactHighlighterColors.PARENTHESES;

  /** Text attributes for curly braces delimiters {@code {}}. */
  public static final TextAttributesKey BRACES = CompactHighlighterColors.BRACES;

  /** Text attributes for square brackets delimiters {@code []}. */
  public static final TextAttributesKey BRACKETS = CompactHighlighterColors.BRACKETS;

  /** Text attributes for comma separators {@code ,}. */
  public static final TextAttributesKey COMMA = CompactHighlighterColors.COMMA;

  /** Text attributes for semicolon statement terminators {@code ;}. */
  public static final TextAttributesKey SEMICOLON = CompactHighlighterColors.SEMICOLON;

  /** Text attributes for member access dot operators {@code .}. */
  public static final TextAttributesKey DOT = CompactHighlighterColors.DOT;

  /** Text attributes for colon type and mapping separators {@code :}. */
  public static final TextAttributesKey COLON = CompactHighlighterColors.COLON;

  /** Text attributes for pragma directive metadata. */
  public static final TextAttributesKey PRAGMA = CompactHighlighterColors.PRAGMA;

  /** Text attributes for multi-line block comments. */
  public static final TextAttributesKey BLOCK_COMMENT = CompactHighlighterColors.BLOCK_COMMENT;

  /**
   * Instantiates a fresh {@link CompactLexer} for lexical tokenization.
   * <p>
   * Invoked by IntelliJ's editor highlighting a pipeline to get an isolated lexer instance.
   * Because JFlex lexers maintain an internal stream state, each highlighter requires an independent instance.
   * </p>
   *
   * @return a new, uninitialized {@link CompactLexer} instance
   */
  @Override
  public @NonNull Lexer getHighlightingLexer() {
    return new CompactLexer();
  }

  /**
   * Maps a raw lexical {@link IElementType} to an array of {@link TextAttributesKey}s.
   * <p>
   * Invoked repeatedly by IntelliJ's rendering pipeline during viewport painting. Uses {@link #pack(TextAttributesKey)}
   * to package single attributes or returns an empty array for unstyled tokens.
   * </p>
   *
   * @param tokenType the lexical token type produced by {@link CompactLexer}
   * @return array of text attribute keys to apply, or empty array if no styling is needed
   */
  @Override
  public TextAttributesKey @NonNull [] getTokenHighlights(IElementType tokenType) {
    // Step 1: Modifiers (export, pure, sealed, new, implements, external)
    if (tokenType == CompactTokenTypes.EXPORT ||
        tokenType == CompactTokenTypes.PURE ||
        tokenType == CompactTokenTypes.SEALED ||
        tokenType == CompactTokenTypes.NEW ||
        tokenType == CompactTokenTypes.IMPLEMENTS ||
        tokenType == CompactTokenTypes.EXTERNAL) {
      return pack(CompactHighlighterColors.MODIFIER);
    }

    // Step 2: Pragma directive keyword
    if (tokenType == CompactTokenTypes.PRAGMA) {
      return pack(PRAGMA);
    }

    // Step 3: Built-in functions and intrinsics (assert, disclose, fold, slice, pad, emit, map)
    if (tokenType == CompactTokenTypes.ASSERT ||
        tokenType == CompactTokenTypes.DISCLOSE ||
        tokenType == CompactTokenTypes.FOLD ||
        tokenType == CompactTokenTypes.SLICE ||
        tokenType == CompactTokenTypes.PAD ||
        tokenType == CompactTokenTypes.EMIT ||
        tokenType == CompactTokenTypes.MAP) {
      return pack(CompactHighlighterColors.BUILTIN_FUNCTION);
    }

    // Step 4: General language keywords (declarations, control flow, ZK primitives)
    if (
            tokenType == CompactTokenTypes.IMPORT ||
            tokenType == CompactTokenTypes.FROM ||
            tokenType == CompactTokenTypes.MODULE ||
            tokenType == CompactTokenTypes.PREFIX ||
            tokenType == CompactTokenTypes.AS ||
            tokenType == CompactTokenTypes.CIRCUIT ||
            tokenType == CompactTokenTypes.CONST ||
            tokenType == CompactTokenTypes.CONSTRUCTOR ||
            tokenType == CompactTokenTypes.CONTRACT ||
            tokenType == CompactTokenTypes.DEFAULT ||
            tokenType == CompactTokenTypes.ELSE ||
            tokenType == CompactTokenTypes.ENUM ||
            tokenType == CompactTokenTypes.FOR ||
            tokenType == CompactTokenTypes.IF ||
            tokenType == CompactTokenTypes.INCLUDE ||
            tokenType == CompactTokenTypes.LEDGER ||
            tokenType == CompactTokenTypes.OF ||
            tokenType == CompactTokenTypes.RETURN ||
            tokenType == CompactTokenTypes.STRUCT ||
            tokenType == CompactTokenTypes.TYPE ||
            tokenType == CompactTokenTypes.WITNESS ||
            tokenType == CompactTokenTypes.HASH ||
            tokenType == CompactTokenTypes.LET
    ) {
      return pack(KEYWORD);
    }

    // Step 5: Built-in primitive and container types (Field, Boolean, Uint, Bytes, etc.)
    if (CompactTokenSets.BUILTIN_TYPES.contains(tokenType)) {
      return pack(TYPE);
    }

    // Step 6: Boolean constants (true, false)
    if (tokenType == CompactTokenTypes.TRUE || tokenType == CompactTokenTypes.FALSE) {
      return pack(CONSTANT);
    }

    // Step 7: String and version literals
    if (tokenType == CompactTokenTypes.STRING_LITERAL) {
      return pack(STRING);
    }
    if (tokenType == CompactTokenTypes.VERSION_LITERAL) {
      return pack(VERSION);
    }

    // Step 8: Numeric literals (Nat & binary/hex/dec integers)
    if (CompactTokenSets.NAT_LITERALS.contains(tokenType)) {
      return pack(NUMBER);
    }

    // Step 9: Comments (Block and line comments)
    if (tokenType == CompactTokenTypes.BLOCK_COMMENT) {
      return pack(BLOCK_COMMENT);
    }
    if (tokenType == CompactTokenTypes.LINE_COMMENT) {
      return pack(COMMENT);
    }

    // Step 10: Mathematical, logical, assignment, and structural operators
    if (
            tokenType == CompactTokenTypes.PLUS ||
            tokenType == CompactTokenTypes.MINUS ||
            tokenType == CompactTokenTypes.STAR ||
            tokenType == CompactTokenTypes.SLASH ||
            tokenType == CompactTokenTypes.PERCENT ||
            tokenType == CompactTokenTypes.ASSIGN ||
            tokenType == CompactTokenTypes.EQEQ ||
            tokenType == CompactTokenTypes.NEQ ||
            tokenType == CompactTokenTypes.LTE ||
            tokenType == CompactTokenTypes.GTE ||
            tokenType == CompactTokenTypes.LT ||
            tokenType == CompactTokenTypes.GT ||
            tokenType == CompactTokenTypes.NOT ||
            tokenType == CompactTokenTypes.ANDAND ||
            tokenType == CompactTokenTypes.OROR ||
            tokenType == CompactTokenTypes.ARROW ||
            tokenType == CompactTokenTypes.SPREAD ||
            tokenType == CompactTokenTypes.RANGE ||
            tokenType == CompactTokenTypes.PLUS_ASSIGN ||
            tokenType == CompactTokenTypes.MINUS_ASSIGN ||
            tokenType == CompactTokenTypes.QUESTION
    ) {
      return pack(OPERATOR);
    }

    // Step 11: Parentheses, braces, and brackets punctuation
    if (tokenType == CompactTokenTypes.LPAREN || tokenType == CompactTokenTypes.RPAREN) {
      return pack(PARENTHESES);
    }
    if (tokenType == CompactTokenTypes.LBRACE || tokenType == CompactTokenTypes.RBRACE) {
      return pack(BRACES);
    }
    if (tokenType == CompactTokenTypes.LBRACKET || tokenType == CompactTokenTypes.RBRACKET) {
      return pack(BRACKETS);
    }

    // Step 12: Delimiters (Comma, colon, semicolon, dot)
    if (tokenType == CompactTokenTypes.COMMA) {
      return pack(COMMA);
    }
    if (tokenType == CompactTokenTypes.COLON) {
      return pack(COLON);
    }
    if (tokenType == CompactTokenTypes.SEMICOLON) {
      return pack(SEMICOLON);
    }
    if (tokenType == CompactTokenTypes.DOT) {
      return pack(DOT);
    }

    // Step 13: Lexer error tokens and unterminated constructs
    if (tokenType == CompactTokenTypes.UNTERMINATED_STRING ||
        tokenType == CompactTokenTypes.UNTERMINATED_BLOCK_COMMENT ||
        tokenType == TokenType.BAD_CHARACTER) {
      return pack(BAD_CHARACTER);
    }

    // Step 14: Fallback empty array for unstyled tokens or whitespace
    return new TextAttributesKey[0];
  }
}
