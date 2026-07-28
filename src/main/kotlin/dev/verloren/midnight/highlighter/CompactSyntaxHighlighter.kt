package dev.verloren.midnight.highlighter

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import dev.verloren.midnight.lexer.CompactLexerAdapter
import dev.verloren.midnight.lexer.CompactTokenTypes

class CompactSyntaxHighlighter : SyntaxHighlighterBase() {

    override fun getHighlightingLexer(): Lexer = CompactLexerAdapter()

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> {
        return pack(
            when (tokenType) {

                /* ===================================================== */
                /* Keywords */
                /* ===================================================== */

                CompactTokenTypes.EXPORT,
                CompactTokenTypes.IMPORT,
                CompactTokenTypes.FROM,
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
                CompactTokenTypes.PRAGMA,
                CompactTokenTypes.PURE,
                CompactTokenTypes.RETURN,
                CompactTokenTypes.SEALED,
                CompactTokenTypes.SLICE,
                CompactTokenTypes.STRUCT,
                CompactTokenTypes.TYPE,
                CompactTokenTypes.WITNESS,
                CompactTokenTypes.EMIT ->
                    KEYWORD

                CompactTokenTypes.RESERVED_KEYWORD ->
                    RESERVED_KEYWORD

                /* ===================================================== */
                /* Types */
                /* ===================================================== */

                CompactTokenTypes.BOOLEAN_TYPE,
                CompactTokenTypes.BYTES_TYPE,
                CompactTokenTypes.FIELD_TYPE,
                CompactTokenTypes.OPAQUE_TYPE,
                CompactTokenTypes.UINT_TYPE,
                CompactTokenTypes.VECTOR_TYPE,
                CompactTokenTypes.JUBJUB_SCALAR_TYPE,
                CompactTokenTypes.SECP256K1_BASE_TYPE,
                CompactTokenTypes.SECP256K1_SCALAR_TYPE ->
                    TYPE

                /* ===================================================== */
                /* Constants */
                /* ===================================================== */

                CompactTokenTypes.TRUE,
                CompactTokenTypes.FALSE ->
                    CONSTANT

                /* ===================================================== */
                /* Literals */
                /* ===================================================== */

                CompactTokenTypes.STRING_LITERAL ->
                    STRING

                CompactTokenTypes.VERSION_LITERAL ->
                    VERSION

                CompactTokenTypes.DECIMAL_LITERAL,
                CompactTokenTypes.BINARY_LITERAL,
                CompactTokenTypes.OCTAL_LITERAL,
                CompactTokenTypes.HEX_LITERAL ->
                    NUMBER

                /* ===================================================== */
                /* Comments */
                /* ===================================================== */

                CompactTokenTypes.LINE_COMMENT,
                CompactTokenTypes.BLOCK_COMMENT ->
                    COMMENT

                /* ===================================================== */
                /* Operators */
                /* ===================================================== */

                CompactTokenTypes.PLUS,
                CompactTokenTypes.MINUS,
                CompactTokenTypes.STAR,
                CompactTokenTypes.SLASH,
                CompactTokenTypes.PERCENT,
                CompactTokenTypes.ASSIGN,
                CompactTokenTypes.EQEQ,
                CompactTokenTypes.NEQ,
                CompactTokenTypes.LTE,
                CompactTokenTypes.GTE,
                CompactTokenTypes.LT,
                CompactTokenTypes.GT,
                CompactTokenTypes.NOT,
                CompactTokenTypes.ANDAND,
                CompactTokenTypes.OROR,
                CompactTokenTypes.ARROW,
                CompactTokenTypes.SPREAD,
                CompactTokenTypes.RANGE,
                CompactTokenTypes.PLUS_ASSIGN,
                CompactTokenTypes.MINUS_ASSIGN ->
                    OPERATOR

                /* ===================================================== */
                /* Delimiters */
                /* ===================================================== */

                CompactTokenTypes.LPAREN,
                CompactTokenTypes.RPAREN ->
                    PARENTHESES

                CompactTokenTypes.LBRACE,
                CompactTokenTypes.RBRACE ->
                    BRACES

                CompactTokenTypes.LBRACKET,
                CompactTokenTypes.RBRACKET ->
                    BRACKETS

                CompactTokenTypes.COMMA ->
                    COMMA

                CompactTokenTypes.SEMICOLON ->
                    SEMICOLON

                CompactTokenTypes.DOT ->
                    DOT

                /* ===================================================== */
                /* Errors */
                /* ===================================================== */

                CompactTokenTypes.UNTERMINATED_STRING,
                CompactTokenTypes.UNTERMINATED_BLOCK_COMMENT,
                TokenType.BAD_CHARACTER ->
                    BAD_CHARACTER

                else -> null
            }
        )
    }

    companion object {
        val KEYWORD =
            TextAttributesKey.createTextAttributesKey(
                "COMPACT_KEYWORD",
                DefaultLanguageHighlighterColors.KEYWORD
            )

        val TYPE =
            TextAttributesKey.createTextAttributesKey(
                "COMPACT_TYPE",
                DefaultLanguageHighlighterColors.CLASS_NAME
            )

        val STRING =
            TextAttributesKey.createTextAttributesKey(
                "COMPACT_STRING",
                DefaultLanguageHighlighterColors.STRING
            )

        val NUMBER =
            TextAttributesKey.createTextAttributesKey(
                "COMPACT_NUMBER",
                DefaultLanguageHighlighterColors.NUMBER
            )

        val COMMENT =
            TextAttributesKey.createTextAttributesKey(
                "COMPACT_COMMENT",
                DefaultLanguageHighlighterColors.LINE_COMMENT
            )

        val BAD_CHARACTER =
            TextAttributesKey.createTextAttributesKey(
                "COMPACT_BAD_CHARACTER",
                HighlighterColors.BAD_CHARACTER
            )

        val RESERVED_KEYWORD =
            TextAttributesKey.createTextAttributesKey(
                "COMPACT_RESERVED_KEYWORD",
                DefaultLanguageHighlighterColors.KEYWORD
            )

        val CONSTANT =
            TextAttributesKey.createTextAttributesKey(
                "COMPACT_CONSTANT",
                DefaultLanguageHighlighterColors.CONSTANT
            )

        val VERSION =
            TextAttributesKey.createTextAttributesKey(
                "COMPACT_VERSION",
                DefaultLanguageHighlighterColors.NUMBER
            )

        val OPERATOR =
            TextAttributesKey.createTextAttributesKey(
                "COMPACT_OPERATOR",
                DefaultLanguageHighlighterColors.OPERATION_SIGN
            )

        val PARENTHESES =
            TextAttributesKey.createTextAttributesKey(
                "COMPACT_PARENTHESES",
                DefaultLanguageHighlighterColors.PARENTHESES
            )

        val BRACES =
            TextAttributesKey.createTextAttributesKey(
                "COMPACT_BRACES",
                DefaultLanguageHighlighterColors.BRACES
            )

        val BRACKETS =
            TextAttributesKey.createTextAttributesKey(
                "COMPACT_BRACKETS",
                DefaultLanguageHighlighterColors.BRACKETS
            )

        val COMMA =
            TextAttributesKey.createTextAttributesKey(
                "COMPACT_COMMA",
                DefaultLanguageHighlighterColors.COMMA
            )

        val SEMICOLON =
            TextAttributesKey.createTextAttributesKey(
                "COMPACT_SEMICOLON",
                DefaultLanguageHighlighterColors.SEMICOLON
            )

        val DOT =
            TextAttributesKey.createTextAttributesKey(
                "COMPACT_DOT",
                DefaultLanguageHighlighterColors.DOT
            )
    }
}