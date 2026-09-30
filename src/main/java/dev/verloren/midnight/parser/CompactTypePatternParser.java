package dev.verloren.midnight.parser;

import com.intellij.lang.PsiBuilder;
import com.intellij.psi.tree.IElementType;
import dev.verloren.midnight.lexer.CompactTokenSets;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import org.jetbrains.annotations.NotNull;

import static dev.verloren.midnight.parser.CompactParserUtil.at;
import static dev.verloren.midnight.parser.CompactParserUtil.expect;

/**
 * Parser delegate responsible for types, type references, parameters, and patterns.
 */
final class CompactTypePatternParser {
  private final CompactParserContext context;

  CompactTypePatternParser(@NotNull CompactParserContext context) {
    this.context = context;
  }

  void parseGenericParameterList(@NotNull PsiBuilder builder) {
    PsiBuilder.Marker list = builder.mark();
    expect(builder, CompactTokenTypes.LT, "Expected '<'");
    while (!builder.eof() && !at(builder, CompactTokenTypes.GT)) {
      PsiBuilder.Marker parameter = builder.mark();
      if (at(builder, CompactTokenTypes.HASH)) {
        builder.advanceLexer();
      }
      expect(builder, CompactTokenTypes.IDENTIFIER, "Expected generic parameter");
      parameter.done(CompactElementTypes.GENERIC_PARAMETER);
      if (at(builder, CompactTokenTypes.COMMA)) {
        builder.advanceLexer();
      } else if (!at(builder, CompactTokenTypes.GT)) {
        builder.error("Expected ',' or '>'");
        builder.advanceLexer();
      }
    }
    expect(builder, CompactTokenTypes.GT, "Expected '>'");
    list.done(CompactElementTypes.GENERIC_PARAMETER_LIST);
  }

  void parseGenericArgumentList(@NotNull PsiBuilder builder) {
    PsiBuilder.Marker list = builder.mark();
    expect(builder, CompactTokenTypes.LT, "Expected '<'");
    while (!builder.eof() && !at(builder, CompactTokenTypes.GT)) {
      PsiBuilder.Marker argument = builder.mark();
      if (isNatLiteral(builder.getTokenType())) {
        parseTypeSize(builder);
      } else {
        parseType(builder);
      }
      argument.done(CompactElementTypes.GENERIC_ARGUMENT);
      if (at(builder, CompactTokenTypes.COMMA)) {
        builder.advanceLexer();
      } else if (!at(builder, CompactTokenTypes.GT)) {
        builder.error("Expected ',' or '>'");
        builder.advanceLexer();
      }
    }
    expect(builder, CompactTokenTypes.GT, "Expected '>'");
    list.done(CompactElementTypes.GENERIC_ARGUMENT_LIST);
  }

  void parseSimpleParameterList(@NotNull PsiBuilder builder) {
    PsiBuilder.Marker list = builder.mark();
    if (!expect(builder, CompactTokenTypes.LPAREN, "Expected '('")) {
      list.done(CompactElementTypes.SIMPLE_PARAMETER_LIST);
      return;
    }
    while (!builder.eof() && !at(builder, CompactTokenTypes.RPAREN)) {
      parseTypedId(builder);
      if (at(builder, CompactTokenTypes.COMMA)) {
        builder.advanceLexer();
      } else if (!at(builder, CompactTokenTypes.RPAREN)) {
        builder.error("Expected ',' or ')'");
        builder.advanceLexer();
      }
    }
    expect(builder, CompactTokenTypes.RPAREN, "Expected ')'");
    list.done(CompactElementTypes.SIMPLE_PARAMETER_LIST);
  }

  void parsePatternParameterList(@NotNull PsiBuilder builder) {
    PsiBuilder.Marker list = builder.mark();
    if (!expect(builder, CompactTokenTypes.LPAREN, "Expected '('")) {
      list.done(CompactElementTypes.PATTERN_PARAMETER_LIST);
      return;
    }
    while (!builder.eof() && !at(builder, CompactTokenTypes.RPAREN)) {
      parseTypedPattern(builder);
      if (at(builder, CompactTokenTypes.COMMA)) {
        builder.advanceLexer();
      } else if (!at(builder, CompactTokenTypes.RPAREN)) {
        builder.error("Expected ',' or ')'");
        builder.advanceLexer();
      }
    }
    expect(builder, CompactTokenTypes.RPAREN, "Expected ')'");
    list.done(CompactElementTypes.PATTERN_PARAMETER_LIST);
  }

