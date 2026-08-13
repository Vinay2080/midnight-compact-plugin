package dev.verloren.midnight.lexer;

import com.intellij.psi.tree.TokenSet;

public final class CompactTokenSets {
  public static final TokenSet COMMENTS = TokenSet.create(
          CompactTokenTypes.LINE_COMMENT,
          CompactTokenTypes.BLOCK_COMMENT,
          CompactTokenTypes.UNTERMINATED_BLOCK_COMMENT
  );

  public static final TokenSet KEYWORDS = TokenSet.create(
          CompactTokenTypes.PRAGMA,
          CompactTokenTypes.EXPORT,
          CompactTokenTypes.FROM,
          CompactTokenTypes.IMPORT,
          CompactTokenTypes.MODULE,
          CompactTokenTypes.PREFIX,
          CompactTokenTypes.ASSERT,
          CompactTokenTypes.AS,
          CompactTokenTypes.CIRCUIT,
          CompactTokenTypes.CONST,
          CompactTokenTypes.CONSTRUCTOR,
          CompactTokenTypes.CONTRACT,
          CompactTokenTypes.DEFAULT,
          CompactTokenTypes.DISCLOSE,
          CompactTokenTypes.ELSE,
          CompactTokenTypes.ENUM,
          CompactTokenTypes.FOLD,
          CompactTokenTypes.FOR,
          CompactTokenTypes.IF,
          CompactTokenTypes.INCLUDE,
          CompactTokenTypes.LEDGER,
          CompactTokenTypes.MAP,
          CompactTokenTypes.NEW,
          CompactTokenTypes.OF,
          CompactTokenTypes.PAD,
          CompactTokenTypes.PURE,
          CompactTokenTypes.RETURN,
          CompactTokenTypes.SEALED,
          CompactTokenTypes.SLICE,
          CompactTokenTypes.STRUCT,
          CompactTokenTypes.TYPE,
          CompactTokenTypes.WITNESS,
          CompactTokenTypes.EMIT,
          CompactTokenTypes.IMPLEMENTS,
          CompactTokenTypes.EXTERNAL,
          CompactTokenTypes.LET,
          CompactTokenTypes.TRUE,
          CompactTokenTypes.FALSE
  );

  public static final TokenSet BUILTIN_TYPES = TokenSet.create(
          CompactTokenTypes.BOOLEAN_TYPE,
          CompactTokenTypes.BYTES_TYPE,
          CompactTokenTypes.FIELD_TYPE,
          CompactTokenTypes.OPAQUE_TYPE,
          CompactTokenTypes.UINT_TYPE,
          CompactTokenTypes.VECTOR_TYPE,
          CompactTokenTypes.JUBJUB_SCALAR_TYPE,
          CompactTokenTypes.SECP256K1_BASE_TYPE,
          CompactTokenTypes.SECP256K1_SCALAR_TYPE
  );

  public static final TokenSet LITERALS = TokenSet.create(
          CompactTokenTypes.TRUE,
          CompactTokenTypes.FALSE,
          CompactTokenTypes.VERSION_LITERAL,
          CompactTokenTypes.DECIMAL_LITERAL,
          CompactTokenTypes.BINARY_LITERAL,
          CompactTokenTypes.OCTAL_LITERAL,
          CompactTokenTypes.HEX_LITERAL,
          CompactTokenTypes.STRING_LITERAL
  );

  public static final TokenSet NAT_LITERALS = TokenSet.create(
          CompactTokenTypes.DECIMAL_LITERAL,
          CompactTokenTypes.BINARY_LITERAL,
          CompactTokenTypes.OCTAL_LITERAL,
          CompactTokenTypes.HEX_LITERAL
  );

  public static final TokenSet OPERATORS = TokenSet.create(
          CompactTokenTypes.ASSIGN,
          CompactTokenTypes.PLUS_ASSIGN,
          CompactTokenTypes.MINUS_ASSIGN,
          CompactTokenTypes.PLUS,
          CompactTokenTypes.MINUS,
          CompactTokenTypes.STAR,
          CompactTokenTypes.SLASH,
          CompactTokenTypes.PERCENT,
          CompactTokenTypes.EQEQ,
          CompactTokenTypes.NEQ,
          CompactTokenTypes.LT,
          CompactTokenTypes.LTE,
          CompactTokenTypes.GT,
          CompactTokenTypes.GTE,
          CompactTokenTypes.ARROW,
          CompactTokenTypes.NOT,
          CompactTokenTypes.ANDAND,
          CompactTokenTypes.OROR,
          CompactTokenTypes.RANGE,
          CompactTokenTypes.DOT,
          CompactTokenTypes.SPREAD,
          CompactTokenTypes.QUESTION
  );

  public static final TokenSet ASSIGNMENT_OPERATORS = TokenSet.create(
          CompactTokenTypes.ASSIGN,
          CompactTokenTypes.PLUS_ASSIGN,
          CompactTokenTypes.MINUS_ASSIGN
  );

  private CompactTokenSets() {
  }
}