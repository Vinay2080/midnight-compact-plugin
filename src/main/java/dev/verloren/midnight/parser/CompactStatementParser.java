package dev.verloren.midnight.parser;

import com.intellij.lang.PsiBuilder;
import com.intellij.psi.tree.TokenSet;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import org.jetbrains.annotations.NotNull;

import static dev.verloren.midnight.parser.CompactParserUtil.at;
import static dev.verloren.midnight.parser.CompactParserUtil.expect;

/**
 * Parser delegate responsible for Compact statements, blocks, control flows, and local bindings.
 */
final class CompactStatementParser {
  private static final TokenSet SEMICOLON_TERMINATOR = TokenSet.create(CompactTokenTypes.SEMICOLON);
  private static final TokenSet RPAREN_TERMINATOR = TokenSet.create(CompactTokenTypes.RPAREN);
  private static final TokenSet COMMA_OR_SEMICOLON_TERMINATOR = TokenSet.create(
          CompactTokenTypes.COMMA,
          CompactTokenTypes.SEMICOLON
  );

  private final CompactParserContext context;

  CompactStatementParser(@NotNull CompactParserContext context) {
    this.context = context;
  }

  void parseBlock(@NotNull PsiBuilder builder) {
    PsiBuilder.Marker block = builder.mark();
    if (!expect(builder, CompactTokenTypes.LBRACE, "Expected '{'")) {
      block.done(CompactElementTypes.BLOCK);
      return;
    }
    while (!builder.eof() && !at(builder, CompactTokenTypes.RBRACE)) {
      if (!parseStatement(builder)) {
        CompactParserUtil.errorAndAdvance(builder, "Expected statement");
      }
    }
    expect(builder, CompactTokenTypes.RBRACE, "Expected '}'");
    block.done(CompactElementTypes.BLOCK);
  }

  boolean parseStatement(@NotNull PsiBuilder builder) {
    if (builder.eof() || at(builder, CompactTokenTypes.RBRACE)) {
      return false;
    }
    if (at(builder, CompactTokenTypes.LBRACE)) {
      parseBlock(builder);
      return true;
    }
    if (at(builder, CompactTokenTypes.IF)) {
      parseIfStatement(builder);
      return true;
    }
    if (at(builder, CompactTokenTypes.FOR)) {
      parseForStatement(builder);
      return true;
    }
    if (at(builder, CompactTokenTypes.CONST)) {
      parseConstStatement(builder);
      return true;
    }
    if (at(builder, CompactTokenTypes.RETURN)) {
      parseReturnStatement(builder);
      return true;
    }
    parseExpressionStatement(builder);
    return true;
  }

  private void parseIfStatement(@NotNull PsiBuilder builder) {
    PsiBuilder.Marker statement = builder.mark();
    expect(builder, CompactTokenTypes.IF, "Expected 'if'");
    expect(builder, CompactTokenTypes.LPAREN, "Expected '('");
    context.expression.parseExpressionSequenceUntil(RPAREN_TERMINATOR, builder);
    expect(builder, CompactTokenTypes.RPAREN, "Expected ')'");
    parseStatement(builder);
    if (at(builder, CompactTokenTypes.ELSE)) {
      builder.advanceLexer();
      parseStatement(builder);
    }
    statement.done(CompactElementTypes.IF_STATEMENT);
  }

  private void parseForStatement(@NotNull PsiBuilder builder) {
    PsiBuilder.Marker statement = builder.mark();
    expect(builder, CompactTokenTypes.FOR, "Expected 'for'");
    expect(builder, CompactTokenTypes.LPAREN, "Expected '('");
    expect(builder, CompactTokenTypes.CONST, "Expected 'const'");
    expect(builder, CompactTokenTypes.IDENTIFIER, "Expected loop variable");
    expect(builder, CompactTokenTypes.OF, "Expected 'of'");
    if ((CompactTypePatternParser.isNatLiteral(builder.getTokenType()) || at(builder, CompactTokenTypes.IDENTIFIER))
            && builder.lookAhead(1) == CompactTokenTypes.RANGE) {
      context.typePattern.parseTypeSize(builder);
      expect(builder, CompactTokenTypes.RANGE, "Expected '..'");
      context.typePattern.parseTypeSize(builder);
    } else {
      context.expression.parseExpressionSequenceUntil(RPAREN_TERMINATOR, builder);
    }
    expect(builder, CompactTokenTypes.RPAREN, "Expected ')'");
    parseStatement(builder);
    statement.done(CompactElementTypes.FOR_STATEMENT);
  }

  void parseConstStatement(@NotNull PsiBuilder builder, boolean exported) {
    PsiBuilder.Marker statement = builder.mark();
    if (exported) {
      expect(builder, CompactTokenTypes.EXPORT, "Expected 'export'");
    }
    expect(builder, CompactTokenTypes.CONST, "Expected 'const'");
    while (!builder.eof() && !at(builder, CompactTokenTypes.SEMICOLON)) {
      parseConstBinding(builder);
      if (at(builder, CompactTokenTypes.COMMA)) {
        builder.advanceLexer();
      } else if (!at(builder, CompactTokenTypes.SEMICOLON)) {
        builder.error("Expected ',' or ';'");
        builder.advanceLexer();
      }
    }
    expect(builder, CompactTokenTypes.SEMICOLON, "Expected ';'");
    statement.done(CompactElementTypes.CONST_STATEMENT);
  }

  void parseConstStatement(@NotNull PsiBuilder builder) {
    parseConstStatement(builder, false);
  }

  private void parseConstBinding(@NotNull PsiBuilder builder) {
    PsiBuilder.Marker binding = builder.mark();
    context.typePattern.parseOptionallyTypedPattern(builder);
    expect(builder, CompactTokenTypes.ASSIGN, "Expected '='");
    context.expression.parseExpressionSequenceUntil(COMMA_OR_SEMICOLON_TERMINATOR, builder);
    binding.done(CompactElementTypes.CONST_BINDING);
  }

  private void parseReturnStatement(@NotNull PsiBuilder builder) {
    PsiBuilder.Marker statement = builder.mark();
    expect(builder, CompactTokenTypes.RETURN, "Expected 'return'");
    if (!at(builder, CompactTokenTypes.SEMICOLON)) {
      context.expression.parseExpressionSequenceUntil(SEMICOLON_TERMINATOR, builder);
    }
    expect(builder, CompactTokenTypes.SEMICOLON, "Expected ';'");
    statement.done(CompactElementTypes.RETURN_STATEMENT);
  }

  private void parseExpressionStatement(@NotNull PsiBuilder builder) {
    PsiBuilder.Marker statement = builder.mark();
    context.expression.parseExpressionSequenceUntil(SEMICOLON_TERMINATOR, builder);
    expect(builder, CompactTokenTypes.SEMICOLON, "Expected ';'");
    statement.done(CompactElementTypes.EXPR_STATEMENT);
  }
}