  void parseArrowParameterList(@NotNull PsiBuilder builder) {
    PsiBuilder.Marker list = builder.mark();
    if (!expect(builder, CompactTokenTypes.LPAREN, "Expected '('")) {
      list.done(CompactElementTypes.ARROW_PARAMETER_LIST);
      return;
    }
    while (!builder.eof() && !at(builder, CompactTokenTypes.RPAREN)) {
      parseOptionallyTypedPattern(builder);
      if (at(builder, CompactTokenTypes.COMMA)) {
        builder.advanceLexer();
      } else if (!at(builder, CompactTokenTypes.RPAREN)) {
        builder.error("Expected ',' or ')'");
        builder.advanceLexer();
      }
    }
    expect(builder, CompactTokenTypes.RPAREN, "Expected ')'");
    list.done(CompactElementTypes.ARROW_PARAMETER_LIST);
  }

  void parseTypedId(@NotNull PsiBuilder builder) {
    PsiBuilder.Marker typedId = builder.mark();
    expect(builder, CompactTokenTypes.IDENTIFIER, "Expected identifier");
    expect(builder, CompactTokenTypes.COLON, "Expected ':'");
    parseType(builder);
    typedId.done(CompactElementTypes.TYPED_ID);
  }

  void parseTypedPattern(@NotNull PsiBuilder builder) {
    PsiBuilder.Marker typedPattern = builder.mark();
    parsePattern(builder);
    expect(builder, CompactTokenTypes.COLON, "Expected ':'");
    parseType(builder);
    typedPattern.done(CompactElementTypes.TYPED_PATTERN);
  }

  void parseOptionallyTypedPattern(@NotNull PsiBuilder builder) {
    PsiBuilder.Marker pattern = builder.mark();
    parsePattern(builder);
    if (at(builder, CompactTokenTypes.COLON)) {
      builder.advanceLexer();
      parseType(builder);
    }
    pattern.done(CompactElementTypes.OPTIONALLY_TYPED_PATTERN);
  }

  void parsePattern(@NotNull PsiBuilder builder) {
    PsiBuilder.Marker pattern = builder.mark();
    if (at(builder, CompactTokenTypes.IDENTIFIER)) {
      builder.advanceLexer();
    } else if (at(builder, CompactTokenTypes.LBRACKET)) {
      parseBracketPattern(builder);
    } else if (at(builder, CompactTokenTypes.LBRACE)) {
      parseBracePattern(builder);
    } else {
      builder.error("Expected pattern");
    }
    pattern.done(CompactElementTypes.PATTERN);
  }

  private void parseBracketPattern(@NotNull PsiBuilder builder) {
    builder.advanceLexer();
    while (!builder.eof() && !at(builder, CompactTokenTypes.RBRACKET)) {
      if (!at(builder, CompactTokenTypes.COMMA)) {
        parsePattern(builder);
      }
      if (at(builder, CompactTokenTypes.COMMA)) {
        builder.advanceLexer();
      } else if (!at(builder, CompactTokenTypes.RBRACKET)) {
        builder.error("Expected ',' or ']'");
        builder.advanceLexer();
      }
    }
    expect(builder, CompactTokenTypes.RBRACKET, "Expected ']'");
  }

  private void parseBracePattern(@NotNull PsiBuilder builder) {
    builder.advanceLexer();
    while (!builder.eof() && !at(builder, CompactTokenTypes.RBRACE)) {
      parsePatternStructElement(builder);
      if (at(builder, CompactTokenTypes.COMMA)) {
        builder.advanceLexer();
      } else if (!at(builder, CompactTokenTypes.RBRACE)) {
        builder.error("Expected ',' or '}'");
        builder.advanceLexer();
      }
    }
    expect(builder, CompactTokenTypes.RBRACE, "Expected '}'");
  }

  void parsePatternStructElement(@NotNull PsiBuilder builder) {
    PsiBuilder.Marker element = builder.mark();
    if (builder.lookAhead(1) == CompactTokenTypes.COLON) {
      expect(builder, CompactTokenTypes.IDENTIFIER, "Expected field name");
      expect(builder, CompactTokenTypes.COLON, "Expected ':'");
      parsePattern(builder);
    } else {
      parsePattern(builder);
    }
    element.done(CompactElementTypes.PATTERN_STRUCT_ELEMENT);
  }

