package dev.verloren.midnight.parser;

import com.intellij.lang.PsiBuilder;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import org.jetbrains.annotations.NotNull;

import static dev.verloren.midnight.parser.CompactParserUtil.at;
import static dev.verloren.midnight.parser.CompactParserUtil.expect;

/**
 * Parser delegate responsible for module linkage directives: includes, imports, and exports.
 */
final class CompactImportExportParser {
  private final CompactParserContext context;

  CompactImportExportParser(@NotNull CompactParserContext context) {
    this.context = context;
  }

  void parseInclude(@NotNull PsiBuilder builder) {
    PsiBuilder.Marker include = builder.mark();
    expect(builder, CompactTokenTypes.INCLUDE, "Expected 'include'");
    expect(builder, CompactTokenTypes.STRING_LITERAL, "Expected include file string");
    expect(builder, CompactTokenTypes.SEMICOLON, "Expected ';'");
    include.done(CompactElementTypes.INCLUDE_FORM);
  }

  void parseImport(@NotNull PsiBuilder builder) {
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
      context.typePattern.parseGenericArgumentList(builder);
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

  private void parseImportSelection(@NotNull PsiBuilder builder) {
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

  private void parseImportElement(@NotNull PsiBuilder builder) {
    PsiBuilder.Marker element = builder.mark();
    expect(builder, CompactTokenTypes.IDENTIFIER, "Expected imported name");
    if (at(builder, CompactTokenTypes.AS)) {
      builder.advanceLexer();
      expect(builder, CompactTokenTypes.IDENTIFIER, "Expected import alias");
    }
    element.done(CompactElementTypes.IMPORT_ELEMENT);
  }

  void parseExportForm(@NotNull PsiBuilder builder) {
    PsiBuilder.Marker export = builder.mark();
    expect(builder, CompactTokenTypes.EXPORT, "Expected 'export'");
    expect(builder, CompactTokenTypes.LBRACE, "Expected '{'");
    parseExportIdentifierList(builder);
    expect(builder, CompactTokenTypes.RBRACE, "Expected '}'");
    if (at(builder, CompactTokenTypes.SEMICOLON)) {
      builder.advanceLexer();
    }
    export.done(CompactElementTypes.EXPORT_FORM);
  }

  private void parseExportIdentifierList(@NotNull PsiBuilder builder) {
    while (!builder.eof() && !at(builder, CompactTokenTypes.RBRACE)) {
      expect(builder, CompactTokenTypes.IDENTIFIER, "Expected identifier");
      if (at(builder, CompactTokenTypes.COMMA)) {
        builder.advanceLexer();
      } else if (!at(builder, CompactTokenTypes.RBRACE)) {
        builder.error("Expected ',' or '}'");
        builder.advanceLexer();
      }
    }
  }
}
