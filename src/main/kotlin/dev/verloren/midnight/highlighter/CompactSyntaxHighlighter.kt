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
                // Keywords
                CompactTokenTypes.EXPORT,
                CompactTokenTypes.IMPORT,
                CompactTokenTypes.FROM,
                CompactTokenTypes.MODULE,
                CompactTokenTypes.PREFIX,
                CompactTokenTypes.CONTRACT,
                CompactTokenTypes.CIRCUIT,
                CompactTokenTypes.CONST,
                CompactTokenTypes.RETURN,
                CompactTokenTypes.IF,
                CompactTokenTypes.ELSE,
                CompactTokenTypes.FOR,
                CompactTokenTypes.STRUCT,
                CompactTokenTypes.ENUM,
                CompactTokenTypes.TYPE,
                CompactTokenTypes.NEW,
                CompactTokenTypes.PURE,
                CompactTokenTypes.SEALED ->
                    KEYWORD

                // Types
                CompactTokenTypes.BOOLEAN_TYPE,
                CompactTokenTypes.BYTES_TYPE,
                CompactTokenTypes.FIELD_TYPE,
                CompactTokenTypes.OPAQUE_TYPE,
                CompactTokenTypes.UINT_TYPE,
                CompactTokenTypes.VECTOR_TYPE ->
                    TYPE

                // Literals
                CompactTokenTypes.STRING_LITERAL ->
                    STRING

                CompactTokenTypes.DECIMAL_LITERAL,
                CompactTokenTypes.BINARY_LITERAL,
                CompactTokenTypes.OCTAL_LITERAL,
                CompactTokenTypes.HEX_LITERAL ->
                    NUMBER

                // Comments
                CompactTokenTypes.LINE_COMMENT,
                CompactTokenTypes.BLOCK_COMMENT ->
                    COMMENT

                // Bad character
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
    }
}