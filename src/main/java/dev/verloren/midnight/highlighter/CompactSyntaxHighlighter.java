package dev.verloren.midnight.highlighter;

import com.intellij.lexer.Lexer;


import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.HighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import dev.verloren.midnight.lexer.CompactLexer;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import org.jspecify.annotations.NonNull;

public class CompactSyntaxHighlighter extends SyntaxHighlighterBase {

  public static final TextAttributesKey KEYWORD = TextAttributesKey.createTextAttributesKey("COMPACT_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD);
  public static final TextAttributesKey TYPE = TextAttributesKey.createTextAttributesKey("COMPACT_TYPE", DefaultLanguageHighlighterColors.CLASS_NAME);
  public static final TextAttributesKey STRING = TextAttributesKey.createTextAttributesKey("COMPACT_STRING", DefaultLanguageHighlighterColors.STRING);
  public static final TextAttributesKey NUMBER = TextAttributesKey.createTextAttributesKey("COMPACT_NUMBER", DefaultLanguageHighlighterColors.NUMBER);
  public static final TextAttributesKey COMMENT = TextAttributesKey.createTextAttributesKey("COMPACT_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT);
  public static final TextAttributesKey BAD_CHARACTER = TextAttributesKey.createTextAttributesKey("COMPACT_BAD_CHARACTER", HighlighterColors.BAD_CHARACTER);
  public static final TextAttributesKey RESERVED_KEYWORD = TextAttributesKey.createTextAttributesKey("COMPACT_RESERVED_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD);
  public static final TextAttributesKey CONSTANT = TextAttributesKey.createTextAttributesKey("COMPACT_CONSTANT", DefaultLanguageHighlighterColors.CONSTANT);
  public static final TextAttributesKey VERSION = TextAttributesKey.createTextAttributesKey("COMPACT_VERSION", DefaultLanguageHighlighterColors.NUMBER);
  public static final TextAttributesKey OPERATOR = TextAttributesKey.createTextAttributesKey("COMPACT_OPERATOR", DefaultLanguageHighlighterColors.OPERATION_SIGN);
  public static final TextAttributesKey PARENTHESES = TextAttributesKey.createTextAttributesKey("COMPACT_PARENTHESES", DefaultLanguageHighlighterColors.PARENTHESES);
  public static final TextAttributesKey BRACES = TextAttributesKey.createTextAttributesKey("COMPACT_BRACES", DefaultLanguageHighlighterColors.BRACES);
  public static final TextAttributesKey BRACKETS = TextAttributesKey.createTextAttributesKey("COMPACT_BRACKETS", DefaultLanguageHighlighterColors.BRACKETS);
  public static final TextAttributesKey COMMA = TextAttributesKey.createTextAttributesKey("COMPACT_COMMA", DefaultLanguageHighlighterColors.COMMA);
  public static final TextAttributesKey SEMICOLON = TextAttributesKey.createTextAttributesKey("COMPACT_SEMICOLON", DefaultLanguageHighlighterColors.SEMICOLON);
  public static final TextAttributesKey DOT = TextAttributesKey.createTextAttributesKey("COMPACT_DOT", DefaultLanguageHighlighterColors.DOT);

  @Override
  public @NonNull Lexer getHighlightingLexer() {
    return new CompactLexer();
  }

  @Override
  public TextAttributesKey @NonNull [] getTokenHighlights(IElementType tokenType) {
    if (
            tokenType == CompactTokenTypes.EXPORT ||
                    tokenType == CompactTokenTypes.IMPORT ||
                    tokenType == CompactTokenTypes.FROM ||
                    tokenType == CompactTokenTypes.MODULE ||
                    tokenType == CompactTokenTypes.PREFIX ||
                    tokenType == CompactTokenTypes.ASSERT ||
                    tokenType == CompactTokenTypes.AS ||
                    tokenType == CompactTokenTypes.CIRCUIT ||
                    tokenType == CompactTokenTypes.CONST ||
                    tokenType == CompactTokenTypes.CONSTRUCTOR ||
                    tokenType == CompactTokenTypes.CONTRACT ||
                    tokenType == CompactTokenTypes.DEFAULT ||
                    tokenType == CompactTokenTypes.DISCLOSE ||
                    tokenType == CompactTokenTypes.ELSE ||
                    tokenType == CompactTokenTypes.ENUM ||
                    tokenType == CompactTokenTypes.FOLD ||
                    tokenType == CompactTokenTypes.FOR ||
                    tokenType == CompactTokenTypes.IF ||
                    tokenType == CompactTokenTypes.INCLUDE ||
                    tokenType == CompactTokenTypes.LEDGER ||
                    tokenType == CompactTokenTypes.MAP ||
                    tokenType == CompactTokenTypes.NEW ||
                    tokenType == CompactTokenTypes.OF ||
                    tokenType == CompactTokenTypes.PAD ||
                    tokenType == CompactTokenTypes.PRAGMA ||
                    tokenType == CompactTokenTypes.PURE ||
                    tokenType == CompactTokenTypes.RETURN ||
                    tokenType == CompactTokenTypes.SEALED ||
                    tokenType == CompactTokenTypes.SLICE ||
                    tokenType == CompactTokenTypes.STRUCT ||
                    tokenType == CompactTokenTypes.TYPE ||
                    tokenType == CompactTokenTypes.WITNESS ||
                    tokenType == CompactTokenTypes.EMIT ||
                    tokenType == CompactTokenTypes.IMPLEMENTS ||
                    tokenType == CompactTokenTypes.EXTERNAL
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

    if (tokenType == CompactTokenTypes.LINE_COMMENT || tokenType == CompactTokenTypes.BLOCK_COMMENT) {
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
                    tokenType == CompactTokenTypes.MINUS_ASSIGN
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
