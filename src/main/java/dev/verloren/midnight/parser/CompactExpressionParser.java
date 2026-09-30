package dev.verloren.midnight.parser;

import com.intellij.lang.PsiBuilder;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static dev.verloren.midnight.parser.CompactParserUtil.at;
import static dev.verloren.midnight.parser.CompactParserUtil.expect;

/**
 * Parser delegate responsible for Compact expressions, operators, and invocations.
 */
final class CompactExpressionParser {
  static final TokenSet RPAREN_TERMINATOR = TokenSet.create(CompactTokenTypes.RPAREN);
  static final TokenSet COLON_TERMINATOR = TokenSet.create(CompactTokenTypes.COLON);

  private final CompactParserContext context;

  CompactExpressionParser(@NotNull CompactParserContext context) {
    this.context = context;
  }

  void parseExpressionSequenceUntil(@NotNull TokenSet terminators, @NotNull PsiBuilder builder) {
    PsiBuilder.Marker sequence = builder.mark();
    if (!builder.eof() && !terminators.contains(builder.getTokenType())) {
      parseExpression(builder);
      while (at(builder, CompactTokenTypes.COMMA) && !terminators.contains(CompactTokenTypes.COMMA)) {
        builder.advanceLexer();
        if (builder.eof() || terminators.contains(builder.getTokenType())) {
          break;
        }
        parseExpression(builder);
      }
    }
    sequence.done(CompactElementTypes.EXPRESSION_SEQUENCE);
  }

  void parseExpression(@NotNull PsiBuilder builder) {
    parseAssignmentExpression(builder);
  }

  private void parseAssignmentExpression(@NotNull PsiBuilder builder) {
    PsiBuilder.Marker left = parseTernaryExpression(builder);
    if (at(builder, CompactTokenTypes.ASSIGN)
            || at(builder, CompactTokenTypes.PLUS_ASSIGN)
            || at(builder, CompactTokenTypes.MINUS_ASSIGN)) {
      PsiBuilder.Marker assignment = left.precede();
      builder.advanceLexer();
      parseAssignmentExpression(builder);
      assignment.done(CompactElementTypes.ASSIGN_EXPR);
    }
  }

  private PsiBuilder.Marker parseTernaryExpression(@NotNull PsiBuilder builder) {
    PsiBuilder.Marker condition = parseBinaryExpression(builder, 0);
    if (at(builder, CompactTokenTypes.QUESTION)) {
      PsiBuilder.Marker ternary = condition.precede();
      builder.advanceLexer();
      parseExpressionSequenceUntil(COLON_TERMINATOR, builder);
      expect(builder, CompactTokenTypes.COLON, "Expected ':'");
      parseExpression(builder);
      ternary.done(CompactElementTypes.TERNARY_EXPR);
      return ternary;
    }
    return condition;
  }

  private PsiBuilder.Marker parseBinaryExpression(@NotNull PsiBuilder builder, int minPrecedence) {
    PsiBuilder.Marker left = parseUnaryExpression(builder);
    while (!builder.eof()) {
      IElementType operator = builder.getTokenType();
      int precedence = CompactParserUtil.binaryPrecedence(operator);
      if (precedence < minPrecedence) {
        break;
      }

      PsiBuilder.Marker expression = left.precede();
      builder.advanceLexer();
      if (operator == CompactTokenTypes.AS) {
        context.typePattern.parseType(builder);
        expression.done(CompactElementTypes.CAST_EXPR);
      } else {
        parseBinaryExpression(builder, precedence + 1);
        expression.done(CompactElementTypes.BINARY_EXPR);
      }
      left = expression;
    }
    return left;
  }

  private PsiBuilder.Marker parseUnaryExpression(@NotNull PsiBuilder builder) {
    if (at(builder, CompactTokenTypes.NOT) || at(builder, CompactTokenTypes.MINUS)) {
      PsiBuilder.Marker unary = builder.mark();
      builder.advanceLexer();
      parseUnaryExpression(builder);
      unary.done(CompactElementTypes.UNARY_EXPR);
      return unary;
    }
    return parsePostfixExpression(builder);
  }

