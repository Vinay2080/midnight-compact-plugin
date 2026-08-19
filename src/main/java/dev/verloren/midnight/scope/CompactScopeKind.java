package dev.verloren.midnight.scope;

/**
 * Enumerates all distinct types of scope boundaries recognized in Compact code.
 */
public enum CompactScopeKind {
  FILE,
  MODULE,
  BLOCK,
  CALLABLE,
  CONSTRUCTOR,
  LAMBDA,
  FOR,
  TYPE_DECLARATION,
  STRUCT,
  ENUM,
  CONTRACT,
  UNKNOWN
}
