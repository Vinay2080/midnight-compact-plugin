package dev.verloren.midnight.resolve.npm;

/**
 * Categorizes symbols exported by external npm packages (functions, constants, classes, types, etc.).
 */
public enum CompactNpmSymbolKind {
  FUNCTION(true, false),
  CONST(true, false),
  LET(true, false),
  VAR(true, false),
  CLASS(true, true),
  INTERFACE(false, true),
  TYPE_ALIAS(false, true),
  ENUM(true, true),
  NAMESPACE(true, true),
  UNKNOWN(true, true);

  private final boolean value;
  private final boolean type;

  CompactNpmSymbolKind(boolean value, boolean type) {
    this.value = value;
    this.type = type;
  }

  public boolean isValue() {
    return value;
  }

  public boolean isType() {
    return type;
  }
}
