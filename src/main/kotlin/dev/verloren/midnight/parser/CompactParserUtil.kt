package dev.verloren.midnight.parser

import com.intellij.lang.PsiBuilder
import com.intellij.lang.parser.GeneratedParserUtilBase
import dev.verloren.midnight.lexer.CompactTokenTypes

object CompactParserUtil : GeneratedParserUtilBase() {
    @JvmStatic
    fun pragmaIdentifier(builder: PsiBuilder, level: Int) : Boolean {
if (builder.tokenType != CompactTokenTypes.IDENTIFIER) {
builder.error("Identifier expected after 'pragma'")
    return false
        }
        builder.advanceLexer()
        return true
    }
}