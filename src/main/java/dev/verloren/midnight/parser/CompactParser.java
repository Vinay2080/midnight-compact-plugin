package dev.verloren.midnight.parser;

import com.intellij.lang.ASTNode;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiParser;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import org.jetbrains.annotations.NotNull;

public final class CompactParser implements PsiParser {
  private static final TokenSet TOP_LEVEL_RECOVERY = TokenSet.create(
          CompactTokenTypes.SEMICOLON,
          CompactTokenTypes.PRAGMA,
          CompactTokenTypes.IMPORT,
          CompactTokenTypes.EXPORT,
          CompactTokenTypes.INCLUDE,
          CompactTokenTypes.MODULE,
          CompactTokenTypes.CONTRACT,
          CompactTokenTypes.CIRCUIT,
          CompactTokenTypes.STRUCT,
          CompactTokenTypes.ENUM,
          CompactTokenTypes.TYPE,
          CompactTokenTypes.LEDGER,
          CompactTokenTypes.WITNESS,
          CompactTokenTypes.CONSTRUCTOR,
          CompactTokenTypes.SEALED,
          CompactTokenTypes.PURE,
          CompactTokenTypes.NEW
  );

  private static final TokenSet SEMICOLON_TERMINATOR = TokenSet.create(CompactTokenTypes.SEMICOLON);
  private static final TokenSet RPAREN_TERMINATOR = TokenSet.create(CompactTokenTypes.RPAREN);
  private static final TokenSet COMMA_OR_SEMICOLON_TERMINATOR = TokenSet.create(
          CompactTokenTypes.COMMA,
          CompactTokenTypes.SEMICOLON
  );
  private static final TokenSet COLON_TERMINATOR = TokenSet.create(CompactTokenTypes.COLON);

  private static boolean at(PsiBuilder builder, IElementType tokenType) {
    return builder.getTokenType() == tokenType;
  }

  private static boolean isProgramElementStart(PsiBuilder builder) {
    IElementType token = builder.getTokenType();
    return token == CompactTokenTypes.PRAGMA
            || token == CompactTokenTypes.INCLUDE
            || token == CompactTokenTypes.IMPORT
            || token == CompactTokenTypes.EXPORT
            || token == CompactTokenTypes.MODULE
            || token == CompactTokenTypes.CONTRACT
            || token == CompactTokenTypes.CIRCUIT
            || token == CompactTokenTypes.STRUCT
            || token == CompactTokenTypes.ENUM
            || token == CompactTokenTypes.TYPE
            || token == CompactTokenTypes.LEDGER
            || token == CompactTokenTypes.WITNESS
            || token == CompactTokenTypes.CONSTRUCTOR
            || token == CompactTokenTypes.SEALED
            || token == CompactTokenTypes.PURE
            || token == CompactTokenTypes.NEW;
  }

  private static void sync(PsiBuilder builder, TokenSet recoverySet) {
    while (!builder.eof() && !recoverySet.contains(builder.getTokenType())) {
      builder.advanceLexer();
    }
  }

  private static boolean isBuiltinType(IElementType token) {
    return token == CompactTokenTypes.BOOLEAN_TYPE
            || token == CompactTokenTypes.BYTES_TYPE
            || token == CompactTokenTypes.FIELD_TYPE
            || token == CompactTokenTypes.OPAQUE_TYPE
            || token == CompactTokenTypes.UINT_TYPE
            || token == CompactTokenTypes.VECTOR_TYPE
            || token == CompactTokenTypes.JUBJUB_SCALAR_TYPE
            || token == CompactTokenTypes.SECP256K1_BASE_TYPE
            || token == CompactTokenTypes.SECP256K1_SCALAR_TYPE;
  }

  private static boolean isTypeReferenceStart(IElementType token) {
    return token == CompactTokenTypes.IDENTIFIER
            || token == CompactTokenTypes.HASH
            || token == CompactTokenTypes.MAP;
  }

  private static boolean isNatLiteral(IElementType token) {
    return token == CompactTokenTypes.DECIMAL_LITERAL
            || token == CompactTokenTypes.BINARY_LITERAL
            || token == CompactTokenTypes.OCTAL_LITERAL
            || token == CompactTokenTypes.HEX_LITERAL;
  }

  private static boolean isLiteral(IElementType token) {
    return token == CompactTokenTypes.TRUE
            || token == CompactTokenTypes.FALSE
            || token == CompactTokenTypes.STRING_LITERAL
            || isNatLiteral(token);
  }