  private PsiBuilder.Marker parsePostfixExpression(@NotNull PsiBuilder builder) {
    PsiBuilder.Marker expression = parsePrimaryExpression(builder);
    while (!builder.eof()) {
      if (at(builder, CompactTokenTypes.LBRACKET)) {
        expression = parseIndexExpression(expression, builder);
      } else if (at(builder, CompactTokenTypes.DOT)) {
        expression = parseDotExpression(expression, builder);
      } else if (at(builder, CompactTokenTypes.LPAREN)) {
        PsiBuilder.Marker call = expression.precede();
        parseArgumentList(builder);
        call.done(CompactElementTypes.CALL_EXPR);
        expression = call;
      } else {
        break;
      }
    }
    return expression;
  }

  private PsiBuilder.Marker parseIndexExpression(PsiBuilder.Marker expression, @NotNull PsiBuilder builder) {
    PsiBuilder.Marker index = expression.precede();
    builder.advanceLexer();
    parseExpressionSequenceUntil(TokenSet.create(CompactTokenTypes.RBRACKET), builder);
    expect(builder, CompactTokenTypes.RBRACKET, "Expected ']'");
    index.done(CompactElementTypes.INDEX_EXPR);
    return index;
  }

  private PsiBuilder.Marker parseDotExpression(PsiBuilder.Marker expression, @NotNull PsiBuilder builder) {
    PsiBuilder.Marker member = expression.precede();
    builder.advanceLexer();
    expect(builder, CompactTokenTypes.IDENTIFIER, "Expected member name");
    if (at(builder, CompactTokenTypes.LPAREN)) {
      parseArgumentList(builder);
      member.done(CompactElementTypes.CALL_EXPR);
    } else {
      member.done(CompactElementTypes.MEMBER_EXPR);
    }
    return member;
  }

  private PsiBuilder.Marker parsePrimaryExpression(@NotNull PsiBuilder builder) {
    if (at(builder, CompactTokenTypes.IDENTIFIER)) {
      return parseReferenceLikeExpression(builder);
    }
    if (isLiteral(builder.getTokenType())) {
      PsiBuilder.Marker literal = builder.mark();
      builder.advanceLexer();
      literal.done(CompactElementTypes.LITERAL_EXPR);
      return literal;
    }
    if (at(builder, CompactTokenTypes.LPAREN)) {
      return parseParenOrLambda(builder);
    }
    if (at(builder, CompactTokenTypes.LBRACKET)) {
      return parseTupleExpression(builder);
    }
    if (at(builder, CompactTokenTypes.BYTES_TYPE) && builder.lookAhead(1) == CompactTokenTypes.LBRACKET) {
      return parseBytesExpression(builder);
    }
    return parseKeywordOrError(builder);
  }

  private PsiBuilder.Marker parseParenOrLambda(@NotNull PsiBuilder builder) {
    if (looksLikeLambdaExpression(builder)) {
      PsiBuilder.Marker lambda = tryParseLambdaExpression(builder);
      if (lambda != null) {
        return lambda;
      }
    }
    PsiBuilder.Marker paren = builder.mark();
    builder.advanceLexer();
    parseExpressionSequenceUntil(RPAREN_TERMINATOR, builder);
    expect(builder, CompactTokenTypes.RPAREN, "Expected ')'");
    paren.done(CompactElementTypes.PAREN_EXPR);
    return paren;
  }

  private PsiBuilder.Marker parseKeywordOrError(@NotNull PsiBuilder builder) {
    if (at(builder, CompactTokenTypes.PAD)) {
      return parseKeywordCallExpression(builder, CompactElementTypes.PAD_EXPR);
    }
    if (at(builder, CompactTokenTypes.DEFAULT)) {
      return parseDefaultExpression(builder);
    }
    if (at(builder, CompactTokenTypes.MAP)) {
      return parseKeywordCallExpression(builder, CompactElementTypes.MAP_EXPR);
    }
    if (at(builder, CompactTokenTypes.FOLD)) {
      return parseKeywordCallExpression(builder, CompactElementTypes.FOLD_EXPR);
    }
    if (at(builder, CompactTokenTypes.SLICE)) {
      return parseSliceExpression(builder);
    }
    if (at(builder, CompactTokenTypes.ASSERT)) {
      return parseKeywordCallExpression(builder, CompactElementTypes.ASSERT_EXPR);
    }
    if (at(builder, CompactTokenTypes.EMIT)) {
      return parseKeywordCallExpression(builder, CompactElementTypes.EMIT_EXPR);
    }
    if (at(builder, CompactTokenTypes.DISCLOSE)) {
      return parseKeywordCallExpression(builder, CompactElementTypes.DISCLOSE_EXPR);
    }

    PsiBuilder.Marker error = builder.mark();
    CompactParserUtil.errorAndAdvance(builder, "Expected expression");
    error.done(CompactElementTypes.LITERAL_EXPR);
    return error;
  }

