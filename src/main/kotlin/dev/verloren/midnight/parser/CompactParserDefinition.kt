package dev.verloren.midnight.parser

import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import dev.verloren.midnight.CompactLanguage
import dev.verloren.midnight.lexer.CompactLexerAdapter
import dev.verloren.midnight.psi.CompactFile
import dev.verloren.midnight.psi.CompactTypes

class CompactParserDefinition : ParserDefinition {

    companion object {
        @JvmField
        val FILE = IFileElementType(CompactLanguage)

        @JvmField
        val WHITE_SPACES = TokenSet.create(TokenType.WHITE_SPACE)

        @JvmField
        val COMMENTS = TokenSet.EMPTY

        @JvmField
        val STRINGS = TokenSet.EMPTY
    }

    override fun createLexer(project: Project?): Lexer = CompactLexerAdapter()

    override fun createParser(project: Project?): PsiParser = CompactParser()

    override fun getFileNodeType(): IFileElementType = FILE

    override fun getCommentTokens(): TokenSet = COMMENTS

    override fun getStringLiteralElements(): TokenSet = STRINGS

    override fun getWhitespaceTokens(): TokenSet = WHITE_SPACES

    override fun createElement(node: ASTNode): PsiElement = CompactTypes.Factory.createElement(node)

    override fun createFile(viewProvider: FileViewProvider): PsiFile =
        CompactFile(viewProvider)
}