  @Override
  public @NotNull ASTNode parse(@NotNull IElementType root, @NotNull PsiBuilder builder) {
    PsiBuilder.Marker file = builder.mark();

    while (!builder.eof()) {
      int startOffset = builder.getCurrentOffset();
      if (!parseProgramElement(builder)) {
        builder.error("Expected Compact declaration");
        if (!builder.eof() && !TOP_LEVEL_RECOVERY.contains(builder.getTokenType())) {
          sync(builder, TOP_LEVEL_RECOVERY);
        }
        if (!builder.eof() && at(builder, CompactTokenTypes.SEMICOLON)) {
          builder.advanceLexer();
        } else if (!builder.eof() && !isProgramElementStart(builder)) {
          builder.advanceLexer();
        }
      }
      if (!builder.eof() && builder.getCurrentOffset() == startOffset) {
        builder.advanceLexer();
      }
    }

    file.done(root);
    return builder.getTreeBuilt();
  }

  private boolean parseProgramElement(PsiBuilder builder) {
    if (builder.eof()) {
      return false;
    }

    if (at(builder, CompactTokenTypes.PRAGMA)) {
      parsePragma(builder);
      return true;
    }
    if (at(builder, CompactTokenTypes.INCLUDE)) {
      parseInclude(builder);
      return true;
    }
    if (at(builder, CompactTokenTypes.IMPORT)) {
      parseImport(builder);
      return true;
    }
    if (at(builder, CompactTokenTypes.EXPORT)) {
      IElementType next = builder.lookAhead(1);
      IElementType nextAfterModifier = next == CompactTokenTypes.PURE || next == CompactTokenTypes.SEALED || next == CompactTokenTypes.NEW
              ? builder.lookAhead(2)
              : next;
      if (next == CompactTokenTypes.LBRACE) {
        parseExportForm(builder);
        return true;
      }
      if (nextAfterModifier == CompactTokenTypes.MODULE) {
        parseModule(builder, true);
        return true;
      }
      if (nextAfterModifier == CompactTokenTypes.STRUCT) {
        parseStruct(builder, true);
        return true;
      }
      if (nextAfterModifier == CompactTokenTypes.ENUM) {
        parseEnum(builder, true);
        return true;
      }
      if (nextAfterModifier == CompactTokenTypes.CONTRACT) {
        parseExternalContract(builder, true);
        return true;
      }
      if (nextAfterModifier == CompactTokenTypes.CONST) {
        parseConstStatement(builder, true);
        return true;
      }
      if (nextAfterModifier == CompactTokenTypes.TYPE) {
        parseTypeAlias(builder, true);
        return true;
      }
      if (nextAfterModifier == CompactTokenTypes.LEDGER) {
        parseLedger(builder, true);
        return true;
      }
      if (nextAfterModifier == CompactTokenTypes.WITNESS) {
        parseWitness(builder, true);
        return true;
      }
      if (nextAfterModifier == CompactTokenTypes.CIRCUIT) {
        parseCircuit(builder, true);
        return true;
      }
      return false;
    }
    if (at(builder, CompactTokenTypes.MODULE)) {
      parseModule(builder, false);
      return true;
    }
    if (at(builder, CompactTokenTypes.STRUCT)) {
      parseStruct(builder, false);
      return true;
    }
    if (at(builder, CompactTokenTypes.ENUM)) {
      parseEnum(builder, false);
      return true;
    }
    if (at(builder, CompactTokenTypes.CONTRACT)) {
      if (builder.lookAhead(1) == CompactTokenTypes.IMPLEMENTS) {
        parseImplements(builder);
      } else {
        parseExternalContract(builder, false);
      }
      return true;
    }
    if (at(builder, CompactTokenTypes.TYPE) || at(builder, CompactTokenTypes.NEW)) {
      parseTypeAlias(builder, false);
      return true;
    }
    if (at(builder, CompactTokenTypes.LEDGER) || at(builder, CompactTokenTypes.SEALED)) {
      parseLedger(builder, false);
      return true;
    }
    if (at(builder, CompactTokenTypes.WITNESS)) {
      parseWitness(builder, false);
      return true;
    }
    if (at(builder, CompactTokenTypes.CONST)) {
      parseConstStatement(builder, false);
      return true;
    }
    if (at(builder, CompactTokenTypes.CONSTRUCTOR)) {
      parseConstructor(builder);
      return true;
    }
    if (at(builder, CompactTokenTypes.CIRCUIT) || at(builder, CompactTokenTypes.PURE)) {
      parseCircuit(builder, false);
      return true;
    }

    return false;
  }

  private void parseInclude(PsiBuilder builder) {
    PsiBuilder.Marker include = builder.mark();
    expect(builder, CompactTokenTypes.INCLUDE, "Expected 'include'");
    expect(builder, CompactTokenTypes.STRING_LITERAL, "Expected include file string");
    expect(builder, CompactTokenTypes.SEMICOLON, "Expected ';'");
    include.done(CompactElementTypes.INCLUDE_FORM);
  }

