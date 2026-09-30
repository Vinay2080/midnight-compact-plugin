package dev.verloren.midnight.parser;

/**
 * Coordination context that wires together modular parser delegates.
 */
final class CompactParserContext {
  final CompactTypePatternParser typePattern;
  final CompactExpressionParser expression;
  final CompactStatementParser statement;
  final CompactPragmaParser pragma;
  final CompactImportExportParser importExport;
  final CompactDeclarationParser declaration;

  CompactParserContext() {
    this.typePattern = new CompactTypePatternParser(this);
    this.expression = new CompactExpressionParser(this);
    this.statement = new CompactStatementParser(this);
    this.pragma = new CompactPragmaParser();
    this.importExport = new CompactImportExportParser(this);
    this.declaration = new CompactDeclarationParser(this);
  }
}
