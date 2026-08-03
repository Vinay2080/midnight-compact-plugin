package dev.verloren.midnight.lexer;


import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;

public final class CompactTokenTypes {
  public static final IElementType PRAGMA = new CompactTokenType("PRAGMA");
  public static final IElementType IDENTIFIER = new CompactTokenType("IDENTIFIER");
  public static final IElementType VERSION = new CompactTokenType("VERSION");
  public static final IElementType SEMICOLON = new CompactTokenType("SEMICOLON");
  public static final IElementType GT = new CompactTokenType("GT");
  public static final IElementType GTE = new CompactTokenType("GTE");
  public static final IElementType LT = new CompactTokenType("LT");
  public static final IElementType LTE = new CompactTokenType("LTE");

  public static final IElementType NOT = new CompactTokenType("NOT");
  public static final IElementType AND = new CompactTokenType("AND");
  public static final IElementType OR = new CompactTokenType("OR");

  public static final IElementType LPAREN = new CompactTokenType("LPAREN");
  public static final IElementType RPAREN = new CompactTokenType("RPAREN");


  public static final IElementType BAD_CHARACTER = TokenType.BAD_CHARACTER;
  public static final IElementType INVALID_VERSION = new CompactTokenType("INVALID_VERSION");

  //
//    // =========================================================
//    // Keywords
//    // =========================================================
//
//    public static final IElementType EXPORT = CompactTypes.EXPORT;
//    public static final IElementType FROM = CompactTypes.FROM;
//    public static final IElementType IMPORT = CompactTypes.IMPORT;
//    public static final IElementType MODULE = CompactTypes.MODULE;
//    public static final IElementType PREFIX = CompactTypes.PREFIX;
//    public static final IElementType ASSERT = CompactTypes.ASSERT;
//    public static final IElementType AS = CompactTypes.AS;
//    public static final IElementType CIRCUIT = CompactTypes.CIRCUIT;
//    public static final IElementType CONST = CompactTypes.CONST;
//    public static final IElementType CONSTRUCTOR = CompactTypes.CONSTRUCTOR;
//    public static final IElementType CONTRACT = CompactTypes.CONTRACT;
//    public static final IElementType DEFAULT = CompactTypes.DEFAULT;
//    public static final IElementType DISCLOSE = CompactTypes.DISCLOSE;
//    public static final IElementType ELSE = CompactTypes.ELSE;
//    public static final IElementType ENUM = CompactTypes.ENUM;
//    public static final IElementType FOLD = CompactTypes.FOLD;
//    public static final IElementType FOR = CompactTypes.FOR;
//    public static final IElementType IF = CompactTypes.IF;
//    public static final IElementType INCLUDE = CompactTypes.INCLUDE;
//    public static final IElementType LEDGER = CompactTypes.LEDGER;
//    public static final IElementType MAP = CompactTypes.MAP;
//    public static final IElementType NEW = CompactTypes.NEW;
//    public static final IElementType OF = CompactTypes.OF;
//    public static final IElementType PAD = CompactTypes.PAD;
//    public static final IElementType PRAGMA = CompactTypes.PRAGMA;
//    public static final IElementType PURE = CompactTypes.PURE;
//    public static final IElementType RETURN = CompactTypes.RETURN;
//    public static final IElementType SEALED = CompactTypes.SEALED;
//    public static final IElementType SLICE = CompactTypes.SLICE;
//    public static final IElementType STRUCT = CompactTypes.STRUCT;
//    public static final IElementType TYPE = CompactTypes.TYPE;
//    public static final IElementType WITNESS = CompactTypes.WITNESS;
//    public static final IElementType EMIT = CompactTypes.EMIT;
//
//    // =========================================================
//    // Built-in Types
//    // =========================================================
//
//    public static final IElementType BOOLEAN_TYPE = CompactTypes.BOOLEAN_TYPE;
//    public static final IElementType BYTES_TYPE = CompactTypes.BYTES_TYPE;
//    public static final IElementType FIELD_TYPE = CompactTypes.FIELD_TYPE;
//    public static final IElementType OPAQUE_TYPE = CompactTypes.OPAQUE_TYPE;
//    public static final IElementType UINT_TYPE = CompactTypes.UINT_TYPE;
//    public static final IElementType VECTOR_TYPE = CompactTypes.VECTOR_TYPE;
//    public static final IElementType JUBJUB_SCALAR_TYPE = CompactTypes.JUBJUB_SCALAR_TYPE;
//    public static final IElementType SECP256K1_BASE_TYPE = CompactTypes.SECP256K1_BASE_TYPE;
//    public static final IElementType SECP256K1_SCALAR_TYPE = CompactTypes.SECP256K1_SCALAR_TYPE;
//
//    // =========================================================
//    // Literals
//    // =========================================================
//
//    public static final IElementType TRUE = CompactTypes.TRUE;
//    public static final IElementType FALSE = CompactTypes.FALSE;
//    public static final IElementType VERSION_LITERAL = CompactTypes.VERSION_LITERAL;
//    public static final IElementType DECIMAL_LITERAL = CompactTypes.DECIMAL_LITERAL;
//    public static final IElementType BINARY_LITERAL = CompactTypes.BINARY_LITERAL;
//    public static final IElementType OCTAL_LITERAL = CompactTypes.OCTAL_LITERAL;
//    public static final IElementType HEX_LITERAL = CompactTypes.HEX_LITERAL;
//    public static final IElementType STRING_LITERAL = CompactTypes.STRING_LITERAL;
//    public static final IElementType IDENTIFIER = CompactTypes.IDENTIFIER;
//
//    // =========================================================
//    // Operators
//    // =========================================================
//
//    public static final IElementType ASSIGN = CompactTypes.ASSIGN;
//    public static final IElementType PLUS_ASSIGN = CompactTypes.PLUS_ASSIGN;
//    public static final IElementType MINUS_ASSIGN = CompactTypes.MINUS_ASSIGN;
//    public static final IElementType PLUS = CompactTypes.PLUS;
//    public static final IElementType MINUS = CompactTypes.MINUS;
//    public static final IElementType STAR = CompactTypes.STAR;
//
//    // Any BNF rule does not reference SLASH and PERCENT, so Grammar-Kit
//    // never generates a corresponding constant in CompactTypes - these two
//    // remain the lexer's own instances.
//    public static final IElementType SLASH = new CompactTokenType("SLASH");
//    public static final IElementType PERCENT = new CompactTokenType("PERCENT");
//
//    public static final IElementType EQEQ = CompactTypes.EQEQ;
//    public static final IElementType NEQ = CompactTypes.NOTEQ;
//    public static final IElementType LT = CompactTypes.LT;
//    public static final IElementType LTE = CompactTypes.LTE;
//    public static final IElementType GT = CompactTypes.GT;
//    public static final IElementType GTE = CompactTypes.GTE;
//    public static final IElementType ARROW = CompactTypes.ARROW;
//    public static final IElementType NOT = CompactTypes.NOT;
//    public static final IElementType ANDAND = CompactTypes.ANDAND;
//    public static final IElementType OROR = CompactTypes.OROR;
//    public static final IElementType RANGE = CompactTypes.RANGE;
//    public static final IElementType DOT = CompactTypes.DOT;
//    public static final IElementType SPREAD = CompactTypes.SPREAD;
//
//    // =========================================================
//    // Delimiters
//    // =========================================================
//
//    public static final IElementType LPAREN = CompactTypes.LPAREN;
//    public static final IElementType RPAREN = CompactTypes.RPAREN;
//    public static final IElementType LBRACE = CompactTypes.LBRACE;
//    public static final IElementType RBRACE = CompactTypes.RBRACE;
//    public static final IElementType LBRACKET = CompactTypes.LBRACKET;
//    public static final IElementType RBRACKET = CompactTypes.RBRACKET;
//    public static final IElementType COMMA = CompactTypes.COMMA;
//    public static final IElementType SEMICOLON = CompactTypes.SEMICOLON;
//
//    // =========================================================
//    // Comments
//    // =========================================================
//
//    public static final IElementType LINE_COMMENT = new CompactTokenType("LINE_COMMENT");
//    public static final IElementType BLOCK_COMMENT = new CompactTokenType("BLOCK_COMMENT");
//
//    // =========================================================
//    // Whitespace & Special
//    // =========================================================
//
//    public static final IElementType WHITE_SPACE = TokenType.WHITE_SPACE;
//    public static final IElementType BAD_CHARACTER = TokenType.BAD_CHARACTER;
//    public static final IElementType UNTERMINATED_STRING = new CompactTokenType("UNTERMINATED_STRING");
//    public static final IElementType UNTERMINATED_BLOCK_COMMENT = new CompactTokenType("UNTERMINATED_BLOCK_COMMENT");
//
  private CompactTokenTypes() {
  }
}