  private void parseImport(PsiBuilder builder) {
    PsiBuilder.Marker importForm = builder.mark();
    expect(builder, CompactTokenTypes.IMPORT, "Expected 'import'");
    if (at(builder, CompactTokenTypes.LBRACE)) {
      parseImportSelection(builder);
    }
    if (at(builder, CompactTokenTypes.IDENTIFIER) || at(builder, CompactTokenTypes.STRING_LITERAL)) {
      builder.advanceLexer();
    } else {
      builder.error("Expected import name");
    }
    if (at(builder, CompactTokenTypes.LT)) {
      parseGenericArgumentList(builder);
    }
    if (at(builder, CompactTokenTypes.PREFIX)) {
      PsiBuilder.Marker prefix = builder.mark();
      builder.advanceLexer();
      expect(builder, CompactTokenTypes.IDENTIFIER, "Expected import prefix name");
      prefix.done(CompactElementTypes.IMPORT_PREFIX);
    }
    expect(builder, CompactTokenTypes.SEMICOLON, "Expected ';'");
    importForm.done(CompactElementTypes.IMPORT_FORM);
  }

  private void parseImportSelection(PsiBuilder builder) {
    PsiBuilder.Marker selection = builder.mark();
    expect(builder, CompactTokenTypes.LBRACE, "Expected '{'");
    while (!builder.eof() && !at(builder, CompactTokenTypes.RBRACE)) {
      parseImportElement(builder);
      if (at(builder, CompactTokenTypes.COMMA)) {
        builder.advanceLexer();
      } else if (!at(builder, CompactTokenTypes.RBRACE)) {
        builder.error("Expected ',' or '}'");
        builder.advanceLexer();
      }
    }
    expect(builder, CompactTokenTypes.RBRACE, "Expected '}'");
    expect(builder, CompactTokenTypes.FROM, "Expected 'from'");
    selection.done(CompactElementTypes.IMPORT_SELECTION);
  }

  private void parseImportElement(PsiBuilder builder) {
    PsiBuilder.Marker element = builder.mark();
    expect(builder, CompactTokenTypes.IDENTIFIER, "Expected imported name");
    if (at(builder, CompactTokenTypes.AS)) {
      builder.advanceLexer();
      expect(builder, CompactTokenTypes.IDENTIFIER, "Expected import alias");
    }
    element.done(CompactElementTypes.IMPORT_ELEMENT);
  }

  private void parseExportForm(PsiBuilder builder) {
    PsiBuilder.Marker export = builder.mark();
    expect(builder, CompactTokenTypes.EXPORT, "Expected 'export'");
    expect(builder, CompactTokenTypes.LBRACE, "Expected '{'");
    parseIdentifierList(builder, CompactTokenTypes.RBRACE);
    expect(builder, CompactTokenTypes.RBRACE, "Expected '}'");
    if (at(builder, CompactTokenTypes.SEMICOLON)) {
      builder.advanceLexer();
    }
    export.done(CompactElementTypes.EXPORT_FORM);
  }

  private void parseModule(PsiBuilder builder, boolean exported) {
    PsiBuilder.Marker module = builder.mark();
    if (exported) {
      expect(builder, CompactTokenTypes.EXPORT, "Expected 'export'");
    }
    expect(builder, CompactTokenTypes.MODULE, "Expected 'module'");
    expect(builder, CompactTokenTypes.IDENTIFIER, "Expected module name");
    if (at(builder, CompactTokenTypes.LT)) {
      parseGenericParameterList(builder);
    }
    if (expect(builder, CompactTokenTypes.LBRACE, "Expected '{'")) {
      while (!builder.eof() && !at(builder, CompactTokenTypes.RBRACE)) {
        if (!parseProgramElement(builder)) {
          errorAndAdvance(builder, "Expected module member declaration");
        }
      }
      expect(builder, CompactTokenTypes.RBRACE, "Expected '}'");
    }
    module.done(CompactElementTypes.MODULE_DEFINITION);
  }

  private void parseStruct(PsiBuilder builder, boolean exported) {
    PsiBuilder.Marker struct = builder.mark();
    if (exported) {
      expect(builder, CompactTokenTypes.EXPORT, "Expected 'export'");
    }
    expect(builder, CompactTokenTypes.STRUCT, "Expected 'struct'");
    expect(builder, CompactTokenTypes.IDENTIFIER, "Expected struct name");
    if (at(builder, CompactTokenTypes.LT)) {
      parseGenericParameterList(builder);
    }
    if (expect(builder, CompactTokenTypes.LBRACE, "Expected '{'")) {
      while (!builder.eof() && !at(builder, CompactTokenTypes.RBRACE)) {
        PsiBuilder.Marker field = builder.mark();
        parseTypedId(builder);
        field.done(CompactElementTypes.STRUCT_FIELD);
        if (at(builder, CompactTokenTypes.SEMICOLON) || at(builder, CompactTokenTypes.COMMA)) {
          builder.advanceLexer();
        } else if (!at(builder, CompactTokenTypes.RBRACE)) {
          builder.error("Expected field separator");
          builder.advanceLexer();
        }
      }
      expect(builder, CompactTokenTypes.RBRACE, "Expected '}'");
    }
    if (at(builder, CompactTokenTypes.SEMICOLON)) {
      builder.advanceLexer();
    }
    struct.done(CompactElementTypes.STRUCT_DECLARATION);
  }

