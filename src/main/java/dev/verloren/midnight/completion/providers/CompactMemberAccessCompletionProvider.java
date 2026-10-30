package dev.verloren.midnight.completion.providers;

import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.PrioritizedLookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import dev.verloren.midnight.psi.CompactCircuitDefinition;
import dev.verloren.midnight.psi.CompactConstBindingImpl;
import dev.verloren.midnight.psi.CompactEnumDefinition;
import dev.verloren.midnight.psi.CompactExpression;
import dev.verloren.midnight.psi.CompactImportElementImpl;
import dev.verloren.midnight.psi.CompactLedgerDeclaration;
import dev.verloren.midnight.psi.CompactMemberExprImpl;
import dev.verloren.midnight.psi.CompactNamedElement;
import dev.verloren.midnight.psi.CompactParameterImpl;
import dev.verloren.midnight.psi.CompactStructDefinition;
import dev.verloren.midnight.psi.CompactTypeDefinition;
import dev.verloren.midnight.psi.CompactTypeDefinitionImpl;
import dev.verloren.midnight.psi.CompactTypeElement;
import dev.verloren.midnight.psi.CompactWitnessDeclaration;
import dev.verloren.midnight.reference.CompactEnumMemberReference;
import dev.verloren.midnight.reference.CompactStructFieldReference;
import dev.verloren.midnight.resolve.CompactResolveUtil;
import dev.verloren.midnight.type.CompactType;
import dev.verloren.midnight.type.CompactTypeInferenceUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Completion provider for Compact member access expressions, including struct fields,
 * enum variants, and member dereferencing on expressions.
 *
 * <p>Supports both fully-formed {@link CompactMemberExprImpl} AST subtrees and broken syntax
 * "dot fallback" positions where the developer has typed a dot {@code '.'} but the parser
 * has not yet reduced the tokens into a formal member expression.</p>
 *
 * <h3>IntelliJ Platform Lifecycle &amp; Threading Contract</h3>
 * <ul>
 *   <li><b>Subsystem Role:</b> Delegate completion provider invoked by {@link dev.verloren.midnight.completion.CompactCompletionContributor}
 *       when the caret is classified as {@link dev.verloren.midnight.completion.CompactCompletionContext.Kind#MEMBER}.</li>
 *   <li><b>Calling Pipeline:</b> Executed during basic completion by {@code CodeCompletionHandlerBase}
 *       on background pooled analysis threads.</li>
 *   <li><b>Threading Model:</b> Runs under an active <b>Read Action</b> ({@code @RequiresReadLock}).
 *       All PSI tree queries and reference resolutions are read-only.</li>
 *   <li><b>Cancellation Support:</b> Multi-step resolution paths through {@link CompactResolveUtil#resolveValue}
 *       and {@link CompactResolveUtil#resolveType} cooperatively respect thread interrupts and
 *       {@link com.intellij.openapi.progress.ProgressManager#checkCanceled()}.</li>
 * </ul>
 *
 * <h3>Member Access Resolution Pipeline</h3>
 * <pre>{@code
 *   Dot Completion Position (after '.')
 *              │
 *              ├── AST MemberExpr Present?
 *              │     ├── Yes ──► MultiResolve (Enum/Struct References)
 *              │     │           + Base Expression Type Resolution
 *              │     │
 *              │     └── No  ──► Dot Fallback: Find leaf before '.'
 *              │                 + Resolve base identifier via Symbol Table
 *              │
 *              ▼
 *   Collect Members (Struct Fields / Enum Variants)
 *              │
 *              ├── Step 1: Unwrap Type Aliases (type Foo = Bar)
 *              ├── Step 2: Unwrap Imported Elements (import { S } from ...)
 *              └── Step 3: Deduplicate & Style Lookup Elements (addNamed)
 * }</pre>
 *
 * <h3>Design Decisions &amp; Architectural Rationale</h3>
 * <ul>
 *   <li><b>Dual-Path Resolution:</b> Actively typed code frequently contains parse errors. By pairing
 *       reference-based resolution with backward leaf walking ("dot fallback"), completion variants
 *       remain responsive even when the AST is incomplete.</li>
 *   <li><b>Type Alias Chaining:</b> Deeply unwraps chained type aliases via {@link #resolveAliasMembers}
 *       to surface concrete struct fields transparently.</li>
 *   <li><b>Memory Leak Guards:</b> Zero storage of PSI nodes or editor documents in static fields;
 *       elements are styled and yielded immediately into {@link CompletionResultSet}.</li>
 * </ul>
 *
 * @see dev.verloren.midnight.completion.CompactCompletionContributor
 * @see dev.verloren.midnight.completion.CompactCompletionContext
 * @see CompactResolveUtil
 * @see CompactTypeInferenceUtil
 */
public final class CompactMemberAccessCompletionProvider {

  /**
   * Private constructor enforcing static utility usage.
   */
  private CompactMemberAccessCompletionProvider() {}

  /**
   * Resolves and registers member completion candidates based on the syntax surrounding {@code position}.
   *
   * @param position the leaf PSI element at the cursor
   * @param result   the completion result set to accumulate lookup elements into
   */
  public static void addMemberCompletions(@NotNull PsiElement position, @NotNull CompletionResultSet result) {
    // Step 1: Attempt structured resolution if enclosed in a CompactMemberExpr PSI node
    CompactMemberExprImpl memberExpr = PsiTreeUtil.getParentOfType(position, CompactMemberExprImpl.class, false);
    if (memberExpr != null) {
      addMemberCompletionsFromMemberExpr(memberExpr, result);
      return;
    }

    // Step 2: Fall back to leaf token inspection if AST is incomplete
    addMemberCompletionsFromDotFallback(position, result);
  }

  /**
   * Resolves member variants from an existing {@link CompactMemberExprImpl} node via references and base expression typing.
   *
   * @param memberExpr the enclosing member expression
   * @param result     the active completion result set
   */
  private static void addMemberCompletionsFromMemberExpr(
      @NotNull CompactMemberExprImpl memberExpr,
      @NotNull CompletionResultSet result
  ) {
    // Step 1: Resolve existing reference targets (struct fields or enum variants)
    switch (Objects.requireNonNull(memberExpr.getReference())) {
      case CompactEnumMemberReference enumRef -> addResolvedNamed(result, enumRef.multiResolve(false));
      case CompactStructFieldReference structRef -> addResolvedNamed(result, structRef.multiResolve(false));
      default -> {}
    }

    // Step 2: Extract base expression type and fallback identifier text
    CompactExpression baseExpr = memberExpr.getBaseExpression();
    if (baseExpr != null) {
      CompactType baseType = baseExpr.getType();
      String typeName = baseType.name();
      if (!"Unknown".equalsIgnoreCase(typeName)) {
        addMembersFromTypeName(typeName, memberExpr, result);
      }
      String baseText = baseExpr.getText();
      if (baseText != null && !baseText.isEmpty()) {
        addMembersFromBaseText(baseText, memberExpr, result);
      }
    }
  }

  /**
   * Fallback scanner that finds the non-whitespace leaf preceding the {@code '.'} operator and resolves its members.
   *
   * @param position the current caret element
   * @param result   the active completion result set
   */
  private static void addMemberCompletionsFromDotFallback(
      @NotNull PsiElement position,
      @NotNull CompletionResultSet result
  ) {
    PsiElement previous = PsiTreeUtil.prevVisibleLeaf(position);
    if (previous != null && previous.getNode() != null && previous.getNode().getElementType() == CompactTokenTypes.DOT) {
      PsiElement leafBeforeDot = PsiTreeUtil.prevVisibleLeaf(previous);
      if (leafBeforeDot != null) {
        String baseText = leafBeforeDot.getText();
        if (baseText != null && !baseText.isEmpty()) {
          addMembersFromBaseText(baseText, leafBeforeDot, result);
        }
      }
    }
  }

  /**
   * Resolves member candidates using the textual name of the base expression.
   *
   * <p>First attempts to resolve {@code baseText} as a value binding (e.g. variable, parameter),
   * extracts its type, and queries members for that type. Afterwards, queries {@code baseText}
   * directly as a type identifier (e.g. enum name in {@code MyEnum.MEMBER}).</p>
   *
   * @param baseText the raw textual identifier preceding the dot
   * @param context  the PSI element context for scope resolution
   * @param result   the active completion result set
   */
  public static void addMembersFromBaseText(
      @NotNull String baseText,
      @NotNull PsiElement context,
      @NotNull CompletionResultSet result
  ) {
    // Step 1: Resolve as a value binding to discover its inferred type
    for (CompactNamedElement valueTarget : CompactResolveUtil.resolveValue(baseText, context)) {
      CompactNamedElement unwrapped = valueTarget instanceof CompactImportElementImpl importElem
          ? CompactResolveUtil.resolveImportElementSource(importElem)
          : valueTarget;
      if (unwrapped instanceof CompactTypeElement typeElem) {
        CompactType valType = typeElem.getType();
        String valTypeName = valType.name();
        if (!"Unknown".equalsIgnoreCase(valTypeName)) {
          addMembersFromTypeName(valTypeName, context, result);
        }
      }
    }

    // Step 2: Resolve directly as a nominal type (e.g. enum or struct definition)
    addMembersFromTypeName(baseText, context, result);
  }

  /**
   * Resolves and adds members for a named nominal type (struct or enum).
   *
   * <p>Normalizes parameterized type signatures (e.g. {@code Vector<Uint<8>>} -&gt; {@code Vector}),
   * unwraps imports and type aliases, and populates fields or enum members.</p>
   *
   * @param typeName the nominal type name
   * @param context  the PSI element context
   * @param result   the active completion result set
   */
  public static void addMembersFromTypeName(
      @NotNull String typeName,
      @NotNull PsiElement context,
      @NotNull CompletionResultSet result
  ) {
    String rawTypeName = CompactTypeInferenceUtil.getRawTypeName(typeName);
    for (CompactNamedElement target : CompactResolveUtil.resolveType(rawTypeName, context)) {
      CompactNamedElement unwrapped = (target instanceof CompactImportElementImpl importElem)
          ? CompactResolveUtil.resolveImportElementSource(importElem)
          : target;
      if (unwrapped instanceof CompactTypeDefinitionImpl typeAlias) {
        resolveAliasMembers(typeAlias, context, result);
      }
      switch (Objects.requireNonNull(unwrapped)) {
        case CompactStructDefinition structDef -> addNamed(result, structDef.getFields());
        case CompactEnumDefinition enumDef -> addNamed(result, enumDef.getMembers());
        default -> {}
      }
    }
  }

  /**
   * Recursively unwraps type alias definitions to locate and register target struct fields.
   *
   * @param typeAlias the type alias AST node
   * @param context   the contextual PSI element
   * @param result    the active completion result set
   */
  private static void resolveAliasMembers(
      @NotNull CompactTypeDefinitionImpl typeAlias,
      @NotNull PsiElement context,
      @NotNull CompletionResultSet result
  ) {
    String rawTarget = CompactTypeInferenceUtil.getRawTypeName(typeAlias.getType().name());
    for (CompactNamedElement aliasTarget : CompactResolveUtil.resolveType(rawTarget, context)) {
      CompactNamedElement aliasUnwrapped = (aliasTarget instanceof CompactImportElementImpl aliasImport)
          ? CompactResolveUtil.resolveImportElementSource(aliasImport)
          : aliasTarget;
      if (aliasUnwrapped instanceof CompactStructDefinition structDef) {
        addNamed(result, structDef.getFields());
      }
    }
  }

  /**
   * Registers elements from standard IntelliJ reference resolution results.
   *
   * @param result         the active completion result set
   * @param resolveResults array of resolve results from reference-multi-resolution
   */
  public static void addResolvedNamed(@NotNull CompletionResultSet result, ResolveResult @NotNull [] resolveResults) {
    for (ResolveResult resolveResult : resolveResults) {
      if (resolveResult.getElement() instanceof CompactNamedElement named) {
        addNamed(result, named, 60.0);
      }
    }
  }

  /**
   * Populates a collection of named elements into the completion result set, suppressing duplicates.
   *
   * @param result   the active completion result set
   * @param elements collection of candidate-named elements
   */
  public static void addNamed(
      @NotNull CompletionResultSet result,
      @NotNull Collection<? extends CompactNamedElement> elements
  ) {
    Set<String> seen = new HashSet<>();
    for (CompactNamedElement element : elements) {
      String name = element.getName();
      if (name != null && seen.add(name)) {
        addNamed(result, element, 60.0);
      }
    }
  }

  /**
   * Constructs and registers a styled, prioritized {@link LookupElementBuilder} for a named Compact element.
   *
   * @param result   the active completion result set
   * @param element  the target-named element
   * @param priority sorting priority score
   */
  public static void addNamed(
      @NotNull CompletionResultSet result,
      @NotNull CompactNamedElement element,
      double priority
  ) {
    String name = element.getName();
    if (name == null || name.isEmpty()) {
      return;
    }
    LookupElementBuilder builder = LookupElementBuilder.create(element, name);
    builder = switch (element) {
      case CompactCircuitDefinition _ -> builder.withTypeText("circuit").withBoldness(true);
      case CompactStructDefinition _ -> builder.withTypeText("struct");
      case CompactEnumDefinition _ -> builder.withTypeText("enum");
      case CompactTypeDefinition _ -> builder.withTypeText("type");
      case CompactLedgerDeclaration _ -> builder.withTypeText("ledger");
      case CompactWitnessDeclaration _ -> builder.withTypeText("witness");
      case CompactParameterImpl _ -> builder.withTypeText("param");
      case CompactConstBindingImpl _ -> builder.withTypeText("const");
      default -> builder;
    };
    if (priority != 0.0) {
      result.addElement(PrioritizedLookupElement.withPriority(builder, priority));
    } else {
      result.addElement(builder);
    }
  }
}
