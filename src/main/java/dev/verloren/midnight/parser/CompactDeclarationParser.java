package dev.verloren.midnight.parser;

import com.intellij.lang.PsiBuilder;
import com.intellij.psi.tree.IElementType;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import org.jetbrains.annotations.NotNull;

import static dev.verloren.midnight.parser.CompactParserUtil.at;
import static dev.verloren.midnight.parser.CompactParserUtil.expect;

/**
 * Parser delegate responsible for top-level Compact declarations:
 * modules, structs, enums, contracts, ledgers, witnesses, and circuits.
 */
final class CompactDeclarationParser {
  private final CompactParserContext context;

  CompactDeclarationParser(@NotNull CompactParserContext context) {
    this.context = context;
  }

  boolean parseProgramElement(@NotNull PsiBuilder builder) {
    if (builder.eof()) {
      return false;
    }
    if (at(builder, CompactTokenTypes.PRAGMA)) {
      context.pragma.parsePragma(builder);
      return true;
    }
    if (at(builder, CompactTokenTypes.INCLUDE)) {
      context.importExport.parseInclude(builder);
      return true;
    }
    if (at(builder, CompactTokenTypes.IMPORT)) {
      context.importExport.parseImport(builder);
      return true;
    }
    if (at(builder, CompactTokenTypes.EXPORT)) {
      return parseExportedElement(builder);
    }
    return parseStandardElement(builder);
  }

  private boolean parseExportedElement(@NotNull PsiBuilder builder) {
    IElementType next = builder.lookAhead(1);
    IElementType nextAfterModifier = next == CompactTokenTypes.PURE || next == CompactTokenTypes.SEALED || next == CompactTokenTypes.NEW
            ? builder.lookAhead(2)
            : next;
    if (next == CompactTokenTypes.LBRACE) {
      context.importExport.parseExportForm(builder);
      return true;
    }
    return parseExportedDeclaration(builder, nextAfterModifier);
  }

  private boolean parseExportedDeclaration(@NotNull PsiBuilder builder, IElementType nextAfterModifier) {
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
      context.statement.parseConstStatement(builder, true);
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

  private boolean parseStandardElement(@NotNull PsiBuilder builder) {
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
      parseContractOrImplements(builder);
      return true;
    }
    return parseStateOrRoutineElement(builder);
  }