  private void parseEnum(PsiBuilder builder, boolean exported) {
    PsiBuilder.Marker enumDecl = builder.mark();
    if (exported) {
      expect(builder, CompactTokenTypes.EXPORT, "Expected 'export'");
    }
    expect(builder, CompactTokenTypes.ENUM, "Expected 'enum'");
    expect(builder, CompactTokenTypes.IDENTIFIER, "Expected enum name");
    if (expect(builder, CompactTokenTypes.LBRACE, "Expected '{'")) {
      while (!builder.eof() && !at(builder, CompactTokenTypes.RBRACE)) {
        PsiBuilder.Marker member = builder.mark();
        expect(builder, CompactTokenTypes.IDENTIFIER, "Expected enum member");
        member.done(CompactElementTypes.ENUM_MEMBER);
        if (at(builder, CompactTokenTypes.COMMA)) {
          builder.advanceLexer();
        } else if (!at(builder, CompactTokenTypes.RBRACE)) {
          builder.error("Expected ',' or '}'");
          builder.advanceLexer();
        }
      }
      expect(builder, CompactTokenTypes.RBRACE, "Expected '}'");
    }
    if (at(builder, CompactTokenTypes.SEMICOLON)) {
      builder.advanceLexer();
    }
    enumDecl.done(CompactElementTypes.ENUM_DECLARATION);
  }

  private void parseExternalContract(PsiBuilder builder, boolean exported) {
    PsiBuilder.Marker contract = builder.mark();
    if (exported) {
      expect(builder, CompactTokenTypes.EXPORT, "Expected 'export'");
    }
    expect(builder, CompactTokenTypes.CONTRACT, "Expected 'contract'");
    expect(builder, CompactTokenTypes.IDENTIFIER, "Expected contract name");
    if (expect(builder, CompactTokenTypes.LBRACE, "Expected '{'")) {
      while (!builder.eof() && !at(builder, CompactTokenTypes.RBRACE)) {
        parseExternalCircuit(builder);
        if (at(builder, CompactTokenTypes.SEMICOLON) || at(builder, CompactTokenTypes.COMMA)) {
          builder.advanceLexer();
        } else if (!at(builder, CompactTokenTypes.RBRACE)) {
          builder.error("Expected circuit separator");
          builder.advanceLexer();
        }
      }
      expect(builder, CompactTokenTypes.RBRACE, "Expected '}'");
    }
    if (at(builder, CompactTokenTypes.SEMICOLON)) {
      builder.advanceLexer();
    }
    contract.done(CompactElementTypes.CONTRACT_DECLARATION);
  }

  private void parseExternalCircuit(PsiBuilder builder) {
    PsiBuilder.Marker circuit = builder.mark();
    if (at(builder, CompactTokenTypes.PURE)) {
      builder.advanceLexer();
    }
    expect(builder, CompactTokenTypes.CIRCUIT, "Expected 'circuit'");
    expect(builder, CompactTokenTypes.IDENTIFIER, "Expected circuit name");
    parseSimpleParameterList(builder);
    expect(builder, CompactTokenTypes.COLON, "Expected ':'");
    parseType(builder);
    circuit.done(CompactElementTypes.EXTERNAL_CIRCUIT);
  }

  private void parseImplements(PsiBuilder builder) {
    PsiBuilder.Marker implementsDecl = builder.mark();
    expect(builder, CompactTokenTypes.CONTRACT, "Expected 'contract'");
    expect(builder, CompactTokenTypes.IMPLEMENTS, "Expected 'implements'");
    parseType(builder);
    expect(builder, CompactTokenTypes.SEMICOLON, "Expected ';'");
    implementsDecl.done(CompactElementTypes.IMPLEMENTS_DECLARATION);
  }

  private boolean expectIdentifier(PsiBuilder builder, String errorMessage) {
    if (at(builder, CompactTokenTypes.IDENTIFIER) || isBuiltinType(builder.getTokenType())) {
      builder.advanceLexer();
      return true;
    }
    builder.error(errorMessage);
    return false;
  }

  private void parseTypeAlias(PsiBuilder builder, boolean exported) {
    PsiBuilder.Marker typeAlias = builder.mark();
    if (exported) {
      expect(builder, CompactTokenTypes.EXPORT, "Expected 'export'");
    }
    if (at(builder, CompactTokenTypes.NEW)) {
      builder.advanceLexer();
    }
    expect(builder, CompactTokenTypes.TYPE, "Expected 'type'");
    expectIdentifier(builder, "Expected type name");
    if (at(builder, CompactTokenTypes.LT)) {
      parseGenericParameterList(builder);
    }
    expect(builder, CompactTokenTypes.ASSIGN, "Expected '='");
    parseType(builder);
    expect(builder, CompactTokenTypes.SEMICOLON, "Expected ';'");
    typeAlias.done(CompactElementTypes.TYPE_ALIAS_DECLARATION);
  }

