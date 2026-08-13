package dev.verloren.midnight.refactoring;

import com.intellij.lang.refactoring.NamesValidator;
import com.intellij.openapi.project.Project;
import com.intellij.psi.tree.IElementType;
import dev.verloren.midnight.lexer.CompactLexer;
import dev.verloren.midnight.lexer.CompactTokenSets;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CompactNamesValidator implements NamesValidator {
  @Override
  public boolean isKeyword(@NotNull String name, @Nullable Project project) {
    IElementType tokenType = singleToken(name);
    return CompactTokenSets.KEYWORDS.contains(tokenType);
  }

  @Override
  public boolean isIdentifier(@NotNull String name, @Nullable Project project) {
    return singleToken(name) == CompactTokenTypes.IDENTIFIER;
  }

  private static @Nullable IElementType singleToken(@NotNull String text) {
    CompactLexer lexer = new CompactLexer();
    lexer.start(text);
    IElementType tokenType = lexer.getTokenType();
    if (tokenType == null || lexer.getTokenStart() != 0 || lexer.getTokenEnd() != text.length()) {
      return null;
    }
    lexer.advance();
    return lexer.getTokenType() == null ? tokenType : null;
  }
}