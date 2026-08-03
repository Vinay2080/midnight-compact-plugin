package dev.verloren.midnight.parser;

import com.intellij.lang.ASTNode;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiParser;
import com.intellij.psi.tree.IElementType;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import org.jetbrains.annotations.NotNull;

public final class CompactParser implements PsiParser {
  @Override
  public @NotNull ASTNode parse(@NotNull IElementType root, @NotNull PsiBuilder builder) {
    PsiBuilder.Marker file = builder.mark();

    while (!builder.eof()) {
      if (at(builder, CompactTokenTypes.PRAGMA)) {
        parsePragma(builder);
      } else {
        errorAndAdvance(builder, "Expected 'pragma'");
      }
    }

    file.done(root);
    return builder.getTreeBuilt();
  }

  private void parsePragma(PsiBuilder builder) {
    PsiBuilder.Marker pragma = builder.mark();

    expect(builder, CompactTokenTypes.PRAGMA, "Expected 'pragma'");

    if (!expectPragmaIdentifier(builder)) {
      recoverPragmaTail(builder);
      expect(builder, CompactTokenTypes.SEMICOLON, "Expected ';'");
      pragma.done(CompactElementTypes.PRAGMA_FORM);
      return;
    }

    if (!parseVersionExpression(builder)) {
      recoverPragmaTail(builder);
    }

    expect(builder, CompactTokenTypes.SEMICOLON, "Expected ';'");
    pragma.done(CompactElementTypes.PRAGMA_FORM);
  }

  private boolean parseVersionExpression(PsiBuilder builder) {

    do {
      if (!parseVersionConstraint(builder)) {
        return false;
      }
    } while (parseLogicalOperator(builder));

    return true;
  }

  private boolean parseLogicalOperator(PsiBuilder builder) {
    if (at(builder, CompactTokenTypes.AND)
            || at(builder, CompactTokenTypes.OR)) {
      builder.advanceLexer();
      return true;
    }

    if (at(builder, CompactTokenTypes.SEMICOLON)) {
      return false;
    }

    builder.error("Expected '&&', '||', or ';'");
    builder.advanceLexer();
    return false;
  }

  private boolean parseVersionConstraint(PsiBuilder builder) {
    parseComparisonOperator(builder);
    return expectVersion(builder);
  }

  private void parseComparisonOperator(PsiBuilder builder) {
    IElementType token = builder.getTokenType();
    if (token == CompactTokenTypes.GT
            || token == CompactTokenTypes.GTE
            || token == CompactTokenTypes.LT
            || token == CompactTokenTypes.LTE
            || token == CompactTokenTypes.NOT) {

      builder.advanceLexer();
    }
  }

  private boolean expectPragmaIdentifier(PsiBuilder builder) {
    if (!at(builder, CompactTokenTypes.IDENTIFIER)) {
      builder.error("Expected pragma identifier");
      return false;
    }
    String text = builder.getTokenText();

    if (!"language_version".equals(text)
            && !"compiler_version".equals(text)) {
      builder.error("Expected 'language_version' or 'compiler_version'");
      builder.advanceLexer();   // consume the bad identifier
      return false;
    }

    builder.advanceLexer();
    return true;
  }

  private boolean expectVersion(PsiBuilder builder) {
    if (at(builder, CompactTokenTypes.VERSION)) {
      builder.advanceLexer();
      return true;
    }

    if (at(builder, CompactTokenTypes.INVALID_VERSION)) {
      builder.error("Malformed version literal; expected '1', '1.0', or '1.2.3'");
      builder.advanceLexer();
      return false;
    }

    builder.error("Expected a version such as '1', '1.0', or '1.2.3'");
    return false;
  }

  private void expect(PsiBuilder builder, IElementType tokenType, String message) {
    if (!at(builder, tokenType)) {
      builder.error(message);
      return;
    }

    builder.advanceLexer();
  }

  private void recoverPragmaTail(PsiBuilder builder) {
    while (!builder.eof()
            && !at(builder, CompactTokenTypes.SEMICOLON)
            && !at(builder, CompactTokenTypes.PRAGMA)) {
      builder.advanceLexer();
    }
  }

  private void errorAndAdvance(PsiBuilder builder, String message) {
    builder.error(message);
    builder.advanceLexer();
  }

  private static boolean at(PsiBuilder builder, IElementType tokenType) {
    return builder.getTokenType() == tokenType;
  }

}