  void parseReturnType(@NotNull PsiBuilder builder) {
    PsiBuilder.Marker returnType = builder.mark();
    expect(builder, CompactTokenTypes.COLON, "Expected ':'");
    parseType(builder);
    returnType.done(CompactElementTypes.RETURN_TYPE);
  }

  void parseType(@NotNull PsiBuilder builder) {
    if (at(builder, CompactTokenTypes.LBRACKET)) {
      parseTupleType(builder);
      return;
    }

    PsiBuilder.Marker type = builder.mark();
    if (isBuiltinType(builder.getTokenType())) {
      parseBuiltinTypeBody(builder);
      type.done(CompactElementTypes.BUILTIN_TYPE);
      return;
    }

    if (isTypeReferenceStart(builder.getTokenType())) {
      if (at(builder, CompactTokenTypes.HASH)) {
        builder.advanceLexer();
      }
      expect(builder, CompactTokenTypes.IDENTIFIER, "Expected type reference name");
      if (at(builder, CompactTokenTypes.LT)) {
        parseGenericArgumentList(builder);
      }
    } else if (isNatLiteral(builder.getTokenType()) || at(builder, CompactTokenTypes.STRING_LITERAL)) {
      builder.advanceLexer();
    } else {
      builder.error("Expected type");
    }
    type.done(CompactElementTypes.TYPE_REFERENCE);
  }

  private void parseTupleType(@NotNull PsiBuilder builder) {
    PsiBuilder.Marker type = builder.mark();
    builder.advanceLexer();
    while (!builder.eof() && !at(builder, CompactTokenTypes.RBRACKET)) {
      parseType(builder);
      if (at(builder, CompactTokenTypes.COMMA)) {
        builder.advanceLexer();
      } else if (!at(builder, CompactTokenTypes.RBRACKET)) {
        builder.error("Expected ',' or ']'");
        builder.advanceLexer();
      }
    }
    expect(builder, CompactTokenTypes.RBRACKET, "Expected ']'");
    type.done(CompactElementTypes.TUPLE_TYPE);
  }

  void parseBuiltinTypeBody(@NotNull PsiBuilder builder) {
    IElementType builtin = builder.getTokenType();
    builder.advanceLexer();

    if (builtin == CompactTokenTypes.UINT_TYPE) {
      expect(builder, CompactTokenTypes.LT, "Expected '<'");
      parseTypeSize(builder);
      if (at(builder, CompactTokenTypes.RANGE)) {
        builder.advanceLexer();
        parseTypeSize(builder);
      }
      expect(builder, CompactTokenTypes.GT, "Expected '>'");
      return;
    }

    if (builtin == CompactTokenTypes.BYTES_TYPE) {
      expect(builder, CompactTokenTypes.LT, "Expected '<'");
      parseTypeSize(builder);
      expect(builder, CompactTokenTypes.GT, "Expected '>'");
      return;
    }

    if (builtin == CompactTokenTypes.OPAQUE_TYPE) {
      expect(builder, CompactTokenTypes.LT, "Expected '<'");
      expect(builder, CompactTokenTypes.STRING_LITERAL, "Expected opaque tag string");
      expect(builder, CompactTokenTypes.GT, "Expected '>'");
      return;
    }

    if (builtin == CompactTokenTypes.VECTOR_TYPE) {
      expect(builder, CompactTokenTypes.LT, "Expected '<'");
      parseTypeSize(builder);
      expect(builder, CompactTokenTypes.COMMA, "Expected ','");
      parseType(builder);
      expect(builder, CompactTokenTypes.GT, "Expected '>'");
    }
  }

  void parseTypeSize(@NotNull PsiBuilder builder) {
    PsiBuilder.Marker size = builder.mark();
    if (isNatLiteral(builder.getTokenType()) || at(builder, CompactTokenTypes.IDENTIFIER)) {
      builder.advanceLexer();
    } else {
      builder.error("Expected type size");
    }
    size.done(CompactElementTypes.TYPE_SIZE);
  }

  static boolean isBuiltinType(IElementType token) {
    return CompactTokenSets.BUILTIN_TYPES.contains(token);
  }

  static boolean isTypeReferenceStart(IElementType token) {
    return token == CompactTokenTypes.IDENTIFIER
            || token == CompactTokenTypes.HASH
            || token == CompactTokenTypes.MAP;
  }

  static boolean isNatLiteral(IElementType token) {
    return CompactTokenSets.NAT_LITERALS.contains(token);
  }
}
