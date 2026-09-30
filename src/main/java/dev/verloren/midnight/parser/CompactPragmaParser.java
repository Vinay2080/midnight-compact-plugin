package dev.verloren.midnight.parser;

import com.intellij.lang.PsiBuilder;
import com.intellij.psi.tree.IElementType;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import org.jetbrains.annotations.NotNull;

import static dev.verloren.midnight.parser.CompactParserUtil.at;
import static dev.verloren.midnight.parser.CompactParserUtil.expect;

/**
 * Parser delegate responsible for pragma directives and version constraints.
 */
final class CompactPragmaParser {

  void parsePragma(@NotNull PsiBuilder builder) {
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

  private boolean parseVersionExpression(@NotNull PsiBuilder builder) {
    do {
      if (!parseVersionConstraint(builder)) {
        return false;
      }
    } while (parseLogicalOperator(builder));
    return true;
  }

  private boolean parseLogicalOperator(@NotNull PsiBuilder builder) {
    if (at(builder, CompactTokenTypes.ANDAND) || at(builder, CompactTokenTypes.OROR)) {
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

  private boolean parseVersionConstraint(@NotNull PsiBuilder builder) {
    parseComparisonOperator(builder);
    return expectVersion(builder);
  }

  private void parseComparisonOperator(@NotNull PsiBuilder builder) {
    IElementType token = builder.getTokenType();
    if (token == CompactTokenTypes.GT
            || token == CompactTokenTypes.GTE
            || token == CompactTokenTypes.LT
            || token == CompactTokenTypes.LTE
            || token == CompactTokenTypes.NOT) {
      builder.advanceLexer();
    }
  }

  private boolean expectPragmaIdentifier(@NotNull PsiBuilder builder) {
    if (!at(builder, CompactTokenTypes.IDENTIFIER)) {
      builder.error("Expected pragma identifier");
      return false;
    }
    String text = builder.getTokenText();
    if (!"language_version".equals(text) && !"compiler_version".equals(text)) {
      builder.error("Expected 'language_version' or 'compiler_version'");
      builder.advanceLexer();
      return false;
    }
    builder.advanceLexer();
    return true;
  }

  private boolean expectVersion(@NotNull PsiBuilder builder) {
    if (at(builder, CompactTokenTypes.VERSION_LITERAL) || at(builder, CompactTokenTypes.DECIMAL_LITERAL)) {
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

  private void recoverPragmaTail(@NotNull PsiBuilder builder) {
    while (!builder.eof()
            && !at(builder, CompactTokenTypes.SEMICOLON)
            && !at(builder, CompactTokenTypes.PRAGMA)) {
      builder.advanceLexer();
    }
  }
}
