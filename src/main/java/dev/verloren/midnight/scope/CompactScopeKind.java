package dev.verloren.midnight.scope;

/**
 * Enumeration of all distinct lexical, structural, and semantic scope boundaries recognized in Compact code.
 *
 * <h3>IntelliJ Platform Subsystem Role & Calling Pipeline</h3>
 * <p>
 * Used across the plugin's semantic analysis, resolution, and inspection systems:
 * <ul>
 *   <li><b>Scope Discovery:</b> Assigned by {@link CompactScopes} when walking up the PSI tree
 *       from any reference or completion site to determine the enclosing scope boundary.</li>
 *   <li><b>Resolution & Shadowing:</b> Informs {@link CompactPsiScope} and {@link dev.verloren.midnight.resolve.CompactResolveUtil}
 *       how symbols are bound, whether forward references are permitted, and how lexical shadowing rules apply.</li>
 * </ul>
 * </p>
 *
 * <h3>Scope Boundary Categories</h3>
 * <ul>
 *   <li><b>File & Module:</b> Top-level containers ({@link #FILE}, {@link #MODULE}) where declarations are
 *       order-independent and allow forward references.</li>
 *   <li><b>Callables & Blocks:</b> Statement-level containers ({@link #CALLABLE}, {@link #CONSTRUCTOR},
 *       {@link #BLOCK}, {@link #LAMBDA}, {@link #FOR}) where local variables and bindings enforce strict
 *       forward declaration rules (declaration must precede usage).</li>
 *   <li><b>Nominal Types:</b> Declaration containers ({@link #STRUCT}, {@link #ENUM}, {@link #CONTRACT},
 *       {@link #TYPE_DECLARATION}) establishing isolated field, variant, and generic parameter scopes.</li>
 * </ul>
 *
 * @see dev.verloren.midnight.scope.CompactScope
 * @see dev.verloren.midnight.scope.CompactScopes
 * @see dev.verloren.midnight.scope.CompactPsiScope
 */
public enum CompactScopeKind {
  /**
   * Top-level file scope container ({@link dev.verloren.midnight.psi.CompactFile}).
   */
  FILE,

  /**
   * Module declaration scope ({@link dev.verloren.midnight.psi.CompactModuleDefinition}).
   */
  MODULE,

  /**
   * General curly-brace statement block ({@link dev.verloren.midnight.psi.CompactBlock}).
   */
  BLOCK,

  /**
   * Circuit or witness declaration enclosing parameters, body, and return signature.
   */
  CALLABLE,

  /**
   * Contract constructor declaration scope.
   */
  CONSTRUCTOR,

  /**
   * Anonymous lambda expression closure scope.
   */
  LAMBDA,

  /**
   * For-statement loop scope enclosing the iteration variable and loop body.
   */
  FOR,

  /**
   * Type alias declaration scope enclosing generic type parameters.
   */
  TYPE_DECLARATION,

  /**
   * Struct declaration scope enclosing fields and generic type parameters.
   */
  STRUCT,

  /**
   * Enum declaration scope enclosing variant members.
   */
  ENUM,

  /**
   * External contract declaration scope enclosing exported interface methods.
   */
  CONTRACT,

  /**
   * Fallback for elements that do not introduce a recognized scope boundary.
   */
  UNKNOWN
}
