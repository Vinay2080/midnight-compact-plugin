package dev.verloren.midnight.parser;

import com.intellij.lang.ASTNode;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;
import dev.verloren.midnight.CompactLanguage;
import dev.verloren.midnight.lexer.CompactLexerAdapter;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import dev.verloren.midnight.psi.CompactFile;
import dev.verloren.midnight.psi.CompactTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CompactParserDefinition implements ParserDefinition {
//
//  public static final IFileElementType FILE =
//          new IFileElementType(CompactLanguage.INSTANCE);
//
//  public static final TokenSet WHITE_SPACES =
//          TokenSet.create(TokenType.WHITE_SPACE);
//
//  @Override
//  public @NotNull Lexer createLexer(@Nullable Project project) {
//    return new CompactLexerAdapter();
//  }
//
//  @Override
//  public @NotNull PsiParser createParser(@Nullable Project project) {
//    return new CompactParser();
//  }
//
//  @Override
//  public @NotNull IFileElementType getFileNodeType() {
//    return FILE;
//  }
//
//  @Override
//  public @NotNull TokenSet getCommentTokens() {
//    return TokenSet.create(
//            CompactTokenTypes.LINE_COMMENT,
//            CompactTokenTypes.BLOCK_COMMENT
//    );
//  }
//
//  @Override
//  public @NotNull TokenSet getStringLiteralElements() {
//    return TokenSet.create(CompactTypes.STRING_LITERAL);
//  }
//
//  @Override
//  public @NotNull TokenSet getWhitespaceTokens() {
//    return WHITE_SPACES;
//  }
//
//  @Override
//  public @NotNull PsiElement createElement(@NotNull ASTNode node) {
//    return CompactTypes.Factory.createElement(node);
//  }
//
//  @Override
//  public @NotNull PsiFile createFile(@NotNull FileViewProvider viewProvider) {
//    return new CompactFile(viewProvider);
//  }
}