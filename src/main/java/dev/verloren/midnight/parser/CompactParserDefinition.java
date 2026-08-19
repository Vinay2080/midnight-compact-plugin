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
import dev.verloren.midnight.lexer.CompactTokenSets;
import dev.verloren.midnight.psi.CompactElementFactory;
import dev.verloren.midnight.psi.CompactFile;
import org.jetbrains.annotations.NotNull;

/**
 * Core IntelliJ integration point for parsing Compact source files.
 *
 * <p>Implements {@link ParserDefinition} and registers:
 * <ul>
 *   <li>The lexer ({@link CompactLexer}) used to tokenize the buffer.</li>
 *   <li>The parser ({@link CompactParser}) used to construct the AST.</li>
 *   <li>The root file element type ({@link #FILE}).</li>
 *   <li>Comment token sets for indexing and formatting.</li>
 *   <li>The PSI element factory method ({@link CompactElementFactory#createElement(ASTNode)}).</li>
 *   <li>The root {@link CompactFile} creation.</li>
 * </ul>
 * </p>
 */
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
    return CompactTokenSets.COMMENTS;
  }

  @Override
  public @NotNull TokenSet getStringLiteralElements() {
    return TokenSet.EMPTY;
  }

  @Override
  public @NotNull PsiElement createElement(ASTNode node) {
    return CompactElementFactory.createElement(node);
  }

  @Override
  public @NotNull PsiFile createFile(@NotNull FileViewProvider viewProvider) {
    return new CompactFile(viewProvider);
  }
}