  private boolean looksLikeLambdaExpression(@NotNull PsiBuilder builder) {
    int depth = 0;
    for (int i = 0; i < 200; i++) {
      IElementType token = builder.lookAhead(i);
      if (token == null) {
        return false;
      }
      if (token == CompactTokenTypes.LPAREN) {
        depth++;
      } else if (token == CompactTokenTypes.RPAREN) {
        depth--;
        if (depth == 0) {
          IElementType next = builder.lookAhead(i + 1);
          return next == CompactTokenTypes.ARROW || next == CompactTokenTypes.COLON;
        }
      }
    }
    return false;
  }

  private @Nullable PsiBuilder.Marker tryParseLambdaExpression(@NotNull PsiBuilder builder) {
    PsiBuilder.Marker rollback = builder.mark();
    PsiBuilder.Marker lambda = builder.mark();
    context.typePattern.parseArrowParameterList(builder);
    if (at(builder, CompactTokenTypes.COLON)) {
      context.typePattern.parseReturnType(builder);
    }
    if (!at(builder, CompactTokenTypes.ARROW)) {
      lambda.drop();
      rollback.rollbackTo();
      return null;
    }

    builder.advanceLexer();
    if (at(builder, CompactTokenTypes.LBRACE)) {
      context.statement.parseBlock(builder);
    } else {
      parseExpression(builder);
    }
    lambda.done(CompactElementTypes.LAMBDA_EXPR);
    rollback.drop();
    return lambda;
  }

  private PsiBuilder.Marker parseReferenceLikeExpression(@NotNull PsiBuilder builder) {
    PsiBuilder.Marker expression = builder.mark();
    builder.advanceLexer();
    boolean hasCommittedGenericArguments = false;
    if (at(builder, CompactTokenTypes.LT)) {
      PsiBuilder.Marker genericAttempt = builder.mark();
      context.typePattern.parseGenericArgumentList(builder);
      if (at(builder, CompactTokenTypes.LPAREN) || at(builder, CompactTokenTypes.LBRACE)) {
        genericAttempt.drop();
        hasCommittedGenericArguments = true;
      } else {
        genericAttempt.rollbackTo();
      }
    }

    if (at(builder, CompactTokenTypes.LPAREN)) {
      parseArgumentList(builder);
      expression.done(CompactElementTypes.CALL_EXPR);
    } else if (at(builder, CompactTokenTypes.LBRACE)) {
      parseStructArgumentList(builder);
      expression.done(CompactElementTypes.STRUCT_LITERAL_EXPR);
    } else {
      if (hasCommittedGenericArguments) {
        builder.error("Expected '(' or '{' after generic arguments");
      }
      expression.done(CompactElementTypes.REFERENCE_EXPR);
    }
    return expression;
  }

  private PsiBuilder.Marker parseTupleExpression(@NotNull PsiBuilder builder) {
    PsiBuilder.Marker tuple = builder.mark();
    expect(builder, CompactTokenTypes.LBRACKET, "Expected '['");
    while (!builder.eof() && !at(builder, CompactTokenTypes.RBRACKET)) {
      parseTupleArgument(builder);
      if (at(builder, CompactTokenTypes.COMMA)) {
        builder.advanceLexer();
      } else if (!at(builder, CompactTokenTypes.RBRACKET)) {
        builder.error("Expected ',' or ']'");
        builder.advanceLexer();
      }
    }
    expect(builder, CompactTokenTypes.RBRACKET, "Expected ']'");
    tuple.done(CompactElementTypes.TUPLE_EXPR);
    return tuple;
  }

