package dev.verloren.midnight.completion.providers;

import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.PrioritizedLookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.psi.PsiElement;
import dev.verloren.midnight.completion.CompactParameterizedTypeInsertHandler;
import dev.verloren.midnight.psi.CompactConstBindingImpl;
import dev.verloren.midnight.psi.CompactNamedElement;
import dev.verloren.midnight.psi.CompactParameterImpl;
import dev.verloren.midnight.resolve.CompactResolveUtil;
import dev.verloren.midnight.type.CompactPrimitiveType;
import dev.verloren.midnight.type.CompactType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Completion provider for Compact values, typed expressions, default initializers, and boolean literals.
 *
 * <p>Implements intelligent type-directed ranking by inferring the {@link CompactType} expected at the caret
 * position (via {@link #getExpectedType(PsiElement)}). Candidates with compatible types receive significant priority
 * boosts, placing contextually relevant identifiers, boolean literals, or default initializers at the top of the popup.</p>
 *
 * <h3>IntelliJ Platform Lifecycle &amp; Threading Contract</h3>
 * <ul>
 *   <li><b>Subsystem Role:</b> Delegate completion provider invoked by {@link dev.verloren.midnight.completion.CompactCompletionContributor}
 *       when the caret is classified as {@link dev.verloren.midnight.completion.CompactCompletionContext.Kind#VALUE} or
 *       {@link dev.verloren.midnight.completion.CompactCompletionContext.Kind#STATEMENT}.</li>
 *   <li><b>Calling Pipeline:</b> Executed during basic completion passes by {@code CodeCompletionHandlerBase}
 *       on background pooled analysis threads.</li>
 *   <li><b>Threading Model:</b> Runs under an active <b>Read Action</b> ({@code @RequiresReadLock}).
 *       All PSI tree walks, type evaluations, and scope resolutions are read-only.</li>
 *   <li><b>Cancellation Support:</b> Resolution loops across local scopes and imports via {@link CompactResolveUtil}
 *       respect daemon thread interrupts.</li>
 * </ul>
 *
 * <h3>Type-Directed Value Completion Pipeline</h3>
 * <pre>{@code
 *   Value Completion Context
 *              │
 *              ├── Inferred Expected Type?
 *              │     ├── Present (Restricted) ──► Prioritize matching types (110.0 vs 20.0)
 *              │     │                            + Contextual Booleans & default<Type>
 *              │     │
 *              │     └── Unknown (Unrestricted) ─► Parameters/Consts (70.0)
 *              │                                   + Declarations (60.0)
 *              │                                   + Booleans (55.0) & default (50.0)
 *              │
 *              └── Common Elements ─────────────► Prefixed Imports + Value Keywords + assert
 * }</pre>
 *
 * <h3>Design Decisions &amp; Architectural Rationale</h3>
 * <ul>
 *   <li><b>Type-Directed Priority Scoring:</b> Assigns high priority (110.0) to candidates matching the expected type
 *       while demoting incompatible candidates (20.0), reducing a cognitive load during typing without hiding out-of-type variants.</li>
 *   <li><b>Syntactic Context Inference:</b> Recognizes expected boolean conditions in {@code if (...)} and {@code assert(...)},
 *       return types in circuits and constructors, and declared types on {@code const} variable bindings.</li>
 *   <li><b>Memory Leak Guards:</b> Does not retain references to {@link PsiElement}, {@link com.intellij.openapi.project.Project},
 *       or {@link com.intellij.openapi.editor.Editor} in static or persistent fields.</li>
 * </ul>
 *
 * @see dev.verloren.midnight.completion.CompactCompletionContributor
 * @see dev.verloren.midnight.completion.CompactCompletionContext
 * @see dev.verloren.midnight.resolve.CompactResolveUtil
 * @see CompactType
 */
public final class CompactValueCompletionProvider {

  /**
   * Private constructor enforcing static utility usage.
   */
  private CompactValueCompletionProvider() {}

  /**
   * Evaluates the active cursor element and populates either type-restricted or general value candidates.
   *
   * @param position the leaf PSI element at the caret
   * @param result   the active completion result set
   */
  public static void addValueCompletions(@NotNull PsiElement position, @NotNull CompletionResultSet result) {
    CompactType expectedType = getExpectedType(position);
    if (expectedType != null && !CompactPrimitiveType.UNKNOWN.equals(expectedType)) {
      addRestrictedValueCompletions(position, expectedType, result);
    } else {
      addUnrestrictedValueCompletions(position, result);
    }
  }

  /**
   * Populates value completions prioritized against an explicit contextual {@code expectedType}.
   *
   * @param position     the leaf PSI element at the cursor
   * @param expectedType the expected Compact type at this expression slot
   * @param result       the active completion result set
   */
  private static void addRestrictedValueCompletions(
      @NotNull PsiElement position,
      @NotNull CompactType expectedType,
      @NotNull CompletionResultSet result
  ) {
    // Step 1: Collect all in-scope value declarations and score based on type compatibility
    Collection<CompactNamedElement> allDecls = CompactResolveUtil.collectValueDeclarations(position);
    Set<String> seen = new HashSet<>();
    for (CompactNamedElement decl : allDecls) {
      String name = decl.getName();
      if ("left".equals(name) || "right".equals(name)) {
        continue;
      }
      CompactType declType = getCandidateType(decl);
      if (name != null && seen.add(name)) {
        double priority = isTypeCompatible(declType, expectedType) ? 110.0 : 20.0;
        CompactMemberAccessCompletionProvider.addNamed(result, decl, priority);
      }
    }

    // Step 2: Inject boolean literals if expecting a boolean expression
    if (isTypeCompatible(CompactPrimitiveType.BOOLEAN, expectedType)) {
      result.addElement(PrioritizedLookupElement.withPriority(
          LookupElementBuilder.create("true").withTypeText("Boolean").bold(), 100.0));
      result.addElement(PrioritizedLookupElement.withPriority(
          LookupElementBuilder.create("false").withTypeText("Boolean").bold(), 100.0));
    }

    // Step 3: Inject targeted default<ExpectedType> initializers
    if (!"Void".equalsIgnoreCase(expectedType.name())) {
      addDefaultCompletionsForExpected(expectedType, result);
    }

    // Step 4: Inject type-specialized Either struct and helper completions
    boolean isEitherExpected = expectedType.name().startsWith("Either");
    CompactStructLiteralCompletionProvider.addEitherAndHelperCompletions(
        result, isEitherExpected ? expectedType : null, isEitherExpected ? 115.0 : 30.0);

    // Step 5: Add imported elements, value keywords, and assert
    addPrefixed(result, CompactResolveUtil.prefixedImportNames(position, CompactResolveUtil.Namespace.VALUE));
    addValueKeywordsExceptSpecial(result);
    result.addElement(CompactKeywordCompletionProvider.createAssertLookupElement());
  }

  /**
   * Populates value completions when the contextual expected type cannot be statically inferred.
   *
   * @param position the leaf PSI element at the cursor
   * @param result   the active completion result set
   */
  private static void addUnrestrictedValueCompletions(
      @NotNull PsiElement position,
      @NotNull CompletionResultSet result
  ) {
    for (CompactNamedElement decl : CompactResolveUtil.collectValueDeclarations(position)) {
      String name = decl.getName();
      if ("left".equals(name) || "right".equals(name)) {
        continue;
      }
      double prio = (decl instanceof CompactParameterImpl || decl instanceof CompactConstBindingImpl) ? 70.0 : 60.0;
      CompactMemberAccessCompletionProvider.addNamed(result, decl, prio);
    }
    addPrefixed(result, CompactResolveUtil.prefixedImportNames(position, CompactResolveUtil.Namespace.VALUE));

    result.addElement(PrioritizedLookupElement.withPriority(
        LookupElementBuilder.create("true").withTypeText("Boolean").bold(), 55.0));
    result.addElement(PrioritizedLookupElement.withPriority(
        LookupElementBuilder.create("false").withTypeText("Boolean").bold(), 55.0));

    result.addElement(PrioritizedLookupElement.withPriority(
        LookupElementBuilder.create("default")
            .withPresentableText("default")
            .withTailText("<Type>", true)
            .withTypeText("default<Type>")
            .bold()
            .withInsertHandler(CompactParameterizedTypeInsertHandler.BRACKETS),
        50.0
    ));
    addCommonDefaultCompletions(result);
    CompactStructLiteralCompletionProvider.addEitherAndHelperCompletions(result, null, 45.0);
    CompactKeywordCompletionProvider.addAll(result, CompactKeywordCompletionProvider.VALUE_KEYWORDS);
    result.addElement(CompactKeywordCompletionProvider.createAssertLookupElement());
  }

  /**
   * Generates {@code default<Type>} initializers specifically matching the {@code expectedType}.
   *
   * @param expectedType the non-null target expected type
   * @param result       the active completion result set
   */
  private static void addDefaultCompletionsForExpected(@NotNull CompactType expectedType, @NotNull CompletionResultSet result) {
    String expectedTypeName = expectedType.name();
    if (!"Unknown".equalsIgnoreCase(expectedTypeName)) {
      result.addElement(PrioritizedLookupElement.withPriority(
          LookupElementBuilder.create("default<" + expectedTypeName + ">")
              .withPresentableText("default<" + expectedTypeName + ">")
              .withTypeText("default<Type>")
              .bold(),
          95.0
      ));
    }
    result.addElement(PrioritizedLookupElement.withPriority(
        LookupElementBuilder.create("default")
            .withPresentableText("default")
            .withTailText("<Type>", true)
            .withTypeText("default<Type>")
            .bold()
            .withInsertHandler(CompactParameterizedTypeInsertHandler.BRACKETS),
        50.0
    ));
    addCommonDefaultCompletions(result);
    result.addElement(PrioritizedLookupElement.withPriority(LookupElementBuilder.create("disclose"), 30.0));
  }

  /**
   * Adds secondary value keywords while filtering out special keywords handled by dedicated logic.
   *
   * @param result active completion result set
   */
  private static void addValueKeywordsExceptSpecial(@NotNull CompletionResultSet result) {
    for (String keyword : CompactKeywordCompletionProvider.VALUE_KEYWORDS) {
      if ("true".equals(keyword) || "false".equals(keyword) || "default".equals(keyword) || "disclose".equals(keyword)) {
        continue;
      }
      result.addElement(PrioritizedLookupElement.withPriority(LookupElementBuilder.create(keyword), 10.0));
    }
  }

  /**
   * Adds common fallback {@code default<...>} options for canonical types (Field, Boolean, Bytes&lt;32&gt;, ContractAddress).
   *
   * @param result active completion result set
   */
  public static void addCommonDefaultCompletions(@NotNull CompletionResultSet result) {
    String[] commonTypes = {"Field", "Boolean", "Bytes<32>", "ContractAddress"};
    for (String type : commonTypes) {
      result.addElement(PrioritizedLookupElement.withPriority(
          LookupElementBuilder.create("default<" + type + ">")
              .withPresentableText("default<" + type + ">")
              .withTypeText("default<Type>")
              .bold(),
          40.0
      ));
    }
  }

  /**
   * Adds prefixed import names (e.g. {@code importedModule.symbol}) into {@code result}.
   *
   * @param result active completion result set
   * @param values collection of prefixed symbol strings
   */
  public static void addPrefixed(@NotNull CompletionResultSet result, @NotNull Collection<String> values) {
    for (String value : values) {
      result.addElement(LookupElementBuilder.create(value));
    }
  }

  /**
   * Infers the expected type at the current cursor by inspecting surrounding grammatical constructs.
   *
   * @param position the leaf PSI element at the caret
   * @return inferred {@link CompactType}, or {@code null} if unconstrained
   */
  public static @Nullable CompactType getExpectedType(@NotNull PsiElement position) {
    return CompactValueTypeUtil.getExpectedType(position);
  }

  /**
   * Extracts the evaluated {@link CompactType} of a named candidate declaration.
   *
   * @param element the candidate named element
   * @return resolved type of the candidate
   */
  public static @NotNull CompactType getCandidateType(@NotNull CompactNamedElement element) {
    return CompactValueTypeUtil.getCandidateType(element);
  }

  /**
   * Checks whether a candidate expression type is assignable or compatible with the expected type.
   *
   * @param candidateType the type of the item being suggested
   * @param expectedType  the contextual expected type constraint
   * @return {@code true} if types are compatible
   */
  public static boolean isTypeCompatible(@NotNull CompactType candidateType, @Nullable CompactType expectedType) {
    return CompactValueTypeUtil.isTypeCompatible(candidateType, expectedType);
  }
}