  private void parseLedger(PsiBuilder builder, boolean exported) {
    PsiBuilder.Marker ledger = builder.mark();
    if (exported) {
      expect(builder, CompactTokenTypes.EXPORT, "Expected 'export'");
    }
    if (at(builder, CompactTokenTypes.SEALED)) {
      builder.advanceLexer();
    }
    expect(builder, CompactTokenTypes.LEDGER, "Expected 'ledger'");
    expect(builder, CompactTokenTypes.IDENTIFIER, "Expected ledger name");
    expect(builder, CompactTokenTypes.COLON, "Expected ':'");
    parseType(builder);
    expect(builder, CompactTokenTypes.SEMICOLON, "Expected ';'");
    ledger.done(CompactElementTypes.LEDGER_DECLARATION);
  }

  private void parseWitness(PsiBuilder builder, boolean exported) {
    PsiBuilder.Marker witness = builder.mark();
    if (exported) {
      expect(builder, CompactTokenTypes.EXPORT, "Expected 'export'");
    }
    expect(builder, CompactTokenTypes.WITNESS, "Expected 'witness'");
    expect(builder, CompactTokenTypes.IDENTIFIER, "Expected witness name");
    if (at(builder, CompactTokenTypes.LT)) {
      parseGenericParameterList(builder);
    }
    parseSimpleParameterList(builder);
    expect(builder, CompactTokenTypes.COLON, "Expected ':'");
    parseType(builder);
    expect(builder, CompactTokenTypes.SEMICOLON, "Expected ';'");
    witness.done(CompactElementTypes.WITNESS_DECLARATION);
  }

  private void parseConstructor(PsiBuilder builder) {
    PsiBuilder.Marker constructor = builder.mark();
    expect(builder, CompactTokenTypes.CONSTRUCTOR, "Expected 'constructor'");
    parsePatternParameterList(builder);
    parseBlock(builder);
    constructor.done(CompactElementTypes.CONSTRUCTOR_DEFINITION);
  }

  private void parseCircuit(PsiBuilder builder, boolean exported) {
    PsiBuilder.Marker circuit = builder.mark();
    if (exported) {
      expect(builder, CompactTokenTypes.EXPORT, "Expected 'export'");
    }
    if (at(builder, CompactTokenTypes.PURE)) {
      builder.advanceLexer();
    }
    expect(builder, CompactTokenTypes.CIRCUIT, "Expected 'circuit'");
    expect(builder, CompactTokenTypes.IDENTIFIER, "Expected circuit name");
    if (at(builder, CompactTokenTypes.LT)) {
      parseGenericParameterList(builder);
    }
    parsePatternParameterList(builder);
    expect(builder, CompactTokenTypes.COLON, "Expected ':'");
    parseType(builder);
    parseBlock(builder);
    circuit.done(CompactElementTypes.CIRCUIT_DEFINITION);
  }

  /**
   * pragma_form ::=
   * PRAGMA
   * pragma_identifier
   * version_expression
   * SEMICOLON
   *
   */
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

