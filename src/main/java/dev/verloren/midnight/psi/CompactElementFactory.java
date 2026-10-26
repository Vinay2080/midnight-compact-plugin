package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.CompactFileType;
import dev.verloren.midnight.parser.CompactElementTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Factory for creating typed Compact PSI elements from AST nodes and generating synthetic PSI fragments.
 *
 * <h3>IntelliJ Platform Lifecycle &amp; Subsystem Role</h3>
 * <p>Serves as the central instantiation hub bridging low-level AST nodes (produced by Grammar-Kit
 * and the JFlex lexer) to high-level, typed {@link CompactPsiElement} instances:
 * <ul>
 *   <li><b>PSI Construction:</b> Called directly by {@link dev.verloren.midnight.parser.CompactParserDefinition#createElement(ASTNode)}
 *       to map each AST composite node to its dedicated domain model class (e.g., {@link CompactCircuitDefinitionImpl},
 *       {@link CompactBinaryExprImpl}).</li>
 *   <li><b>Synthetic Element Synthesis:</b> Generates synthetic PSI fragments (such as fresh identifier leaves
 *       via {@link #createIdentifierLeaf(Project, String)} or pragma headers via {@link #createPragmaForm(Project, String)})
 *       by parsing short ephemeral code snippets through {@link PsiFileFactory}.</li>
 * </ul>
 * </p>
 *
 * <h3>Calling Pipeline</h3>
 * <p>Invoked across core IDE pipelines:
 * <ul>
 *   <li>{@link dev.verloren.midnight.parser.CompactParserDefinition#createElement(ASTNode)} during AST-to-PSI conversion.</li>
 *   <li>{@link CompactNamedElementImpl#setName(String)} during symbol rename refactorings.</li>
 *   <li>IntelliJ quick-fixes and intention actions inserting synthetic pragmas or elements.</li>
 * </ul>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Execution Thread:</b> {@link #createElement(ASTNode)} is called on background parser threads or daemon threads.
 *       Synthetic creation methods ({@link #createIdentifierLeaf}) require a platform Read Action ({@code @RequiresReadLock}).</li>
 *   <li><b>Write Action:</b> Creating synthetic files via {@link PsiFileFactory} requires a Read Action; attaching synthesized
 *       elements to the active document AST must be executed inside a {@link com.intellij.openapi.command.WriteCommandAction} on the EDT.</li>
 * </ul>
 *
 * <h3>Design Decisions &amp; Memory Safety</h3>
 * <ul>
 *   <li><b>Stateless Utility:</b> Uses a private constructor and static pure functions. No mutable state or caching is retained,
 *       ensuring zero thread-synchronization bottlenecks and thread safety across concurrent background parses.</li>
 *   <li><b>Memory Leak Prevention:</b> {@link Project} and {@link PsiElement} instances passed into factory methods
 *       are strictly scoped to the call stack and never cached in static fields.</li>
 *   <li><b>Safe AST Fallback:</b> Any unrecognized {@link IElementType} defaults to instantiating a generic
 *       {@link CompactPsiElement}, guaranteeing that syntax extensions or unmapped grammar nodes never crash the parser.</li>
 * </ul>
 *
 * @see dev.verloren.midnight.parser.CompactParserDefinition
 * @see CompactPsiElement
 * @see CompactNamedElementImpl#setName
 */
public final class CompactElementFactory {

  /**
   * Private constructor preventing direct instantiation of this stateless utility factory.
   */
  private CompactElementFactory() {
  }

  /**
   * Maps an {@link ASTNode} to its dedicated {@link CompactPsiElement} subclass based on its {@link IElementType}.
   *
   * <p>If no specialized wrapper exists for the given node type, falls back gracefully to
   * instantiating a baseline {@link CompactPsiElement}.</p>
   *
   * @param node the AST node to wrap
   * @return the corresponding typed PSI element wrapper; never {@code null}
   */
  public static @NotNull PsiElement createElement(@NotNull ASTNode node) {
    IElementType elementType = node.getElementType();
    if (elementType == CompactElementTypes.PRAGMA_FORM) {
      return new CompactPragmaFormImpl(node);
    }
    if (elementType == CompactElementTypes.INCLUDE_FORM) {
      return new CompactIncludeDeclarationImpl(node);
    }
    if (elementType == CompactElementTypes.IMPORT_FORM) {
      return new CompactImportDeclarationImpl(node);
    }
    if (elementType == CompactElementTypes.EXPORT_FORM) {
      return new CompactExportDeclarationImpl(node);
    }
    if (elementType == CompactElementTypes.MODULE_DEFINITION) {
      return new CompactModuleDefinitionImpl(node);
    }
    if (elementType == CompactElementTypes.STRUCT_DECLARATION) {
      return new CompactStructDefinitionImpl(node);
    }
    if (elementType == CompactElementTypes.ENUM_DECLARATION) {
      return new CompactEnumDefinitionImpl(node);
    }
    if (elementType == CompactElementTypes.CONTRACT_DECLARATION) {
      return new CompactExternalContractDeclarationImpl(node);
    }
    if (elementType == CompactElementTypes.IMPLEMENTS_DECLARATION) {
      return new CompactContractImplementsDeclarationImpl(node);
    }
    if (elementType == CompactElementTypes.EXTERNAL_CIRCUIT) {
      return new CompactExternalCircuitImpl(node);
    }
    if (elementType == CompactElementTypes.TYPE_ALIAS_DECLARATION) {
      return new CompactTypeDefinitionImpl(node);
    }
    if (elementType == CompactElementTypes.LEDGER_DECLARATION) {
      return new CompactLedgerDeclarationImpl(node);
    }
    if (elementType == CompactElementTypes.WITNESS_DECLARATION) {
      return new CompactWitnessDeclarationImpl(node);
    }
    if (elementType == CompactElementTypes.CONSTRUCTOR_DEFINITION) {
      return new CompactConstructorDeclarationImpl(node);
    }
    if (elementType == CompactElementTypes.CIRCUIT_DEFINITION) {
      return new CompactCircuitDefinitionImpl(node);
    }
    if (elementType == CompactElementTypes.BLOCK) {
      return new CompactBlock(node);
    }
    if (elementType == CompactElementTypes.BINARY_EXPR) {
      return new CompactBinaryExprImpl(node);
    }
    if (elementType == CompactElementTypes.UNARY_EXPR) {
      return new CompactUnaryExprImpl(node);
    }
    if (elementType == CompactElementTypes.PAREN_EXPR) {
      return new CompactParenExprImpl(node);
    }
    if (elementType == CompactElementTypes.CAST_EXPR) {
      return new CompactCastExprImpl(node);
    }
    if (elementType == CompactElementTypes.TERNARY_EXPR) {
      return new CompactTernaryExprImpl(node);
    }
    if (elementType == CompactElementTypes.REFERENCE_EXPR) {
      return new CompactReferenceExprImpl(node);
    }
    if (elementType == CompactElementTypes.LITERAL_EXPR) {
      return new CompactLiteralExprImpl(node);
    }
    if (elementType == CompactElementTypes.CALL_EXPR) {
      return new CompactCallExprImpl(node);
    }
    if (elementType == CompactElementTypes.TYPE_REFERENCE) {
      return new CompactTypeReferenceImpl(node);
    }
    if (elementType == CompactElementTypes.BUILTIN_TYPE) {
      return new CompactBuiltinTypeImpl(node);
    }
    if (elementType == CompactElementTypes.STRUCT_LITERAL_EXPR) {
      return new CompactStructLiteralExprImpl(node);
    }
    if (elementType == CompactElementTypes.MEMBER_EXPR) {
      return new CompactMemberExprImpl(node);
    }
    if (elementType == CompactElementTypes.IMPORT_ELEMENT) {
      return new CompactImportElementImpl(node);
    }
    if (elementType == CompactElementTypes.TYPED_ID) {
      return new CompactParameterImpl(node);
    }
    if (elementType == CompactElementTypes.TYPED_PATTERN) {
      return new CompactTypedPatternImpl(node);
    }
    if (elementType == CompactElementTypes.STRUCT_FIELD) {
      return new CompactStructFieldImpl(node);
    }
    if (elementType == CompactElementTypes.ENUM_MEMBER) {
      return new CompactEnumMemberImpl(node);
    }
    if (elementType == CompactElementTypes.CONST_BINDING) {
      return new CompactConstBindingImpl(node);
    }
    if (elementType == CompactElementTypes.GENERIC_PARAMETER) {
      return new CompactGenericParameterImpl(node);
    }
    if (elementType == CompactElementTypes.PATTERN) {
      return new CompactPatternImpl(node);
    }
    if (elementType == CompactElementTypes.PAD_EXPR) {
      return new CompactPadExprImpl(node);
    }
    if (elementType == CompactElementTypes.TUPLE_EXPR) {
      return new CompactTupleExprImpl(node);
    }
    if (elementType == CompactElementTypes.INDEX_EXPR) {
      return new CompactIndexExprImpl(node);
    }
    if (elementType == CompactElementTypes.DEFAULT_EXPR) {
      return new CompactDefaultExprImpl(node);
    }

    return new CompactPsiElement(node);
  }

  /**
   * Synthesizes a new AST identifier leaf element containing the specified identifier name.
   *
   * <p>Constructs an in-memory dummy Compact file containing {@code "type <name> = Field;"},
   * navigates to the synthesized type definition's identifier node, and returns it for replacement.</p>
   *
   * @param project the current project context
   * @param text the new identifier text
   * @return the newly created identifier leaf {@link PsiElement}
   * @throws IllegalArgumentException if the provided text cannot form a valid Compact identifier
   */
  public static @NotNull PsiElement createIdentifierLeaf(@NotNull Project project, @NotNull String text) {
    // Step 1: Parse a synthetic dummy type alias snippet containing the target identifier
    CompactFile file = (CompactFile) PsiFileFactory.getInstance(project)
            .createFileFromText("rename.compact", CompactFileType.INSTANCE, "type " + text + " = Field;");

    // Step 2: Locate the type definition and extract its identifier leaf node
    CompactTypeDefinition typeDefinition = PsiTreeUtil.findChildOfType(file, CompactTypeDefinition.class);
    if (typeDefinition == null || typeDefinition.getNameIdentifier() == null) {
      throw new IllegalArgumentException("Invalid Compact identifier: " + text);
    }
    return typeDefinition.getNameIdentifier();
  }

  /**
   * Synthesizes a new {@link CompactPragmaForm} element from raw pragma text.
   *
   * <p>Parses an ephemeral snippet (e.g. {@code "pragma language_version >= 0.20;"}) and extracts
   * the root pragma PSI element. Used by quick-fixes and file templates.</p>
   *
   * @param project the current project context
   * @param text the complete pragma statement text
   * @return the synthesized {@link CompactPragmaForm}, or {@code null} if parsing failed
   */
  public static @Nullable CompactPragmaForm createPragmaForm(@NotNull Project project, @NotNull String text) {
    CompactFile file = (CompactFile) PsiFileFactory.getInstance(project)
        .createFileFromText("pragma.compact", CompactFileType.INSTANCE, text);
    return PsiTreeUtil.findChildOfType(file, CompactPragmaForm.class);
  }

  /**
   * Checks whether the given {@link IElementType} is mapped to a dedicated typed {@link CompactPsiElement} subclass.
   *
   * @param elementType the element type to check
   * @return {@code true} if a specialized PSI implementation exists for this element type; {@code false} otherwise
   */
  public static boolean hasDedicatedElement(@NotNull IElementType elementType) {
    return elementType == CompactElementTypes.PRAGMA_FORM
            || elementType == CompactElementTypes.INCLUDE_FORM
            || elementType == CompactElementTypes.IMPORT_FORM
            || elementType == CompactElementTypes.EXPORT_FORM
            || elementType == CompactElementTypes.MODULE_DEFINITION
            || elementType == CompactElementTypes.STRUCT_DECLARATION
            || elementType == CompactElementTypes.ENUM_DECLARATION
            || elementType == CompactElementTypes.CONTRACT_DECLARATION
            || elementType == CompactElementTypes.IMPLEMENTS_DECLARATION
            || elementType == CompactElementTypes.EXTERNAL_CIRCUIT
            || elementType == CompactElementTypes.TYPE_ALIAS_DECLARATION
            || elementType == CompactElementTypes.LEDGER_DECLARATION
            || elementType == CompactElementTypes.WITNESS_DECLARATION
            || elementType == CompactElementTypes.CONSTRUCTOR_DEFINITION
            || elementType == CompactElementTypes.CIRCUIT_DEFINITION
            || elementType == CompactElementTypes.BLOCK
            || elementType == CompactElementTypes.BINARY_EXPR
            || elementType == CompactElementTypes.UNARY_EXPR
            || elementType == CompactElementTypes.PAREN_EXPR
            || elementType == CompactElementTypes.CAST_EXPR
            || elementType == CompactElementTypes.TERNARY_EXPR
            || elementType == CompactElementTypes.REFERENCE_EXPR
            || elementType == CompactElementTypes.LITERAL_EXPR
            || elementType == CompactElementTypes.CALL_EXPR
            || elementType == CompactElementTypes.TYPE_REFERENCE
            || elementType == CompactElementTypes.BUILTIN_TYPE
            || elementType == CompactElementTypes.STRUCT_LITERAL_EXPR
            || elementType == CompactElementTypes.MEMBER_EXPR
            || elementType == CompactElementTypes.IMPORT_ELEMENT
            || elementType == CompactElementTypes.TYPED_ID
            || elementType == CompactElementTypes.TYPED_PATTERN
            || elementType == CompactElementTypes.STRUCT_FIELD
            || elementType == CompactElementTypes.ENUM_MEMBER
            || elementType == CompactElementTypes.CONST_BINDING
            || elementType == CompactElementTypes.GENERIC_PARAMETER
            || elementType == CompactElementTypes.PATTERN
            || elementType == CompactElementTypes.PAD_EXPR
            || elementType == CompactElementTypes.TUPLE_EXPR
            || elementType == CompactElementTypes.INDEX_EXPR
            || elementType == CompactElementTypes.DEFAULT_EXPR;
  }
}
