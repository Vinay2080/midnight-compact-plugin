package dev.verloren.midnight.lexer;


import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;

/**
 * Static registry of all lexical token types recognised in Compact source code.
 *
 * <p>Contains definitions for:
 * <ul>
 *   <li><b>Keywords:</b> Compact language keywords such as {@code circuit}, {@code witness}, {@code ledger}, {@code struct}, {@code enum}, {@code contract}.</li>
 *   <li><b>Builtin Types:</b> Primitive type keywords such as {@code Boolean}, {@code Field}, {@code Uint}, {@code Bytes}, {@code Vector}.</li>
 *   <li><b>Literals:</b> Boolean, numeric (decimal, hex, binary, octal), version, and string literals.</li>
 *   <li><b>Operators & Delimiters:</b> Arithmetic, relational, logical, assignment, and punctuation tokens.</li>
 *   <li><b>Comments & Whitespace:</b> Line and block comments, whitespace, and lexical error tokens.</li>
 * </ul>
 * </p>
 */
public final class CompactTokenTypes {

  public static final IElementType INVALID_VERSION = new CompactTokenType("INVALID_VERSION");


  // =========================================================
  // Keywords
  // =========================================================
  public static final IElementType PRAGMA = new CompactTokenType("PRAGMA");
  public static final IElementType EXPORT = new CompactTokenType("EXPORT");
  public static final IElementType FROM = new CompactTokenType("FROM");
  public static final IElementType IMPORT = new CompactTokenType("IMPORT");
  public static final IElementType MODULE = new CompactTokenType("MODULE");
  public static final IElementType PREFIX = new CompactTokenType("PREFIX");
  public static final IElementType ASSERT = new CompactTokenType("ASSERT");
  public static final IElementType AS = new CompactTokenType("AS");
  public static final IElementType CIRCUIT = new CompactTokenType("CIRCUIT");
  public static final IElementType CONST = new CompactTokenType("CONST");
  public static final IElementType CONSTRUCTOR = new CompactTokenType("CONSTRUCTOR");
  public static final IElementType CONTRACT = new CompactTokenType("CONTRACT");
  public static final IElementType DEFAULT = new CompactTokenType("DEFAULT");
  public static final IElementType DISCLOSE = new CompactTokenType("DISCLOSE");
  public static final IElementType ELSE = new CompactTokenType("ELSE");
  public static final IElementType ENUM = new CompactTokenType("ENUM");
  public static final IElementType FOLD = new CompactTokenType("FOLD");
  public static final IElementType FOR = new CompactTokenType("FOR");
  public static final IElementType IF = new CompactTokenType("IF");
  public static final IElementType INCLUDE = new CompactTokenType("INCLUDE");
  public static final IElementType LEDGER = new CompactTokenType("LEDGER");
  public static final IElementType MAP = new CompactTokenType("MAP");
  public static final IElementType NEW = new CompactTokenType("NEW");
  public static final IElementType OF = new CompactTokenType("OF");
  public static final IElementType PAD = new CompactTokenType("PAD");
  public static final IElementType PURE = new CompactTokenType("PURE");
  public static final IElementType RETURN = new CompactTokenType("RETURN");
  public static final IElementType SEALED = new CompactTokenType("SEALED");
  public static final IElementType SLICE = new CompactTokenType("SLICE");
  public static final IElementType STRUCT = new CompactTokenType("STRUCT");
  public static final IElementType TYPE = new CompactTokenType("TYPE");
  public static final IElementType WITNESS = new CompactTokenType("WITNESS");
  public static final IElementType EMIT = new CompactTokenType("EMIT");
  public static final IElementType IMPLEMENTS = new CompactTokenType("IMPLEMENTS");
  public static final IElementType EXTERNAL = new CompactTokenType("EXTERNAL");
  public static final IElementType HASH = new CompactTokenType("HASH");

  // =========================================================
  // Built-in Types
  // =========================================================

  public static final IElementType BOOLEAN_TYPE = new CompactTokenType("BOOLEAN_TYPE");
  public static final IElementType BYTES_TYPE = new CompactTokenType("BYTES_TYPE");
  public static final IElementType FIELD_TYPE = new CompactTokenType("FIELD_TYPE");
  public static final IElementType OPAQUE_TYPE = new CompactTokenType("OPAQUE_TYPE");
  public static final IElementType UINT_TYPE = new CompactTokenType("UINT_TYPE");
  public static final IElementType VECTOR_TYPE = new CompactTokenType("VECTOR_TYPE");
  public static final IElementType JUBJUB_SCALAR_TYPE = new CompactTokenType("JUBJUB_SCALAR_TYPE");
  public static final IElementType SECP256K1_BASE_TYPE = new CompactTokenType("SECP256K1_BASE_TYPE");
  public static final IElementType SECP256K1_SCALAR_TYPE = new CompactTokenType("SECP256K1_SCALAR_TYPE");

// =========================================================
// Literals
// =========================================================