  private void parseGenericParameterList(PsiBuilder builder) {
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

  private void parseGenericArgumentList(PsiBuilder builder) {
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

  private void parseSimpleParameterList(PsiBuilder builder) {
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

  private void parsePatternParameterList(PsiBuilder builder) {
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

  private void parseTypedId(PsiBuilder builder) {
    PsiBuilder.Marker typedId = builder.mark();
    expect(builder, CompactTokenTypes.IDENTIFIER, "Expected identifier");
    expect(builder, CompactTokenTypes.COLON, "Expected ':'");
    parseType(builder);
    typedId.done(CompactElementTypes.TYPED_ID);
  }

  private void parseTypedPattern(PsiBuilder builder) {
    PsiBuilder.Marker typedPattern = builder.mark();
    parsePattern(builder);
    expect(builder, CompactTokenTypes.COLON, "Expected ':'");
    parseType(builder);
    typedPattern.done(CompactElementTypes.TYPED_PATTERN);
  }

  private void parsePattern(PsiBuilder builder) {
    PsiBuilder.Marker pattern = builder.mark();
    if (at(builder, CompactTokenTypes.IDENTIFIER)) {
      builder.advanceLexer();
    } else if (at(builder, CompactTokenTypes.LBRACKET)) {
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
    } else if (at(builder, CompactTokenTypes.LBRACE)) {
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
    } else {
      builder.error("Expected pattern");
    }
    pattern.done(CompactElementTypes.PATTERN);
  }

  private void parsePatternStructElement(PsiBuilder builder) {
    PsiBuilder.Marker element = builder.mark();
    expect(builder, CompactTokenTypes.IDENTIFIER, "Expected field name");
    if (at(builder, CompactTokenTypes.COLON)) {
      builder.advanceLexer();
      parsePattern(builder);
    }
    element.done(CompactElementTypes.PATTERN_STRUCT_ELEMENT);
  }

  private void parseArrowParameterList(PsiBuilder builder) {
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

  private void parseOptionallyTypedPattern(PsiBuilder builder) {
    PsiBuilder.Marker pattern = builder.mark();
    parsePattern(builder);
    if (at(builder, CompactTokenTypes.COLON)) {
      builder.advanceLexer();
      parseType(builder);
    }
    pattern.done(CompactElementTypes.OPTIONALLY_TYPED_PATTERN);
  }

  private void parseReturnType(PsiBuilder builder) {
    PsiBuilder.Marker returnType = builder.mark();
    expect(builder, CompactTokenTypes.COLON, "Expected ':'");
    parseType(builder);
    returnType.done(CompactElementTypes.RETURN_TYPE);
  }

  private void parseType(PsiBuilder builder) {
    PsiBuilder.Marker type;
    if (at(builder, CompactTokenTypes.LBRACKET)) {
      type = builder.mark();
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
      return;
    }

    type = builder.mark();
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

  private void parseBuiltinTypeBody(PsiBuilder builder) {
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

  private void parseTypeSize(PsiBuilder builder) {
    PsiBuilder.Marker size = builder.mark();
    if (isNatLiteral(builder.getTokenType()) || at(builder, CompactTokenTypes.IDENTIFIER)) {
      builder.advanceLexer();
    } else {
      builder.error("Expected type size");
    }
    size.done(CompactElementTypes.TYPE_SIZE);
  }

  private void parseBlock(PsiBuilder builder) {
    PsiBuilder.Marker block = builder.mark();
    if (!expect(builder, CompactTokenTypes.LBRACE, "Expected '{'")) {
      block.done(CompactElementTypes.BLOCK);
      return;
    }
    while (!builder.eof() && !at(builder, CompactTokenTypes.RBRACE)) {
      if (!parseStatement(builder)) {
        errorAndAdvance(builder, "Expected statement");
      }
    }
    expect(builder, CompactTokenTypes.RBRACE, "Expected '}'");
    block.done(CompactElementTypes.BLOCK);
  }

  private boolean parseStatement(PsiBuilder builder) {
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

  private void parseIfStatement(PsiBuilder builder) {
    PsiBuilder.Marker statement = builder.mark();
    expect(builder, CompactTokenTypes.IF, "Expected 'if'");
    expect(builder, CompactTokenTypes.LPAREN, "Expected '('");
    parseExpressionSequenceUntil(RPAREN_TERMINATOR, builder);
    expect(builder, CompactTokenTypes.RPAREN, "Expected ')'");
    parseStatement(builder);
    if (at(builder, CompactTokenTypes.ELSE)) {
      builder.advanceLexer();
      parseStatement(builder);
    }
    statement.done(CompactElementTypes.IF_STATEMENT);
  }

  private void parseForStatement(PsiBuilder builder) {
    PsiBuilder.Marker statement = builder.mark();
    expect(builder, CompactTokenTypes.FOR, "Expected 'for'");
    expect(builder, CompactTokenTypes.LPAREN, "Expected '('");
    expect(builder, CompactTokenTypes.CONST, "Expected 'const'");
    expect(builder, CompactTokenTypes.IDENTIFIER, "Expected loop variable");
    expect(builder, CompactTokenTypes.OF, "Expected 'of'");
    if ((isNatLiteral(builder.getTokenType()) || at(builder, CompactTokenTypes.IDENTIFIER))
            && builder.lookAhead(1) == CompactTokenTypes.RANGE) {
      parseTypeSize(builder);
      expect(builder, CompactTokenTypes.RANGE, "Expected '..'");
      parseTypeSize(builder);
    } else {
      parseExpressionSequenceUntil(RPAREN_TERMINATOR, builder);
    }
    expect(builder, CompactTokenTypes.RPAREN, "Expected ')'");
    parseStatement(builder);
    statement.done(CompactElementTypes.FOR_STATEMENT);
  }

  private void parseConstStatement(PsiBuilder builder, boolean exported) {
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

  private void parseConstStatement(PsiBuilder builder) {
    parseConstStatement(builder, false);
  }

  private void parseConstBinding(PsiBuilder builder) {
    PsiBuilder.Marker binding = builder.mark();
    parseOptionallyTypedPattern(builder);
    expect(builder, CompactTokenTypes.ASSIGN, "Expected '='");
    parseExpressionSequenceUntil(COMMA_OR_SEMICOLON_TERMINATOR, builder);
    binding.done(CompactElementTypes.CONST_BINDING);
  }

  private void parseReturnStatement(PsiBuilder builder) {
    PsiBuilder.Marker statement = builder.mark();
    expect(builder, CompactTokenTypes.RETURN, "Expected 'return'");
    if (!at(builder, CompactTokenTypes.SEMICOLON)) {
      parseExpressionSequenceUntil(SEMICOLON_TERMINATOR, builder);
    }
    expect(builder, CompactTokenTypes.SEMICOLON, "Expected ';'");
    statement.done(CompactElementTypes.RETURN_STATEMENT);
  }

  private void parseExpressionStatement(PsiBuilder builder) {
    PsiBuilder.Marker statement = builder.mark();
    parseExpressionSequenceUntil(SEMICOLON_TERMINATOR, builder);
    expect(builder, CompactTokenTypes.SEMICOLON, "Expected ';'");
    statement.done(CompactElementTypes.EXPR_STATEMENT);
  }

  private void parseExpressionSequenceUntil(TokenSet terminators, PsiBuilder builder) {
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

  private PsiBuilder.Marker parseExpression(PsiBuilder builder) {
    return parseAssignmentExpression(builder);
  }

  private PsiBuilder.Marker parseAssignmentExpression(PsiBuilder builder) {
    PsiBuilder.Marker left = parseTernaryExpression(builder);
    if (at(builder, CompactTokenTypes.ASSIGN)
            || at(builder, CompactTokenTypes.PLUS_ASSIGN)
            || at(builder, CompactTokenTypes.MINUS_ASSIGN)) {
      PsiBuilder.Marker assignment = left.precede();
      builder.advanceLexer();
      parseAssignmentExpression(builder);
      assignment.done(CompactElementTypes.ASSIGN_EXPR);
      return assignment;
    }
    return left;
  }

  private PsiBuilder.Marker parseTernaryExpression(PsiBuilder builder) {
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

  private PsiBuilder.Marker parseBinaryExpression(PsiBuilder builder, int minPrecedence) {
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
        parseType(builder);
        expression.done(CompactElementTypes.CAST_EXPR);
      } else {
        parseBinaryExpression(builder, precedence + 1);
        expression.done(CompactElementTypes.BINARY_EXPR);
      }
      left = expression;
    }
    return left;
  }

  private PsiBuilder.Marker parseUnaryExpression(PsiBuilder builder) {
    if (at(builder, CompactTokenTypes.NOT) || at(builder, CompactTokenTypes.MINUS)) {
      PsiBuilder.Marker unary = builder.mark();
      builder.advanceLexer();
      parseUnaryExpression(builder);
      unary.done(CompactElementTypes.UNARY_EXPR);
      return unary;
    }
    return parsePostfixExpression(builder);
  }

  private PsiBuilder.Marker parsePostfixExpression(PsiBuilder builder) {
    PsiBuilder.Marker expression = parsePrimaryExpression(builder);
    while (!builder.eof()) {
      if (at(builder, CompactTokenTypes.LBRACKET)) {
        PsiBuilder.Marker index = expression.precede();
        builder.advanceLexer();
        parseExpressionSequenceUntil(TokenSet.create(CompactTokenTypes.RBRACKET), builder);
        expect(builder, CompactTokenTypes.RBRACKET, "Expected ']'");
        index.done(CompactElementTypes.INDEX_EXPR);
        expression = index;
      } else if (at(builder, CompactTokenTypes.DOT)) {
        PsiBuilder.Marker member = expression.precede();
        builder.advanceLexer();
        expect(builder, CompactTokenTypes.IDENTIFIER, "Expected member name");
        if (at(builder, CompactTokenTypes.LPAREN)) {
          parseArgumentList(builder);
          member.done(CompactElementTypes.CALL_EXPR);
        } else {
          member.done(CompactElementTypes.MEMBER_EXPR);
        }
        expression = member;
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

  private PsiBuilder.Marker parsePrimaryExpression(PsiBuilder builder) {
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
    if (at(builder, CompactTokenTypes.LBRACKET)) {
      return parseTupleExpression(builder);
    }
    if (at(builder, CompactTokenTypes.BYTES_TYPE) && builder.lookAhead(1) == CompactTokenTypes.LBRACKET) {
      return parseBytesExpression(builder);
    }
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
    errorAndAdvance(builder, "Expected expression");
    error.done(CompactElementTypes.LITERAL_EXPR);
    return error;
  }

  private boolean looksLikeLambdaExpression(PsiBuilder builder) {
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

  private PsiBuilder.Marker tryParseLambdaExpression(PsiBuilder builder) {
    PsiBuilder.Marker rollback = builder.mark();
    PsiBuilder.Marker lambda = builder.mark();
    parseArrowParameterList(builder);
    if (at(builder, CompactTokenTypes.COLON)) {
      parseReturnType(builder);
    }
    if (!at(builder, CompactTokenTypes.ARROW)) {
      lambda.drop();
      rollback.rollbackTo();
      return null;
    }

    builder.advanceLexer();
    if (at(builder, CompactTokenTypes.LBRACE)) {
      parseBlock(builder);
    } else {
      parseExpression(builder);
    }
    lambda.done(CompactElementTypes.LAMBDA_EXPR);
    rollback.drop();
    return lambda;
  }

  private PsiBuilder.Marker parseReferenceLikeExpression(PsiBuilder builder) {
    PsiBuilder.Marker expression = builder.mark();
    builder.advanceLexer();
    boolean hasCommittedGenericArguments = false;
    if (at(builder, CompactTokenTypes.LT)) {
      PsiBuilder.Marker genericAttempt = builder.mark();
      parseGenericArgumentList(builder);
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

  private PsiBuilder.Marker parseTupleExpression(PsiBuilder builder) {
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

  private void parseTupleArgument(PsiBuilder builder) {
    PsiBuilder.Marker argument = builder.mark();
    if (at(builder, CompactTokenTypes.SPREAD)) {
      builder.advanceLexer();
    }
    parseExpression(builder);
    argument.done(CompactElementTypes.TUPLE_ARG);
  }

  private PsiBuilder.Marker parseBytesExpression(PsiBuilder builder) {
    PsiBuilder.Marker bytes = builder.mark();
    expect(builder, CompactTokenTypes.BYTES_TYPE, "Expected 'Bytes'");
    expect(builder, CompactTokenTypes.LBRACKET, "Expected '['");
    parseExpressionSequenceUntil(TokenSet.create(CompactTokenTypes.RBRACKET), builder);
    expect(builder, CompactTokenTypes.RBRACKET, "Expected ']'");
    bytes.done(CompactElementTypes.BYTES_EXPR);
    return bytes;
  }

  private PsiBuilder.Marker parseKeywordCallExpression(PsiBuilder builder, IElementType expressionType) {
    PsiBuilder.Marker expression = builder.mark();
    builder.advanceLexer();
    parseArgumentList(builder);
    expression.done(expressionType);
    return expression;
  }

  private PsiBuilder.Marker parseDefaultExpression(PsiBuilder builder) {
    PsiBuilder.Marker expression = builder.mark();
    expect(builder, CompactTokenTypes.DEFAULT, "Expected 'default'");
    expect(builder, CompactTokenTypes.LT, "Expected '<'");
    parseType(builder);
    expect(builder, CompactTokenTypes.GT, "Expected '>'");
    expression.done(CompactElementTypes.DEFAULT_EXPR);
    return expression;
  }

  private PsiBuilder.Marker parseSliceExpression(PsiBuilder builder) {
    PsiBuilder.Marker expression = builder.mark();
    expect(builder, CompactTokenTypes.SLICE, "Expected 'slice'");
    expect(builder, CompactTokenTypes.LT, "Expected '<'");
    parseTypeSize(builder);
    expect(builder, CompactTokenTypes.GT, "Expected '>'");
    parseArgumentList(builder);
    expression.done(CompactElementTypes.SLICE_EXPR);
    return expression;
  }

  private void parseArgumentList(PsiBuilder builder) {
    expect(builder, CompactTokenTypes.LPAREN, "Expected '('");
    parseExpressionSequenceUntil(RPAREN_TERMINATOR, builder);
    expect(builder, CompactTokenTypes.RPAREN, "Expected ')'");
  }

  private void parseStructArgumentList(PsiBuilder builder) {
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

  private void parseStructArgument(PsiBuilder builder) {
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

  private void parseIdentifierList(PsiBuilder builder, IElementType terminator) {
    while (!builder.eof() && !at(builder, terminator)) {
      expect(builder, CompactTokenTypes.IDENTIFIER, "Expected identifier");
      if (at(builder, CompactTokenTypes.COMMA)) {
        builder.advanceLexer();
      } else if (!at(builder, terminator)) {
        builder.error("Expected ',' or terminator");
        builder.advanceLexer();
      }
    }
  }

  /**
   * version_expression ::=
   * version_constraint
   * (logical_operator version_constraint)*
   */
  private boolean parseVersionExpression(PsiBuilder builder) {

    do {
      if (!parseVersionConstraint(builder)) {
        return false;
      }
    } while (parseLogicalOperator(builder));

    return true;
  }

  /**
   * logical_operator ::=
   * AND
   * | OR
   */
  private boolean parseLogicalOperator(PsiBuilder builder) {
    if (at(builder, CompactTokenTypes.ANDAND)
            || at(builder, CompactTokenTypes.OROR)) {
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

  /**
   * version_constraint ::=
   * version_operator? VERSION
   */
  private boolean parseVersionConstraint(PsiBuilder builder) {
    parseComparisonOperator(builder);
    return expectVersion(builder);
  }

  /**
   * version_operator ::=
   * GT
   * | GTE
   * | LT
   * | LTE
   * | NOT
   */
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

  /**
   * pragma_identifier ::= IDENTIFIER
   * only 'language_version' and 'compiler_version' are allowed
   */
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

  /**
   * version ::= VERSION
   */
  private boolean expectVersion(PsiBuilder builder) {
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

  private boolean expect(PsiBuilder builder, IElementType tokenType, String message) {
    if (!at(builder, tokenType)) {
      builder.error(message);
      return false;
    }

    builder.advanceLexer();
    return true;
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
    if (!builder.eof()) {
      builder.advanceLexer();
    }
  }

}
