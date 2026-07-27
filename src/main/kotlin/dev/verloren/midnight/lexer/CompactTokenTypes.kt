package dev.verloren.midnight.lexer

import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType

object CompactTokenTypes {

    // =========================================================
    // Keywords
    // =========================================================

    @JvmField
    val EXPORT = CompactTokenType("EXPORT")

    @JvmField
    val FROM = CompactTokenType("FROM")

    @JvmField
    val IMPORT = CompactTokenType("IMPORT")

    @JvmField
    val MODULE = CompactTokenType("MODULE")

    @JvmField
    val PREFIX = CompactTokenType("PREFIX")

    @JvmField
    val ASSERT = CompactTokenType("ASSERT")

    @JvmField
    val AS = CompactTokenType("AS")

    @JvmField
    val CIRCUIT = CompactTokenType("CIRCUIT")

    @JvmField
    val CONST = CompactTokenType("CONST")

    @JvmField
    val CONSTRUCTOR = CompactTokenType("CONSTRUCTOR")

    @JvmField
    val CONTRACT = CompactTokenType("CONTRACT")

    @JvmField
    val DEFAULT = CompactTokenType("DEFAULT")

    @JvmField
    val DISCLOSE = CompactTokenType("DISCLOSE")

    @JvmField
    val ELSE = CompactTokenType("ELSE")

    @JvmField
    val ENUM = CompactTokenType("ENUM")

    @JvmField
    val FOLD = CompactTokenType("FOLD")

    @JvmField
    val FOR = CompactTokenType("FOR")

    @JvmField
    val IF = CompactTokenType("IF")

    @JvmField
    val INCLUDE = CompactTokenType("INCLUDE")

    @JvmField
    val LEDGER = CompactTokenType("LEDGER")

    @JvmField
    val MAP = CompactTokenType("MAP")

    @JvmField
    val NEW = CompactTokenType("NEW")

    @JvmField
    val OF = CompactTokenType("OF")

    @JvmField
    val PAD = CompactTokenType("PAD")

    @JvmField
    val PRAGMA = CompactTokenType("PRAGMA")

    @JvmField
    val PURE = CompactTokenType("PURE")

    @JvmField
    val RETURN = CompactTokenType("RETURN")

    @JvmField
    val SEALED = CompactTokenType("SEALED")

    @JvmField
    val SLICE = CompactTokenType("SLICE")

    @JvmField
    val STRUCT = CompactTokenType("STRUCT")

    @JvmField
    val TYPE = CompactTokenType("TYPE")

    @JvmField
    val WITNESS = CompactTokenType("WITNESS")

    // =========================================================
    // Built-in Types
    // =========================================================

    @JvmField
    val BOOLEAN_TYPE = CompactTokenType("BOOLEAN_TYPE")

    @JvmField
    val BYTES_TYPE = CompactTokenType("BYTES_TYPE")

    @JvmField
    val FIELD_TYPE = CompactTokenType("FIELD_TYPE")

    @JvmField
    val OPAQUE_TYPE = CompactTokenType("OPAQUE_TYPE")

    @JvmField
    val UINT_TYPE = CompactTokenType("UINT_TYPE")

    @JvmField
    val VECTOR_TYPE = CompactTokenType("VECTOR_TYPE")

    // =========================================================
    // Literals
    // =========================================================

    @JvmField
    val TRUE = CompactTokenType("TRUE")

    @JvmField
    val FALSE = CompactTokenType("FALSE")

    @JvmField
    val VERSION_LITERAL = CompactTokenType("VERSION_LITERAL")

    @JvmField
    val DECIMAL_LITERAL = CompactTokenType("DECIMAL_LITERAL")

    @JvmField
    val BINARY_LITERAL = CompactTokenType("BINARY_LITERAL")

    @JvmField
    val OCTAL_LITERAL = CompactTokenType("OCTAL_LITERAL")

    @JvmField
    val HEX_LITERAL = CompactTokenType("HEX_LITERAL")


    @JvmField
    val STRING_LITERAL = CompactTokenType("STRING_LITERAL")

    @JvmField
    val IDENTIFIER = CompactTokenType("IDENTIFIER")

    // =========================================================
    // Reserved Keywords
    // =========================================================

    @JvmField
    val RESERVED_KEYWORD = CompactTokenType("RESERVED_KEYWORD")

    // =========================================================
    // Operators
    // =========================================================

    @JvmField
    val ASSIGN = CompactTokenType("=")

    @JvmField
    val PLUS_ASSIGN = CompactTokenType("+=")

    @JvmField
    val MINUS_ASSIGN = CompactTokenType("-=")

    @JvmField
    val PLUS = CompactTokenType("+")

    @JvmField
    val MINUS = CompactTokenType("-")

    @JvmField
    val STAR = CompactTokenType("*")

    @JvmField
    val SLASH = CompactTokenType("/")

    @JvmField
    val PERCENT = CompactTokenType("%")

    @JvmField
    val EQEQ = CompactTokenType("==")

    @JvmField
    val NEQ = CompactTokenType("!=")

    @JvmField
    val LT = CompactTokenType("<")

    @JvmField
    val LTE = CompactTokenType("<=")

    @JvmField
    val GT = CompactTokenType(">")

    @JvmField
    val GTE = CompactTokenType(">=")

    @JvmField
    val ARROW = CompactTokenType("=>")

    @JvmField
    val NOT = CompactTokenType("!")

    @JvmField
    val ANDAND = CompactTokenType("&&")

    @JvmField
    val OROR = CompactTokenType("||")

    @JvmField
    val RANGE = CompactTokenType("..")

    @JvmField
    val DOT = CompactTokenType(".")

    @JvmField
    val SPREAD = CompactTokenType("...")

    @JvmField
    val QUESTION = CompactTokenType("?")

    @JvmField
    val COLON = CompactTokenType(":")

    // =========================================================
    // Delimiters
    // =========================================================

    @JvmField
    val LPAREN = CompactTokenType("(")

    @JvmField
    val RPAREN = CompactTokenType(")")

    @JvmField
    val LBRACE = CompactTokenType("{")

    @JvmField
    val RBRACE = CompactTokenType("}")

    @JvmField
    val LBRACKET = CompactTokenType("[")

    @JvmField
    val RBRACKET = CompactTokenType("]")

    @JvmField
    val COMMA = CompactTokenType(",")

    @JvmField
    val SEMICOLON = CompactTokenType(";")

    @JvmField
    val HASH = CompactTokenType("#")

    // =========================================================
    // Comments
    // =========================================================

    @JvmField
    val LINE_COMMENT = CompactTokenType("LINE_COMMENT")

    @JvmField
    val BLOCK_COMMENT = CompactTokenType("BLOCK_COMMENT")

    // =========================================================
    // Whitespace & Special
    // =========================================================

    @JvmField
    val WHITE_SPACE: IElementType = TokenType.WHITE_SPACE

    @JvmField
    val BAD_CHARACTER: IElementType = TokenType.BAD_CHARACTER

    @JvmField
    val UNTERMINATED_STRING = CompactTokenType("UNTERMINATED_STRING")

    @JvmField
    val UNTERMINATED_BLOCK_COMMENT =
        CompactTokenType("UNTERMINATED_BLOCK_COMMENT")
}