  public static final IElementType TRUE = new CompactTokenType("TRUE");
  public static final IElementType FALSE = new CompactTokenType("FALSE");

  public static final IElementType VERSION_LITERAL = new CompactTokenType("VERSION_LITERAL");
  public static final IElementType DECIMAL_LITERAL = new CompactTokenType("DECIMAL_LITERAL");
  public static final IElementType BINARY_LITERAL = new CompactTokenType("BINARY_LITERAL");
  public static final IElementType OCTAL_LITERAL = new CompactTokenType("OCTAL_LITERAL");
  public static final IElementType HEX_LITERAL = new CompactTokenType("HEX_LITERAL");
  public static final IElementType STRING_LITERAL = new CompactTokenType("STRING_LITERAL");

  public static final IElementType IDENTIFIER = new CompactTokenType("IDENTIFIER");

  // =========================================================
  // Operators
  // =========================================================

  public static final IElementType ASSIGN = new CompactTokenType("ASSIGN");

  public static final IElementType PLUS_ASSIGN = new CompactTokenType("PLUS_ASSIGN");
  public static final IElementType MINUS_ASSIGN = new CompactTokenType("MINUS_ASSIGN");

  public static final IElementType PLUS = new CompactTokenType("PLUS");
  public static final IElementType MINUS = new CompactTokenType("MINUS");
  public static final IElementType STAR = new CompactTokenType("STAR");
  public static final IElementType SLASH = new CompactTokenType("SLASH");
  public static final IElementType PERCENT = new CompactTokenType("PERCENT");

  public static final IElementType EQEQ = new CompactTokenType("EQEQ");
  public static final IElementType NEQ = new CompactTokenType("NEQ");

  public static final IElementType LT = new CompactTokenType("LT");
  public static final IElementType LTE = new CompactTokenType("LTE");
  public static final IElementType GT = new CompactTokenType("GT");
  public static final IElementType GTE = new CompactTokenType("GTE");

  public static final IElementType ARROW = new CompactTokenType("ARROW");

  public static final IElementType NOT = new CompactTokenType("NOT");
  public static final IElementType ANDAND = new CompactTokenType("ANDAND");
  public static final IElementType OROR = new CompactTokenType("OROR");

  public static final IElementType RANGE = new CompactTokenType("RANGE");
  public static final IElementType DOT = new CompactTokenType("DOT");
  public static final IElementType SPREAD = new CompactTokenType("SPREAD");
  // =========================================================
// Delimiters
// =========================================================

  public static final IElementType LPAREN = new CompactTokenType("LPAREN");
  public static final IElementType RPAREN = new CompactTokenType("RPAREN");

  public static final IElementType LBRACE = new CompactTokenType("LBRACE");
  public static final IElementType RBRACE = new CompactTokenType("RBRACE");

  public static final IElementType LBRACKET = new CompactTokenType("LBRACKET");
  public static final IElementType RBRACKET = new CompactTokenType("RBRACKET");

  public static final IElementType COMMA = new CompactTokenType("COMMA");
  public static final IElementType COLON = new CompactTokenType("COLON");
  public static final IElementType SEMICOLON = new CompactTokenType("SEMICOLON");

// =========================================================
// Comments
// =========================================================

  public static final IElementType LINE_COMMENT = new CompactTokenType("LINE_COMMENT");
  public static final IElementType BLOCK_COMMENT = new CompactTokenType("BLOCK_COMMENT");

// =========================================================
// Whitespace & Special
// =========================================================

  public static final IElementType WHITE_SPACE = TokenType.WHITE_SPACE;
  public static final IElementType BAD_CHARACTER = TokenType.BAD_CHARACTER;
  public static final IElementType UNTERMINATED_BLOCK_COMMENT =
          new CompactTokenType("UNTERMINATED_BLOCK_COMMENT");

  public static final IElementType UNTERMINATED_STRING =
          new CompactTokenType("UNTERMINATED_STRING");

  public static final IElementType LET = new CompactTokenType("LET");
  public static final IElementType QUESTION = new CompactTokenType("QUESTION");

  private CompactTokenTypes() {
  }
}
