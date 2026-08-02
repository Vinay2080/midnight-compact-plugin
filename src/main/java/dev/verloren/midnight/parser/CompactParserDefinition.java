package dev.verloren.midnight.parser;

import com.intellij.lang.ASTNode;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;
import dev.verloren.midnight.CompactLanguage;
import dev.verloren.midnight.lexer.CompactLexer;
import dev.verloren.midnight.psi.CompactFile;
import dev.verloren.midnight.psi.CompactPsiElement;
import org.jetbrains.annotations.NotNull;

public class CompactParserDefinition implements ParserDefinition {
  public static final IFileElementType FILE = new IFileElementType(CompactLanguage.INSTANCE);

  @Override
  public @NotNull Lexer createLexer(Project project) {
    return new CompactLexer();
  }

  @Override
  public @NotNull PsiParser createParser(Project project) {
    return new CompactParser();
  }

  @Override
  public @NotNull IFileElementType getFileNodeType() {
    return FILE;
  }

  @Override
  public @NotNull TokenSet getCommentTokens() {
    return TokenSet.EMPTY;
  }

  @Override
  public @NotNull TokenSet getStringLiteralElements() {
    return TokenSet.EMPTY;
  }

  @Override
  public @NotNull PsiElement createElement(ASTNode node) {
    return new CompactPsiElement(node);
  }

  @Override
  public @NotNull PsiFile createFile(@NotNull FileViewProvider viewProvider) {
    return new CompactFile(viewProvider);
  }
}