  private boolean parseStateOrRoutineElement(@NotNull PsiBuilder builder) {
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
      context.statement.parseConstStatement(builder, false);
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

  private void parseContractOrImplements(@NotNull PsiBuilder builder) {
    if (builder.lookAhead(1) == CompactTokenTypes.IMPLEMENTS) {
      parseImplements(builder);
    } else {
      parseExternalContract(builder, false);
    }
  }

  private void parseModule(@NotNull PsiBuilder builder, boolean exported) {
    PsiBuilder.Marker module = builder.mark();
    if (exported) {
      expect(builder, CompactTokenTypes.EXPORT, "Expected 'export'");
    }
    expect(builder, CompactTokenTypes.MODULE, "Expected 'module'");
    expect(builder, CompactTokenTypes.IDENTIFIER, "Expected module name");
    if (at(builder, CompactTokenTypes.LT)) {
      context.typePattern.parseGenericParameterList(builder);
    }
    if (expect(builder, CompactTokenTypes.LBRACE, "Expected '{'")) {
      while (!builder.eof() && !at(builder, CompactTokenTypes.RBRACE)) {
        if (!parseProgramElement(builder)) {
          CompactParserUtil.errorAndAdvance(builder, "Expected module member declaration");
        }
      }
      expect(builder, CompactTokenTypes.RBRACE, "Expected '}'");
    }
    module.done(CompactElementTypes.MODULE_DEFINITION);
  }

  private void parseStruct(@NotNull PsiBuilder builder, boolean exported) {
    PsiBuilder.Marker struct = builder.mark();
    if (exported) {
      expect(builder, CompactTokenTypes.EXPORT, "Expected 'export'");
    }
    expect(builder, CompactTokenTypes.STRUCT, "Expected 'struct'");
    expect(builder, CompactTokenTypes.IDENTIFIER, "Expected struct name");
    if (at(builder, CompactTokenTypes.LT)) {
      context.typePattern.parseGenericParameterList(builder);
    }
    if (expect(builder, CompactTokenTypes.LBRACE, "Expected '{'")) {
      while (!builder.eof() && !at(builder, CompactTokenTypes.RBRACE)) {
        PsiBuilder.Marker field = builder.mark();
        context.typePattern.parseTypedId(builder);
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

  private void parseEnum(@NotNull PsiBuilder builder, boolean exported) {
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

  private void parseExternalContract(@NotNull PsiBuilder builder, boolean exported) {
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

  private void parseExternalCircuit(@NotNull PsiBuilder builder) {
    PsiBuilder.Marker circuit = builder.mark();
    if (at(builder, CompactTokenTypes.PURE)) {
      builder.advanceLexer();
    }
    expect(builder, CompactTokenTypes.CIRCUIT, "Expected 'circuit'");
    expect(builder, CompactTokenTypes.IDENTIFIER, "Expected circuit name");
    context.typePattern.parseSimpleParameterList(builder);
    expect(builder, CompactTokenTypes.COLON, "Expected ':'");
    context.typePattern.parseType(builder);
    circuit.done(CompactElementTypes.EXTERNAL_CIRCUIT);
  }

  private void parseImplements(@NotNull PsiBuilder builder) {
    PsiBuilder.Marker implementsDecl = builder.mark();
    expect(builder, CompactTokenTypes.CONTRACT, "Expected 'contract'");
    expect(builder, CompactTokenTypes.IMPLEMENTS, "Expected 'implements'");
    context.typePattern.parseType(builder);
    expect(builder, CompactTokenTypes.SEMICOLON, "Expected ';'");
    implementsDecl.done(CompactElementTypes.IMPLEMENTS_DECLARATION);
  }

  private void expectTypeName(@NotNull PsiBuilder builder) {
    if (at(builder, CompactTokenTypes.IDENTIFIER) || CompactTypePatternParser.isBuiltinType(builder.getTokenType())) {
      builder.advanceLexer();
      return;
    }
    builder.error("Expected type name");
  }

  private void parseTypeAlias(@NotNull PsiBuilder builder, boolean exported) {
    PsiBuilder.Marker typeAlias = builder.mark();
    if (exported) {
      expect(builder, CompactTokenTypes.EXPORT, "Expected 'export'");
    }
    if (at(builder, CompactTokenTypes.NEW)) {
      builder.advanceLexer();
    }
    expect(builder, CompactTokenTypes.TYPE, "Expected 'type'");
    expectTypeName(builder);
    if (at(builder, CompactTokenTypes.LT)) {
      context.typePattern.parseGenericParameterList(builder);
    }
    expect(builder, CompactTokenTypes.ASSIGN, "Expected '='");
    context.typePattern.parseType(builder);
    expect(builder, CompactTokenTypes.SEMICOLON, "Expected ';'");
    typeAlias.done(CompactElementTypes.TYPE_ALIAS_DECLARATION);
  }

  private void parseLedger(@NotNull PsiBuilder builder, boolean exported) {
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
    context.typePattern.parseType(builder);
    expect(builder, CompactTokenTypes.SEMICOLON, "Expected ';'");
    ledger.done(CompactElementTypes.LEDGER_DECLARATION);
  }

  private void parseWitness(@NotNull PsiBuilder builder, boolean exported) {
    PsiBuilder.Marker witness = builder.mark();
    if (exported) {
      expect(builder, CompactTokenTypes.EXPORT, "Expected 'export'");
    }
    expect(builder, CompactTokenTypes.WITNESS, "Expected 'witness'");
    expect(builder, CompactTokenTypes.IDENTIFIER, "Expected witness name");
    if (at(builder, CompactTokenTypes.LT)) {
      context.typePattern.parseGenericParameterList(builder);
    }
    context.typePattern.parseSimpleParameterList(builder);
    expect(builder, CompactTokenTypes.COLON, "Expected ':'");
    context.typePattern.parseType(builder);
    expect(builder, CompactTokenTypes.SEMICOLON, "Expected ';'");
    witness.done(CompactElementTypes.WITNESS_DECLARATION);
  }

  private void parseConstructor(@NotNull PsiBuilder builder) {
    PsiBuilder.Marker constructor = builder.mark();
    expect(builder, CompactTokenTypes.CONSTRUCTOR, "Expected 'constructor'");
    context.typePattern.parsePatternParameterList(builder);
    context.statement.parseBlock(builder);
    constructor.done(CompactElementTypes.CONSTRUCTOR_DEFINITION);
  }

  private void parseCircuit(@NotNull PsiBuilder builder, boolean exported) {
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
      context.typePattern.parseGenericParameterList(builder);
    }
    context.typePattern.parsePatternParameterList(builder);
    expect(builder, CompactTokenTypes.COLON, "Expected ':'");
    context.typePattern.parseType(builder);
    context.statement.parseBlock(builder);
    circuit.done(CompactElementTypes.CIRCUIT_DEFINITION);
  }
}
