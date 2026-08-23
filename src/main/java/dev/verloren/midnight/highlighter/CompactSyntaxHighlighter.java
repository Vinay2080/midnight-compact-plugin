package dev.verloren.midnight.highlighter;

import com.intellij.lexer.Lexer;


import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import dev.verloren.midnight.lexer.CompactLexer;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import org.jspecify.annotations.NonNull;

/**
 * Syntax highlighter for Compact source code.
 *
 * <p>Maps lexical tokens from {@link CompactLexer} to IntelliJ {@link TextAttributesKey}s
 * for editor syntax highlighting.</p>
 */
public class CompactSyntaxHighlighter extends SyntaxHighlighterBase {

  public static final TextAttributesKey KEYWORD = CompactHighlighterColors.KEYWORD;
  public static final TextAttributesKey TYPE = CompactHighlighterColors.BUILTIN_TYPE;
  public static final TextAttributesKey STRING = CompactHighlighterColors.STRING;
  public static final TextAttributesKey NUMBER = CompactHighlighterColors.NUMBER;
  public static final TextAttributesKey COMMENT = CompactHighlighterColors.LINE_COMMENT;
  public static final TextAttributesKey BAD_CHARACTER = CompactHighlighterColors.BAD_CHARACTER;
  public static final TextAttributesKey RESERVED_KEYWORD = CompactHighlighterColors.RESERVED_KEYWORD;
  public static final TextAttributesKey CONSTANT = CompactHighlighterColors.BOOLEAN;
  public static final TextAttributesKey VERSION = CompactHighlighterColors.VERSION;
  public static final TextAttributesKey OPERATOR = CompactHighlighterColors.OPERATOR;
  public static final TextAttributesKey PARENTHESES = CompactHighlighterColors.PARENTHESES;
  public static final TextAttributesKey BRACES = CompactHighlighterColors.BRACES;
  public static final TextAttributesKey BRACKETS = CompactHighlighterColors.BRACKETS;
  public static final TextAttributesKey COMMA = CompactHighlighterColors.COMMA;
  public static final TextAttributesKey SEMICOLON = CompactHighlighterColors.SEMICOLON;
  public static final TextAttributesKey DOT = CompactHighlighterColors.DOT;
  public static final TextAttributesKey COLON = CompactHighlighterColors.COLON;
  public static final TextAttributesKey PRAGMA = CompactHighlighterColors.PRAGMA;
  public static final TextAttributesKey DOC_COMMENT = CompactHighlighterColors.DOC_COMMENT;
  public static final TextAttributesKey BLOCK_COMMENT = CompactHighlighterColors.BLOCK_COMMENT;

  @Override
  public @NonNull Lexer getHighlightingLexer() {
    return new CompactLexer();
  }

  @Override
  public TextAttributesKey @NonNull [] getTokenHighlights(IElementType tokenType) {
    if (tokenType == CompactTokenTypes.EXPORT ||
        tokenType == CompactTokenTypes.PURE ||
        tokenType == CompactTokenTypes.SEALED ||
        tokenType == CompactTokenTypes.NEW ||
        tokenType == CompactTokenTypes.IMPLEMENTS ||
        tokenType == CompactTokenTypes.EXTERNAL) {
      return pack(CompactHighlighterColors.MODIFIER);
    }

    if (tokenType == CompactTokenTypes.PRAGMA) {
      return pack(PRAGMA);
    }

    if (tokenType == CompactTokenTypes.ASSERT ||
        tokenType == CompactTokenTypes.DISCLOSE ||
        tokenType == CompactTokenTypes.FOLD ||
        tokenType == CompactTokenTypes.SLICE ||
        tokenType == CompactTokenTypes.PAD ||
        tokenType == CompactTokenTypes.EMIT ||
        tokenType == CompactTokenTypes.MAP) {
      return pack(CompactHighlighterColors.BUILTIN_FUNCTION);
    }

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

    if (tokenType == CompactTokenTypes.BOOLEAN_TYPE ||
            tokenType == CompactTokenTypes.BYTES_TYPE ||
            tokenType == CompactTokenTypes.FIELD_TYPE ||
            tokenType == CompactTokenTypes.OPAQUE_TYPE ||
            tokenType == CompactTokenTypes.UINT_TYPE ||
            tokenType == CompactTokenTypes.VECTOR_TYPE ||
            tokenType == CompactTokenTypes.JUBJUB_SCALAR_TYPE ||
            tokenType == CompactTokenTypes.SECP256K1_BASE_TYPE ||
            tokenType == CompactTokenTypes.SECP256K1_SCALAR_TYPE) {
      return pack(TYPE);
    }

    if (tokenType == CompactTokenTypes.TRUE || tokenType == CompactTokenTypes.FALSE) {
      return pack(CONSTANT);
    }

    if (tokenType == CompactTokenTypes.STRING_LITERAL) {
      return pack(STRING);
    }

    if (tokenType == CompactTokenTypes.VERSION_LITERAL) {
      return pack(VERSION);
    }

    if (tokenType == CompactTokenTypes.DECIMAL_LITERAL ||
            tokenType == CompactTokenTypes.BINARY_LITERAL ||
            tokenType == CompactTokenTypes.OCTAL_LITERAL ||
            tokenType == CompactTokenTypes.HEX_LITERAL) {
      return pack(NUMBER);
    }

    if (tokenType == CompactTokenTypes.BLOCK_COMMENT) {
      return pack(BLOCK_COMMENT);
    }
    if (tokenType == CompactTokenTypes.LINE_COMMENT) {
      return pack(COMMENT);
    }

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

    if (tokenType == CompactTokenTypes.LPAREN || tokenType == CompactTokenTypes.RPAREN) {
      return pack(PARENTHESES);
    }

    if (tokenType == CompactTokenTypes.LBRACE || tokenType == CompactTokenTypes.RBRACE) {
      return pack(BRACES);
    }
    if (tokenType == CompactTokenTypes.LBRACKET || tokenType == CompactTokenTypes.RBRACKET) {
      return pack(BRACKETS);
    }

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

    if (tokenType == CompactTokenTypes.UNTERMINATED_STRING ||
            tokenType == CompactTokenTypes.UNTERMINATED_BLOCK_COMMENT ||
            tokenType == TokenType.BAD_CHARACTER) {
      return pack(BAD_CHARACTER);
    }

    return new TextAttributesKey[0];
  }
}
