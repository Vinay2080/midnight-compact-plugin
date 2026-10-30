package dev.verloren.midnight.highlighter;

import com.intellij.lang.ASTNode;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import dev.verloren.midnight.psi.*;
import dev.verloren.midnight.resolve.CompactResolveUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Semantic highlighting annotator for the Midnight Compact smart contract language.
 *
 * <h3>Subsystem Role &amp; Extension Point</h3>
 * <p>Registered in {@code plugin.xml} via the {@code com.intellij.annotator} extension point
 * for {@link dev.verloren.midnight.CompactLanguage}. Serves as Tier 2 of the highlighting engine,
 * augmenting the basic lexical token colours from {@link CompactSyntaxHighlighter} with deep,
 * context-sensitive semantic colours derived from PSI tree inspection and reference resolution:
 * <ul>
 *   <li><b>Declarations:</b> Specific colours for circuits, witnesses, contracts, modules, structs,
 *       enums, enum members, struct fields, type aliases, generic type parameters, and ledger variables.</li>
 *   <li><b>Calls vs Usages:</b> Differentiates circuit calls from witness calls ({@code wit_...()}),
 *       builtin cryptographic functions ({@code disclose}, {@code persistentHash}), and struct field accesses.</li>
 *   <li><b>Read vs Write Usages:</b> Distinguishes read access from mutating assignment write access
 *       for both ledger state ({@link CompactHighlighterColors#LEDGER_WRITE}) and local variables
 *       ({@link CompactHighlighterColors#LOCAL_VARIABLE_WRITE}).</li>
 *   <li><b>Doc Comments &amp; Tags:</b> Parses embedded JSDoc/RustDoc style tags ({@code @param}, {@code @return},
 *       {@code @notice}) and tag values within documentation comments.</li>
 *   <li><b>String Escape Validation:</b> Synthesizes micro-ranges inside string literals to colorize valid
 *       escapes (such as {@code \n}, {@code \xHH}, {@code \u005Cu{...}}) and visually flag invalid escape sequences.</li>
 *   <li><b>Pragma Metadata:</b> Colorizes pragma directives and version requirement strings.</li>
 * </ul>
 * </p>
 *
 * <h3>Calling Pipeline &amp; Lifecycle</h3>
 * <p>Invoked by IntelliJ's daemon code analyser ({@code GeneralHighlightingPass}) on background threads
 * while the developer edits code. It traverses PSI elements in the active editor viewport, creating silent
 * informational annotations via {@link AnnotationHolder#newSilentAnnotation(HighlightSeverity)}.</p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Execution Thread:</b> Always runs on pooled background daemon threads managed by {@code DaemonCodeAnalyzer}.</li>
 *   <li><b>Read Action:</b> Requires a platform Read Action ({@code @RequiresReadLock}) while traversing PSI
 *       trees and resolving references via {@link CompactResolveUtil}.</li>
 *   <li><b>Non-Intrusive Highlighting:</b> Emits {@link HighlightSeverity#INFORMATION} annotations with text attributes,
 *       avoiding error gutters or wavy underlines.</li>
 * </ul>
 *
 * <h3>Semantic Highlighting Architecture</h3>
 * <pre>
 *   [GeneralHighlightingPass (Background Daemon)]
 *                          │
 *                          ▼
 *             [CompactHighlightingAnnotator]
 *                          │
 *   ┌──────────────────────┼──────────────────────┐
 *   ▼                      ▼                      ▼
 * [Lexical Enhancements]  [Declaration Pass]    [Reference &amp; Usage Pass]
 * - Doc Comments &amp; Tags - Circuits &amp; Witnesses - Calls: Circuit vs Witness
 * - String Escapes       - Contracts &amp; Modules  - Members: Field vs Enum
 * - Modifiers &amp; Pragmas  - Ledgers &amp; Constants  - Mutability: Read vs Write
 * </pre>
 *
 * @see dev.verloren.midnight.highlighter.CompactSyntaxHighlighter
 * @see dev.verloren.midnight.highlighter.CompactHighlighterColors
 * @see dev.verloren.midnight.resolve.CompactResolveUtil
 * @see com.intellij.lang.annotation.Annotator
 */
public class CompactHighlightingAnnotator implements Annotator {

  /** Set of standard built-in cryptographic and utility function identifiers provided by the Compact runtime. */
  private static final Set<String> BUILTIN_FUNCTIONS = Set.of(
      "assert", "disclose", "fold", "slice", "pad", "emit", "map",
      "transientHash", "persistentHash", "transientCommit", "persistentCommit",
      "transcribe", "publicKey", "degradeToTransient",
      "default", "some", "none", "left", "right",
      "merkleTreePathRoot", "merkleTreePathRootNoLeafHash", "nativeToken", "tokenType",
      "secp256r1EcdsaVerify", "ed25519Verify", "sha512"
  );

  /** Set of standard built-in primitive and container type identifiers recognized by the Compact type system. */
  private static final Set<String> BUILTIN_TYPES = Set.of(
      "Field", "Boolean", "Uint", "Bytes", "Vector", "Opaque", "Cell", "Void",
      "JubjubScalar", "Secp256k1Base", "Secp256k1Scalar", "Secp256r1", "Curve25519",
      "Counter", "Set", "Map", "List", "HistoricMerkleTree", "MerkleTree",
      "Kernel", "ContractAddress", "PublicAddress", "ShieldedCoinInfo", "QualifiedShieldedCoinInfo",
      "ZswapCoinPublicKey", "ShieldedSendResult",
      "Maybe", "Either", "MerkleTreeDigest", "MerkleTreePath", "MerkleTreePathEntry", "LeafPreimage"
  );

  /** Regex pattern matching doc comment tags (e.g. {@code @param}, {@code @return}). */
  private static final Pattern DOC_TAG_PATTERN = Pattern.compile("@([a-zA-Z_][a-zA-Z0-9_]*)");

  /**
   * Annotates the given PSI element with rich semantic text attributes based on its resolved role.
   * <p>
   * Invoked per PSI element in the visible editor viewport by {@code GeneralHighlightingPass}.
   * Inspects AST structure, resolves references, and creates silent annotations with {@link TextAttributesKey}s.
   * </p>
   *
   * @param element the PSI element to analyse
   * @param holder  annotation holder receiving newly constructed text attribute highlights
   */
  @Override
  public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
    // Fast-path exit: skip entire file root element to avoid unnecessary traversal overhead
    if (element instanceof PsiFile) {
      return;
    }

    // Fast-path exit: skip trivial leaf tokens (whitespace and standard punctuation) already styled lexically
    if (element.getFirstChild() == null && element.getNode() != null) {
      IElementType tt = element.getNode().getElementType();
      if (tt == com.intellij.psi.TokenType.WHITE_SPACE
          || tt == CompactTokenTypes.SEMICOLON
          || tt == CompactTokenTypes.LBRACE || tt == CompactTokenTypes.RBRACE
          || tt == CompactTokenTypes.LPAREN || tt == CompactTokenTypes.RPAREN
          || tt == CompactTokenTypes.LBRACKET || tt == CompactTokenTypes.RBRACKET
          || tt == CompactTokenTypes.COMMA || tt == CompactTokenTypes.COLON) {
        return;
      }
    }

    IElementType tokenType = element.getNode() != null ? element.getNode().getElementType() : null;

    // 1. Comments: Differentiate documentation comments (/// or /**) from standard comments
    //  are styled by Tier 1 lexer; doc comments are parsed here for JSDoc tags
    if (tokenType == CompactTokenTypes.LINE_COMMENT) {
      if (element.getText().startsWith("///")) {
        highlightDocComment(holder, element);
      }
      return;
    }
    if (tokenType == CompactTokenTypes.BLOCK_COMMENT) {
      if (element.getText().startsWith("/**")) {
        highlightDocComment(holder, element);
      }
      return;
    }

    // 2. String literal escapes: Inspect inner text ranges to highlight \n, \xHH, \u005Cu{...} or flag invalid escapes
    if (tokenType == CompactTokenTypes.STRING_LITERAL) {
      highlightStringEscapes(holder, element);
      return;
    }

    // 3. Modifiers (export, pure, sealed, new, implements, external):
    // Emphasize contract/circuit capability and visibility modifiers with distinct modifier text attributes
    if (tokenType == CompactTokenTypes.EXPORT
        || tokenType == CompactTokenTypes.PURE
        || tokenType == CompactTokenTypes.SEALED
        || tokenType == CompactTokenTypes.NEW
        || tokenType == CompactTokenTypes.IMPLEMENTS
        || tokenType == CompactTokenTypes.EXTERNAL) {
      highlight(holder, element, CompactHighlighterColors.MODIFIER);
      return;
    }

    // 4. Built-in function keywords (assert, disclose, fold, slice, pad, emit):
    // Style cryptographic and runtime intrinsics; for 'map', disambiguate type context (Map<K, V>) vs transform fn
    if (tokenType == CompactTokenTypes.ASSERT
        || tokenType == CompactTokenTypes.DISCLOSE
        || tokenType == CompactTokenTypes.FOLD
        || tokenType == CompactTokenTypes.SLICE
        || tokenType == CompactTokenTypes.PAD
        || tokenType == CompactTokenTypes.EMIT) {
      highlight(holder, element, CompactHighlighterColors.BUILTIN_FUNCTION);
      return;
    }
    if (tokenType == CompactTokenTypes.MAP) {
      if (element.getParent() instanceof CompactTypeReferenceImpl) {
        highlight(holder, element, CompactHighlighterColors.BUILTIN_TYPE);
      } else {
        highlight(holder, element, CompactHighlighterColors.BUILTIN_FUNCTION);
      }
      return;
    }

    // 5. Version literal in pragmas or dependencies (e.g. >= 0.23.0)
    if (tokenType == CompactTokenTypes.VERSION_LITERAL) {
      highlight(holder, element, CompactHighlighterColors.VERSION);
      return;
    }

    // 6. Built-in types: Style primitive AST nodes (Field, Boolean, Uint, Bytes, etc.)
    switch (element) {
      case CompactBuiltinTypeImpl builtinType -> {
        PsiElement firstChild = builtinType.getFirstChild();
        if (firstChild != null) {
          highlight(holder, firstChild, CompactHighlighterColors.BUILTIN_TYPE);
        }
        return;
      }

      // 7. Direct call expressions (e.g. compute(...), wit_secretKey(...)):
      // Resolves callee reference to distinguish private off-chain witnesses from public circuits
      case CompactCallExprImpl callExpr -> {
        // Step 7a: Extract the callee identifier leaf token from the call expression node
        ASTNode idNode = callExpr.getNode().findChildByType(CompactTokenTypes.IDENTIFIER);
        if (idNode != null) {
          String callName = idNode.getText();
          // Step 7b: Check if the function name matches a standard built-in cryptographic intrinsic
          if (BUILTIN_FUNCTIONS.contains(callName)) {
            highlight(holder, idNode.getPsi(), CompactHighlighterColors.BUILTIN_FUNCTION);
            return;
          }
          // Step 7c: Resolve reference target through PSI indexing and unwrap import alias wrappers
          PsiReference ref = callExpr.getReference();
          PsiElement target = ref != null ? ref.resolve() : null;
          if (target instanceof CompactImportElementImpl importElement) {
            target = CompactResolveUtil.resolveImportElementSource(importElement);
          }
          // Step 7d: Differentiate witness calls (private off-chain) from standard circuit calls
          if (target instanceof CompactWitnessDeclaration) {
            highlight(holder, idNode.getPsi(), CompactHighlighterColors.WITNESS_CALL);
          } else {
            highlight(holder, idNode.getPsi(), CompactHighlighterColors.CIRCUIT_CALL);
          }
        }
        return;
      }

      // 8. Struct literal expressions (e.g. Point { x: 1, y: 2 }):
      // Resolves the struct type header to style as user-defined struct, built-in type, or fallback
      case CompactStructLiteralExprImpl structLiteral -> {
        // Step 8a: Extract the struct type name identifier leaf token from the struct literal AST node
        ASTNode idNode = structLiteral.getNode().findChildByType(CompactTokenTypes.IDENTIFIER);
        if (idNode != null) {
          // Step 8b: Resolve PSI reference on the struct literal expression to locate the definition
          PsiReference ref = structLiteral.getReference();
          PsiElement target = ref != null ? ref.resolve() : null;
          // Step 8c: If the symbol was imported, unwrap the import alias to reach the origin declaration
          if (target instanceof CompactImportElementImpl importElement) {
            target = CompactResolveUtil.resolveImportElementSource(importElement);
          }
          // Step 8d: Discriminate between user struct definitions, built-in types, and unresolved fallback
          if (target instanceof CompactStructDefinition) {
            highlight(holder, idNode.getPsi(), CompactHighlighterColors.STRUCT_DECLARATION);
          } else if (BUILTIN_TYPES.contains(idNode.getText())) {
            highlight(holder, idNode.getPsi(), CompactHighlighterColors.BUILTIN_TYPE);
          } else {
            highlight(holder, idNode.getPsi(), CompactHighlighterColors.STRUCT_DECLARATION);
          }
        }
        return;
      }
      default -> {
      }
    }

    // 9. Struct arguments & destructuring pattern struct elements
    // 9a. Field assignments inside struct literal instantiations: Point { x: 1 } -> highlight 'x' as FIELD_DECLARATION
    if (element.getNode() != null && element.getNode().getElementType() == dev.verloren.midnight.parser.CompactElementTypes.STRUCT_ARG) {
      ASTNode idNode = element.getNode().findChildByType(CompactTokenTypes.IDENTIFIER);
      if (idNode != null) {
        highlight(holder, idNode.getPsi(), CompactHighlighterColors.FIELD_DECLARATION);
      }
      return;
    }
    // 9b. Bound field variables in destructuring: const Point { x, y } = ... -> highlight 'x' as LOCAL_VARIABLE_DECLARATION
    if (element.getNode() != null && element.getNode().getElementType() == dev.verloren.midnight.parser.CompactElementTypes.PATTERN_STRUCT_ELEMENT) {
      ASTNode idNode = element.getNode().findChildByType(CompactTokenTypes.IDENTIFIER);
      if (idNode != null) {
        highlight(holder, idNode.getPsi(), CompactHighlighterColors.LOCAL_VARIABLE_DECLARATION);
      }
      return;
    }

    // 10. Named declarations: Circuits, witnesses, contracts, modules, structs, ledgers, variables
    switch (element) {
      case CompactNamedElement namedElement -> {
        // Step 10a: Extract declaration name identifier leaf node
        PsiElement nameIdentifier = namedElement.getNameIdentifier();
        if (nameIdentifier != null) {
          // Step 10b: Query declaration classification table and emit semantic highlight
          TextAttributesKey declKey = getDeclarationKey(namedElement);
          if (declKey != null) {
            highlight(holder, nameIdentifier, declKey);
          }
        }
      }
      // 11. Type references (e.g. x: Point, state: GameState)
      case CompactTypeReferenceImpl typeRef -> annotateTypeReference(holder, typeRef);

      // 12. Member access expressions (e.g. p.x or GameState.PLAYING)
      case CompactMemberExprImpl memberExpr -> annotateMemberExpr(holder, memberExpr);

      // 13. Reference expressions & calls (e.g. foo, MAX, x)
      case CompactReferenceExprImpl refExpr -> annotateReferenceExpr(holder, refExpr);

      // 14. Pragma declarations
      case CompactPragmaFormImpl pragmaForm -> annotatePragma(holder, pragmaForm);
      default -> {
      }
    }
  }

  // =========================================================================
  // Declaration Highlighting
  // =========================================================================

  /**
   * Resolves the appropriate declaration {@link TextAttributesKey} for a named Compact element.
   * <p>
   * Inspects the concrete PSI type of the declaration. For patterns (bindings), checks enclosing
   * parents to distinguish parameter bindings from local variables vs top-level constants.
   * </p>
   *
   * @param declaration the named PSI element declaration
   * @return corresponding text attributes key, or null if no custom key is assigned
   */
  private static @Nullable TextAttributesKey getDeclarationKey(@NotNull CompactNamedElement declaration) {
    return switch (declaration) {
      case CompactCircuitDefinition _ -> CompactHighlighterColors.CIRCUIT_DECLARATION;
      case CompactWitnessDeclaration _ -> CompactHighlighterColors.WITNESS_DECLARATION;
      case CompactConstructorDeclaration _ -> CompactHighlighterColors.CONSTRUCTOR_DECLARATION;
      case CompactExternalContractDeclaration _ -> CompactHighlighterColors.CONTRACT_DECLARATION;
      case CompactModuleDefinition _ -> CompactHighlighterColors.MODULE_DECLARATION;
      case CompactStructDefinition _ -> CompactHighlighterColors.STRUCT_DECLARATION;
      case CompactEnumDefinition _ -> CompactHighlighterColors.ENUM_DECLARATION;
      case CompactEnumMemberImpl _ -> CompactHighlighterColors.ENUM_MEMBER_DECLARATION;
      case CompactStructFieldImpl _ -> CompactHighlighterColors.FIELD_DECLARATION;
      case CompactTypeDefinition _ -> CompactHighlighterColors.TYPE_ALIAS_DECLARATION;
      case CompactGenericParameterImpl _ -> CompactHighlighterColors.TYPE_PARAMETER;
      case CompactLedgerDeclaration _ -> CompactHighlighterColors.LEDGER_DECLARATION;
      case CompactPatternImpl pattern -> {
        // Step 1: If pattern is enclosed in parameter lists, it declares a function/circuit parameter
        if (PsiTreeUtil.getParentOfType(pattern, CompactParameterImpl.class) != null
            || PsiTreeUtil.getParentOfType(pattern, CompactTypedPatternImpl.class) != null
            || hasAncestorOfType(pattern, dev.verloren.midnight.parser.CompactElementTypes.PATTERN_PARAMETER_LIST)
            || hasAncestorOfType(pattern, dev.verloren.midnight.parser.CompactElementTypes.SIMPLE_PARAMETER_LIST)
            || hasAncestorOfType(pattern, dev.verloren.midnight.parser.CompactElementTypes.ARROW_PARAMETER_LIST)) {
          yield CompactHighlighterColors.PARAMETER_DECLARATION;
        }
        // Step 2: If pattern is enclosed in a const binding, check whether it is inside a block (local) or module (const)
        if (PsiTreeUtil.getParentOfType(pattern, CompactConstBindingImpl.class) != null) {
          yield PsiTreeUtil.getParentOfType(pattern, CompactBlock.class) != null
              ? CompactHighlighterColors.LOCAL_VARIABLE_DECLARATION
              : CompactHighlighterColors.CONSTANT_DECLARATION;
        }
        yield null;
      }
      case CompactParameterImpl _ -> CompactHighlighterColors.PARAMETER_DECLARATION;
      case CompactConstBindingImpl constBinding -> PsiTreeUtil.getParentOfType(constBinding, CompactBlock.class) != null
          ? CompactHighlighterColors.LOCAL_VARIABLE_DECLARATION
          : CompactHighlighterColors.CONSTANT_DECLARATION;
      case CompactImportElementImpl _ -> CompactHighlighterColors.IMPORTED_SYMBOL;
      default -> null;
    };
  }

  // =========================================================================
  // Type Reference Highlighting
  // =========================================================================

  /**
   * Annotates a type reference node with resolved type attributes (Struct, Enum, Alias, or Builtin).
   * <p>
   * Resolves the type reference target within the TYPE namespace and applies semantic attributes
   * matching the declaration kind (e.g. {@code struct}, {@code enum}, {@code type} alias).
   * </p>
   *
   * @param holder  annotation holder receiving highlights
   * @param typeRef PSI node representing the type reference
   */
  private static void annotateTypeReference(@NotNull AnnotationHolder holder, @NotNull CompactTypeReferenceImpl typeRef) {
    // Step 1: Extract identifier or 'map' token from the type reference AST
    PsiElement idElement = typeRef.findIdentifierChild();
    if (idElement == null) {
      ASTNode mapNode = typeRef.getNode().findChildByType(CompactTokenTypes.MAP);
      if (mapNode != null) {
        idElement = mapNode.getPsi();
      }
    }
    if (idElement == null) {
      return;
    }

    // Step 2: Check for built-in primitive/container types (e.g. Field, Uint, Bytes, Vector, Map)
    String name = idElement.getText();
    if (BUILTIN_TYPES.contains(name)) {
      highlight(holder, idElement, CompactHighlighterColors.BUILTIN_TYPE);
      return;
    }

    // Step 3: Resolve reference target through PSI indexing and unwrap import aliases
    PsiReference ref = typeRef.getReference();
    PsiElement target = ref != null ? ref.resolve() : null;
    if (target instanceof CompactImportElementImpl importElement) {
      target = CompactResolveUtil.resolveImportElementSource(importElement);
    }

    // Step 4: Dispatch highlight based on resolved definition type
    switch (target) {
      case CompactStructDefinition _ -> highlight(holder, idElement, CompactHighlighterColors.STRUCT_DECLARATION);
      case CompactEnumDefinition _ -> highlight(holder, idElement, CompactHighlighterColors.ENUM_DECLARATION);
      case CompactTypeDefinition _ -> highlight(holder, idElement, CompactHighlighterColors.TYPE_ALIAS_DECLARATION);
      case CompactGenericParameterImpl _ -> highlight(holder, idElement, CompactHighlighterColors.TYPE_PARAMETER);
      case null, default -> highlight(holder, idElement, CompactHighlighterColors.TYPE_REFERENCE);
    }
  }

  // =========================================================================
  // Member Expression Highlighting
  // =========================================================================

  /**
   * Annotates member access RHS tokens (e.g. {@code p.x} or {@code UpdateType.Grant}).
   * <p>
   * Scans AST siblings after the dot operator to locate the RHS identifier and resolves its
   * target to differentiate struct field accesses from enum member variant accesses.
   * </p>
   *
   * @param holder     annotation holder receiving highlights
   * @param memberExpr member access expression AST node
   */
  private static void annotateMemberExpr(@NotNull AnnotationHolder holder, @NotNull CompactMemberExprImpl memberExpr) {
    // Step 1: Locate member identifier token following the dot separator
    ASTNode dotNode = memberExpr.getNode().findChildByType(CompactTokenTypes.DOT);
    if (dotNode == null) {
      return;
    }

    ASTNode memberNode = null;
    ASTNode curr = dotNode.getTreeNext();
    while (curr != null) {
      if (curr.getElementType() == CompactTokenTypes.IDENTIFIER) {
        memberNode = curr;
        break;
      }
      curr = curr.getTreeNext();
    }
    if (memberNode == null) {
      return;
    }

    // Step 2: Resolve member reference to target declaration
    PsiElement memberPsi = memberNode.getPsi();
    PsiReference ref = memberExpr.getReference();
    PsiElement target = ref != null ? ref.resolve() : null;

    // Step 3: Differentiate enum member access from struct field access
    if (target instanceof CompactEnumMemberImpl) {
      highlight(holder, memberPsi, CompactHighlighterColors.ENUM_MEMBER_ACCESS);
    } else if (target instanceof CompactStructFieldImpl) {
      highlight(holder, memberPsi, CompactHighlighterColors.FIELD_ACCESS);
    } else {
      // Fallback based on reference provider type
      if (ref instanceof dev.verloren.midnight.reference.CompactEnumMemberReference) {
        highlight(holder, memberPsi, CompactHighlighterColors.ENUM_MEMBER_ACCESS);
      } else {
        highlight(holder, memberPsi, CompactHighlighterColors.FIELD_ACCESS);
      }
    }
  }

  // =========================================================================
  // Reference Expression & Call Highlighting
  // =========================================================================

  /**
   * Annotates identifier reference expressions based on resolved target and read/write mutability.
   * <p>
   * Resolves symbols across circuits, witnesses, enums, structs, ledgers, and variables.
   * Differentiates mutating assignment writes (LHS) from read usages for both ledger state and local variables.
   * </p>
   *
   * @param holder  annotation holder receiving highlights
   * @param refExpr reference expression PSI element
   */
  private static void annotateReferenceExpr(@NotNull AnnotationHolder holder, @NotNull CompactReferenceExprImpl refExpr) {
    // Step 1: Guard against double-highlighting if this is part of member access RHS (handled in annotateMemberExpr)
    PsiElement parent = refExpr.getParent();
    if (parent instanceof CompactMemberExprImpl memberParent && memberParent.getBaseExpression() != refExpr) {
      return;
    }

    String name = refExpr.getText();

    // Step 2: Check for builtin runtime function call (e.g. disclose, persistentHash)
    if (BUILTIN_FUNCTIONS.contains(name)) {
      highlight(holder, refExpr, CompactHighlighterColors.BUILTIN_FUNCTION);
      return;
    }

    // Step 3: Determine if reference is invoked as a call expression (unwrapping parentheses)
    boolean isCall = isCallExpression(refExpr);

    // Step 4: Resolve symbol target and unwrap import aliases to canonical definition
    PsiReference ref = refExpr.getReference();
    PsiElement target = ref != null ? ref.resolve() : null;

    if (target instanceof CompactImportElementImpl importElement) {
      CompactNamedElement unwrapped = CompactResolveUtil.resolveImportElementSource(importElement);
      if (unwrapped != null) {
        target = unwrapped;
      }
    }

    // Step 5: Dispatch semantic attributes based on resolved target kind
    switch (target) {
      case CompactCircuitDefinition _ -> highlight(holder, refExpr, CompactHighlighterColors.CIRCUIT_CALL);
      case CompactWitnessDeclaration _ -> highlight(holder, refExpr, CompactHighlighterColors.WITNESS_CALL);
      case CompactEnumMemberImpl _ -> highlight(holder, refExpr, CompactHighlighterColors.ENUM_MEMBER_ACCESS);
      case CompactEnumDefinition _ -> highlight(holder, refExpr, CompactHighlighterColors.ENUM_DECLARATION);
      case CompactStructDefinition _ -> highlight(holder, refExpr, CompactHighlighterColors.STRUCT_DECLARATION);
      case CompactTypeDefinition _ -> highlight(holder, refExpr, CompactHighlighterColors.TYPE_ALIAS_DECLARATION);
      // Step 6: Distinguish mutating assignment writes from read usages for on-chain ledger state
      case CompactLedgerDeclaration _ -> highlight(holder, refExpr, isWriteAccess(refExpr)
          ? CompactHighlighterColors.LEDGER_WRITE
          : CompactHighlighterColors.LEDGER_USAGE);
      case CompactParameterImpl _ -> highlight(holder, refExpr, CompactHighlighterColors.PARAMETER_USAGE);
      case CompactPatternImpl pattern when (
          PsiTreeUtil.getParentOfType(pattern, CompactParameterImpl.class) != null
          || PsiTreeUtil.getParentOfType(pattern, CompactTypedPatternImpl.class) != null
          || hasAncestorOfType(pattern, dev.verloren.midnight.parser.CompactElementTypes.PATTERN_PARAMETER_LIST)
          || hasAncestorOfType(pattern, dev.verloren.midnight.parser.CompactElementTypes.SIMPLE_PARAMETER_LIST)
          || hasAncestorOfType(pattern, dev.verloren.midnight.parser.CompactElementTypes.ARROW_PARAMETER_LIST)
      ) -> highlight(holder, refExpr, CompactHighlighterColors.PARAMETER_USAGE);
      // Step 7: Distinguish mutating writes from read usages for local block variables
      case CompactConstBindingImpl constBinding -> highlight(holder, refExpr,
          PsiTreeUtil.getParentOfType(constBinding, CompactBlock.class) != null
              ? (isWriteAccess(refExpr) ? CompactHighlighterColors.LOCAL_VARIABLE_WRITE : CompactHighlighterColors.LOCAL_VARIABLE_USAGE)
              : CompactHighlighterColors.CONSTANT_USAGE);
      case CompactPatternImpl pattern when PsiTreeUtil.getParentOfType(pattern, CompactConstBindingImpl.class) != null -> highlight(holder, refExpr,
          PsiTreeUtil.getParentOfType(pattern, CompactBlock.class) != null
              ? (isWriteAccess(refExpr) ? CompactHighlighterColors.LOCAL_VARIABLE_WRITE : CompactHighlighterColors.LOCAL_VARIABLE_USAGE)
              : CompactHighlighterColors.CONSTANT_USAGE);
      case CompactGenericParameterImpl _ -> highlight(holder, refExpr, CompactHighlighterColors.TYPE_PARAMETER);
      case null, default -> {
        // Step 8: Fallback for unresolved function calls or uppercase constants
        if (isCall) {
          highlight(holder, refExpr, CompactHighlighterColors.CIRCUIT_CALL);
        } else if (isConstantIdentifier(name)) {
          highlight(holder, refExpr, CompactHighlighterColors.CONSTANT_USAGE);
        }
      }
    }
  }

  /**
   * Determines if a reference expression appears on the LHS of an assignment expression.
   * <p>
   * Inspects the enclosing parent node: if it is an {@code ASSIGN_EXPR} and this reference
   * is or belongs to the first child (LHS), it represents a mutating reassignment writing.
   * </p>
   *
   * @param refExpr the reference expression to evaluate
   * @return true if the expression represents mutating write access
   */
  private static boolean isWriteAccess(@NotNull CompactReferenceExprImpl refExpr) {
    PsiElement parent = refExpr.getParent();
    if (parent != null && parent.getNode() != null && parent.getNode().getElementType() == dev.verloren.midnight.parser.CompactElementTypes.ASSIGN_EXPR) {
      PsiElement first = parent.getFirstChild();
      return first == refExpr || (PsiTreeUtil.isAncestor(first, refExpr, false));
    }
    return false;
  }

  /**
   * Helper verifying whether a PSI element has an ancestor of the specified AST element type.
   */
  private static boolean hasAncestorOfType(@NotNull PsiElement element, @NotNull IElementType type) {
    return CompactPsiUtil.hasAncestorOfType(element, type);
  }

  /**
   * Determines whether the reference expression is the callee of a call expression (unwrapping parens).
   */
  private static boolean isCallExpression(@NotNull CompactReferenceExprImpl refExpr) {
    PsiElement parent = refExpr.getParent();
    while (parent instanceof CompactParenExprImpl) {
      parent = parent.getParent();
    }
    return parent instanceof CompactCallExprImpl;
  }

  // =========================================================================
  // Pragma Directive Highlighting
  // =========================================================================

  /**
   * Highlights the pragma directive identifier (e.g. {@code language_version}).
   */
  private static void annotatePragma(@NotNull AnnotationHolder holder, @NotNull CompactPragmaFormImpl pragmaForm) {
    PsiElement pragmaId = pragmaForm.getPragmaIdentifier();
    if (pragmaId != null) {
      highlight(holder, pragmaId, CompactHighlighterColors.PRAGMA);
    }
  }

  // =========================================================================
  // String Escape Highlighting
  // =========================================================================

  /**
   * Inspects string literal content, highlighting valid escapes and flagging malformed sequences.
   * <p>
   * Scans string characters starting after the opening quote up to the closing quote.
   * Uses micro-range annotations via {@link #highlightRange(AnnotationHolder, TextRange, TextAttributesKey)}
   * to colour valid escapes or highlight syntax errors for invalid escapes.
   * </p>
   *
   * @param holder  annotation holder receiving highlights
   * @param element string literal PSI element
   */
  private static void highlightStringEscapes(@NotNull AnnotationHolder holder, @NotNull PsiElement element) {
    // Step 1: Verify string literal length boundaries (must be at least 2 for enclosing quotes)
    String text = element.getText();
    int startOffset = element.getTextRange().getStartOffset();
    int len = text.length();
    if (len < 2) {
      return;
    }

    // Step 2: Scan for escape sequences beginning with backslash '\'
    int i = 1;
    while (i < len - 1) {
      if (text.charAt(i) == '\\') {
        if (i + 1 >= len - 1) {
          // Trailing backslash before end quote -> flag as invalid escape
          highlightRange(holder, TextRange.create(startOffset + i, startOffset + i + 1), CompactHighlighterColors.INVALID_STRING_ESCAPE);
          break;
        }

        char next = text.charAt(i + 1);
        // Step 3: Match single-character escapes (\n, \r, \t, \\, \", \', \0)
        if (next == 'n' || next == 'r' || next == 't' || next == '\\' || next == '"' || next == '\'' || next == '0') {
          highlightRange(holder, TextRange.create(startOffset + i, startOffset + i + 2), CompactHighlighterColors.VALID_STRING_ESCAPE);
          i += 2;
        // Step 4: Match 2-digit hex byte escapes (\xHH)
        } else if (next == 'x') {
          if (i + 3 < len - 1 && isHexDigit(text.charAt(i + 2)) && isHexDigit(text.charAt(i + 3))) {
            highlightRange(holder, TextRange.create(startOffset + i, startOffset + i + 4), CompactHighlighterColors.VALID_STRING_ESCAPE);
            i += 4;
          } else {
            highlightRange(holder, TextRange.create(startOffset + i, Math.min(startOffset + i + 2, startOffset + len - 1)), CompactHighlighterColors.INVALID_STRING_ESCAPE);
            i += 2;
          }
        // Step 5: Match variable-length Unicode escapes (\u005Cu{...}) validating 1-6 hex digits
        } else if (next == 'u' && i + 2 < len - 1 && text.charAt(i + 2) == '{') {
          int closeBrace = text.indexOf('}', i + 3);
          if (closeBrace != -1 && closeBrace < len - 1 && isValidHexSequence(text, i + 3, closeBrace)) {
            highlightRange(holder, TextRange.create(startOffset + i, startOffset + closeBrace + 1), CompactHighlighterColors.VALID_STRING_ESCAPE);
            i = closeBrace + 1;
          } else {
            int invalidEnd = closeBrace != -1 && closeBrace < len - 1 ? closeBrace + 1 : Math.min(i + 3, len - 1);
            highlightRange(holder, TextRange.create(startOffset + i, startOffset + invalidEnd), CompactHighlighterColors.INVALID_STRING_ESCAPE);
            i = invalidEnd;
          }
        // Step 6: Flag unrecognized escape sequences
        } else {
          highlightRange(holder, TextRange.create(startOffset + i, startOffset + i + 2), CompactHighlighterColors.INVALID_STRING_ESCAPE);
          i += 2;
        }
      } else {
        i++;
      }
    }
  }

  /**
   * Checks whether a character is a valid hexadecimal digit (0-9, a-f, A-F).
   */
  private static boolean isHexDigit(char c) {
    return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
  }

  /**
   * Validates whether a character sequence between bounds is composed of 1 to 6 hexadecimal digits.
   */
  private static boolean isValidHexSequence(String text, int start, int end) {
    if (start >= end || end - start > 6) {
      return false;
    }
    for (int i = start; i < end; i++) {
      if (!isHexDigit(text.charAt(i))) {
        return false;
      }
    }
    return true;
  }

  /**
   * Emits a silent informational annotation applying text attributes across the entire PSI element range.
   * <p>
   * Uses {@code newSilentAnnotation(HighlightSeverity.INFORMATION)} so the text styling is applied
   * without creating error markers, warnings, or wavy underlines in the editor.
   * </p>
   *
   * @param holder  the annotation holder
   * @param element the PSI element receiving text styling
   * @param key     the text attributes key to apply
   */
  private static void highlight(@NotNull AnnotationHolder holder, @NotNull PsiElement element, @NotNull TextAttributesKey key) {
    holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
        .range(element.getTextRange())
        .textAttributes(key)
        .create();
  }

  /**
   * Emits a silent informational annotation applying text attributes across a specific character range.
   *
   * @param holder the annotation holder
   * @param range  the document text range receiving text styling
   * @param key    the text attributes key to apply
   */
  private static void highlightRange(@NotNull AnnotationHolder holder, @NotNull TextRange range, @NotNull TextAttributesKey key) {
    holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
        .range(range)
        .textAttributes(key)
        .create();
  }

  /**
   * Highlights doc comment content, isolating and styling JSDoc/RustDoc tags and tag values.
   *
   * @param holder  the annotation holder
   * @param element doc comment token PSI element
   */
  private static void highlightDocComment(@NotNull AnnotationHolder holder, @NotNull PsiElement element) {
    // Step 1: Apply baseline doc comment text attributes across the entire comment
    highlight(holder, element, CompactHighlighterColors.DOC_COMMENT);

    String text = element.getText();
    int startOffset = element.getTextRange().getStartOffset();
    Matcher matcher = DOC_TAG_PATTERN.matcher(text);

    // Step 2: Scan for JSDoc/RustDoc tags using regex pattern
    while (matcher.find()) {
      int tagStart = matcher.start();
      int tagEnd = matcher.end();
      // Step 3: Highlight the tag itself (@param, @return, etc.)
      highlightRange(holder, TextRange.create(startOffset + tagStart, startOffset + tagEnd), CompactHighlighterColors.DOC_COMMENT_TAG);

      // Step 4: Highlight associated parameter, type, or module names following the tag
      String tagName = matcher.group(1);
      if ("param".equals(tagName) || "type".equals(tagName) || "module".equals(tagName)) {
        int idx = tagEnd;
        while (idx < text.length() && Character.isWhitespace(text.charAt(idx)) && text.charAt(idx) != '\n' && text.charAt(idx) != '\r') {
          idx++;
        }
        if (idx < text.length() && (Character.isLetter(text.charAt(idx)) || text.charAt(idx) == '_' || text.charAt(idx) == '$')) {
          int valueStart = idx;
          while (idx < text.length() && (Character.isLetterOrDigit(text.charAt(idx)) || text.charAt(idx) == '_' || text.charAt(idx) == '$')) {
            idx++;
          }
          int valueEnd = idx;
          highlightRange(holder, TextRange.create(startOffset + valueStart, startOffset + valueEnd), CompactHighlighterColors.DOC_COMMENT_TAG_VALUE);
        }
      }
    }
  }

  /**
   * Checks whether an identifier conforms to the uppercase constant naming convention (e.g. {@code MAX_SUPPLY}).
   */
  private static boolean isConstantIdentifier(@NotNull String name) {
    if (name.length() < 2) {
      return false;
    }
    if (!Character.isUpperCase(name.charAt(0))) {
      return false;
    }
    for (int i = 0; i < name.length(); i++) {
      char c = name.charAt(i);
      if (!Character.isUpperCase(c) && !Character.isDigit(c) && c != '_') {
        return false;
      }
    }
    return !BUILTIN_TYPES.contains(name);
  }
}