  private void parseTupleArgument(@NotNull PsiBuilder builder) {
    PsiBuilder.Marker argument = builder.mark();
    if (at(builder, CompactTokenTypes.SPREAD)) {
      builder.advanceLexer();
    }
    parseExpression(builder);
    argument.done(CompactElementTypes.TUPLE_ARG);
  }

  private PsiBuilder.Marker parseBytesExpression(@NotNull PsiBuilder builder) {
    PsiBuilder.Marker bytes = builder.mark();
    expect(builder, CompactTokenTypes.BYTES_TYPE, "Expected 'Bytes'");
    expect(builder, CompactTokenTypes.LBRACKET, "Expected '['");
    parseExpressionSequenceUntil(TokenSet.create(CompactTokenTypes.RBRACKET), builder);
    expect(builder, CompactTokenTypes.RBRACKET, "Expected ']'");
    bytes.done(CompactElementTypes.BYTES_EXPR);
    return bytes;
  }

  private PsiBuilder.Marker parseKeywordCallExpression(@NotNull PsiBuilder builder, IElementType expressionType) {
    PsiBuilder.Marker expression = builder.mark();
    builder.advanceLexer();
    parseArgumentList(builder);
    expression.done(expressionType);
    return expression;
  }

  private PsiBuilder.Marker parseDefaultExpression(@NotNull PsiBuilder builder) {
    PsiBuilder.Marker expression = builder.mark();
    expect(builder, CompactTokenTypes.DEFAULT, "Expected 'default'");
    expect(builder, CompactTokenTypes.LT, "Expected '<'");
    context.typePattern.parseType(builder);
    expect(builder, CompactTokenTypes.GT, "Expected '>'");
    expression.done(CompactElementTypes.DEFAULT_EXPR);
    return expression;
  }

  private PsiBuilder.Marker parseSliceExpression(@NotNull PsiBuilder builder) {
    PsiBuilder.Marker expression = builder.mark();
    expect(builder, CompactTokenTypes.SLICE, "Expected 'slice'");
    expect(builder, CompactTokenTypes.LT, "Expected '<'");
    context.typePattern.parseTypeSize(builder);
    expect(builder, CompactTokenTypes.GT, "Expected '>'");
    parseArgumentList(builder);
    expression.done(CompactElementTypes.SLICE_EXPR);
    return expression;
  }

  void parseArgumentList(@NotNull PsiBuilder builder) {
    expect(builder, CompactTokenTypes.LPAREN, "Expected '('");
    parseExpressionSequenceUntil(RPAREN_TERMINATOR, builder);
    expect(builder, CompactTokenTypes.RPAREN, "Expected ')'");
  }

  void parseStructArgumentList(@NotNull PsiBuilder builder) {
    expect(builder, CompactTokenTypes.LBRACE, "Expected '{'");
    while (!builder.eof() && !at(builder, CompactTokenTypes.RBRACE)) {
      parseStructArgument(builder);
      if (at(builder, CompactTokenTypes.COMMA)) {
        builder.advanceLexer();
      } else if (!at(builder, CompactTokenTypes.RBRACE)) {
        builder.error("Expected ',' or '}'");
        builder.advanceLexer();
      }
    }
    expect(builder, CompactTokenTypes.RBRACE, "Expected '}'");
  }

  private void parseStructArgument(@NotNull PsiBuilder builder) {
    PsiBuilder.Marker argument = builder.mark();
    if (at(builder, CompactTokenTypes.SPREAD)) {
      builder.advanceLexer();
      parseExpression(builder);
    } else {
      expect(builder, CompactTokenTypes.IDENTIFIER, "Expected field name");
      if (at(builder, CompactTokenTypes.COLON)) {
        builder.advanceLexer();
        parseExpression(builder);
      }
    }
    argument.done(CompactElementTypes.STRUCT_ARG);
  }

  static boolean isLiteral(IElementType token) {
    return token == CompactTokenTypes.TRUE
            || token == CompactTokenTypes.FALSE
            || token == CompactTokenTypes.STRING_LITERAL
            || CompactTypePatternParser.isNatLiteral(token);
  }
}
