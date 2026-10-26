package dev.verloren.midnight.completion.providers;

import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.PrioritizedLookupElement;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import dev.verloren.midnight.completion.CompactAssertInsertHandler;
import dev.verloren.midnight.completion.CompactDeclarationInsertHandler;
import dev.verloren.midnight.completion.CompactLedgerInsertHandler;
import dev.verloren.midnight.ide.templates.CompactDeclarationTriggerResolver;
import dev.verloren.midnight.ide.templates.CompactDeclarationType;
import org.jetbrains.annotations.NotNull;

/**
 * High-performance completion provider for Compact language keywords, live declaration templates, and modifier tokens.
 *
 * <p>Generates prioritized lookup elements for top-level declaration constructs (e.g. {@code circuit},
 * {@code ledger}, {@code struct}), post-export declarations, statement keywords, and value-position keywords.
 * Integrates directly with {@link CompactDeclarationTriggerResolver} to register prefix aliases and custom
 * {@link InsertHandler} implementations (e.g. {@link CompactDeclarationInsertHandler}, {@link CompactLedgerInsertHandler})
 * to drive interactive live template expansions with tab-stops.</p>
 *
 * <h3>IntelliJ Platform Lifecycle &amp; Threading Contract</h3>
 * <ul>
 *   <li><b>Subsystem Role:</b> Pure completion provider and lookup factory invoked as a delegate
 *       by {@link dev.verloren.midnight.completion.CompactCompletionContributor}.</li>
 *   <li><b>Calling Pipeline:</b> Executed during basic code completion ({@link com.intellij.codeInsight.completion.CompletionType#BASIC})
 *       by {@code com.intellij.codeInsight.completion.CodeCompletionHandlerBase} across background pooled worker threads.</li>
 *   <li><b>Threading Model:</b> Runs within an active <b>Read Action</b> ({@code @RequiresReadLock}).
 *       All methods are read-only and mutate only the passed {@link CompletionResultSet}. Document writes, AST alterations,
 *       and interactive template launches are strictly deferred to {@link InsertHandler} invocations executed on the EDT
 *       upon user item selection.</li>
 *   <li><b>Cancellation Support:</b> Performs zero disk I/O, heavy parsing, or blocking operations.
 *       Generates lightweight in-memory lookup builders, cooperatively yielding to IDE daemon cancellation
 *       and typing interrupts without blocking the EDT.</li>
 * </ul>
 *
 * <h3>Subsystem Interaction &amp; Request Flow</h3>
 * <pre>{@code
 *   CompactCompletionContributor (Worker Thread, Read Action)
 *              │
 *              ├─► CompactCompletionContext.classify(caret)
 *              │         │
 *              │         ▼ (TOP_LEVEL / AFTER_EXPORT / STATEMENT / VALUE)
 *              ▼
 *   CompactKeywordCompletionProvider
 *              │
 *              ├── registerDeclarationTemplate() ──► CompactDeclarationTriggerResolver (aliases: "ci", "cir")
 *              │                                 └── Attaches CompactDeclarationInsertHandler (Live Template)
 *              │
 *              └── addAll() / PrioritizedLookupElement.withPriority() ──► CompletionResultSet
 * }</pre>
 *
 * <h3>Keyword Priority &amp; Weight Hierarchy</h3>
 * <pre>{@code
 *   Priority
 *     125.0  ──► export ledger / ledger templates (Primary state root)
 *     120.0  ──► export circuit / circuit templates, sealed ledger, pure circuit
 *     115.0  ──► struct, enum, type, witness declaration templates
 *     110.0  ──► module, contract declaration templates
 *     100.0  ──► Export modifiers ('pure', 'sealed', 'new')
 *      90.0  ──► Compound export block ('export { ... }')
 *      10.0  ──► assert statement with insert handler
 *       0.0  ──► Bare fallback keywords (pragma, include, import, etc.)
 * }</pre>
 *
 * <h3>Design Decisions &amp; Architectural Rationale</h3>
 * <ul>
 *   <li><b>Stateless Utility Provider:</b> Declared {@code final} with a private constructor to prevent
 *       instantiation. All methods are pure static functions, eliminating heap allocation overhead on keystrokes.</li>
 *   <li><b>Dual-Form Generation:</b> Generates both exported ({@code export circuit}) and bare ({@code circuit})
 *       forms simultaneously with tiered priorities so developers can declare and export in a single keystroke.</li>
 *   <li><b>Live Template Binding:</b> Attaches specialized insert handlers (e.g. {@link CompactLedgerInsertHandler},
 *       {@link CompactDeclarationInsertHandler}) to template lookup elements rather than simple text snippets.</li>
 *   <li><b>Memory Leak Guards:</b> Zero retention of {@link com.intellij.openapi.project.Project},
 *       {@link com.intellij.psi.PsiElement}, or {@link com.intellij.openapi.editor.Editor} instances in static memory.</li>
 * </ul>
 *
 * @see dev.verloren.midnight.completion.CompactCompletionContributor
 * @see dev.verloren.midnight.completion.CompactDeclarationInsertHandler
 * @see dev.verloren.midnight.completion.CompactLedgerInsertHandler
 * @see dev.verloren.midnight.completion.CompactAssertInsertHandler
 * @see CompactDeclarationTriggerResolver
 */
public final class CompactKeywordCompletionProvider {

  /**
   * Top-level declaration keywords valid at file, contract, or module scope.
   *
   * <p>Immutable token array registered as fallback items when declaration templates are matched.</p>
   */
  public static final String[] DECLARATION_KEYWORDS = {
      "pragma", "include", "import", "export", "module", "contract",
      "struct", "enum", "type", "witness", "constructor", "circuit", "ledger"
  };

  /**
   * Local statement keywords valid inside circuit or function block bodies.
   *
   * <p>Immutable token array registered inside imperative block contexts.</p>
   */
  public static final String[] STATEMENT_KEYWORDS = {
      "const", "if", "for", "return", "emit"
  };

  /**
   * Value/expression keywords valid as operands in expression contexts.
   *
   * <p>Immutable token array registered inside expression and parameter positions.</p>
   */
  public static final String[] VALUE_KEYWORDS = {
      "true", "false", "default", "disclose", "map", "fold", "pad", "slice", "emit"
  };

  /**
   * Private constructor enforcing static utility usage and preventing instantiation.
   */
  private CompactKeywordCompletionProvider() {}

  /**
   * Populates top-level declaration live templates and raw declaration keywords into {@code result}.
   *
   * @param result the active completion result set
   */
  public static void addDeclarationCompletions(@NotNull CompletionResultSet result) {
    // Step 1: Register high-priority interactive declaration templates with custom insert handlers
    registerDeclarationTemplate(result, CompactDeclarationType.LEDGER, 125.0, 110.0, CompactLedgerInsertHandler.INSTANCE);
    registerDeclarationTemplate(result, CompactDeclarationType.CIRCUIT, 120.0, 105.0, new CompactDeclarationInsertHandler(CompactDeclarationType.CIRCUIT));
    registerDeclarationTemplate(result, CompactDeclarationType.STRUCT, 115.0, 105.0, new CompactDeclarationInsertHandler(CompactDeclarationType.STRUCT));
    registerDeclarationTemplate(result, CompactDeclarationType.ENUM, 115.0, 105.0, new CompactDeclarationInsertHandler(CompactDeclarationType.ENUM));
    registerDeclarationTemplate(result, CompactDeclarationType.TYPE, 115.0, 105.0, new CompactDeclarationInsertHandler(CompactDeclarationType.TYPE));
    registerDeclarationTemplate(result, CompactDeclarationType.MODULE, 110.0, 100.0, new CompactDeclarationInsertHandler(CompactDeclarationType.MODULE));
    registerDeclarationTemplate(result, CompactDeclarationType.CONTRACT, 110.0, 100.0, new CompactDeclarationInsertHandler(CompactDeclarationType.CONTRACT));
    registerDeclarationTemplate(result, CompactDeclarationType.WITNESS, 115.0, 100.0, new CompactDeclarationInsertHandler(CompactDeclarationType.WITNESS));

    // Step 2: Add raw fallback declaration keywords
    addAll(result, DECLARATION_KEYWORDS);
  }

  /**
   * Populates declaration templates and modifiers valid immediately following an {@code export} keyword.
   *
   * @param result the active completion result set
   */
  public static void addAfterExportCompletions(@NotNull CompletionResultSet result) {
    registerAfterExportTemplate(result, CompactDeclarationType.LEDGER, 120.0, CompactLedgerInsertHandler.INSTANCE);
    registerAfterExportTemplate(result, CompactDeclarationType.CIRCUIT, 120.0, new CompactDeclarationInsertHandler(CompactDeclarationType.CIRCUIT));
    registerAfterExportTemplate(result, CompactDeclarationType.STRUCT, 115.0, new CompactDeclarationInsertHandler(CompactDeclarationType.STRUCT));
    registerAfterExportTemplate(result, CompactDeclarationType.ENUM, 115.0, new CompactDeclarationInsertHandler(CompactDeclarationType.ENUM));
    registerAfterExportTemplate(result, CompactDeclarationType.TYPE, 115.0, new CompactDeclarationInsertHandler(CompactDeclarationType.TYPE));
    registerAfterExportTemplate(result, CompactDeclarationType.MODULE, 110.0, new CompactDeclarationInsertHandler(CompactDeclarationType.MODULE));
    registerAfterExportTemplate(result, CompactDeclarationType.CONTRACT, 110.0, new CompactDeclarationInsertHandler(CompactDeclarationType.CONTRACT));
    registerAfterExportTemplate(result, CompactDeclarationType.WITNESS, 105.0, new CompactDeclarationInsertHandler(CompactDeclarationType.WITNESS));

    addExportModifierKeywords(result);
  }

  /**
   * Adds modifier keywords valid immediately after {@code export} (such as {@code pure}, {@code sealed}, and {@code new}).
   *
   * @param result the active completion result set
   */
  private static void addExportModifierKeywords(@NotNull CompletionResultSet result) {
    result.addElement(PrioritizedLookupElement.withPriority(
        LookupElementBuilder.create("pure")
            .withLookupString("pure")
            .withPresentableText("pure")
            .withTailText(" circuit ...", true)
            .bold(),
        100.0
    ));
    result.addElement(PrioritizedLookupElement.withPriority(
        LookupElementBuilder.create("sealed")
            .withLookupString("sealed")
            .withPresentableText("sealed")
            .withTailText(" ledger ...", true)
            .bold(),
        100.0
    ));
    result.addElement(PrioritizedLookupElement.withPriority(
        LookupElementBuilder.create("new")
            .withLookupString("new")
            .withPresentableText("new")
            .withTailText(" type ...", true)
            .bold(),
        95.0
    ));
    result.addElement(PrioritizedLookupElement.withPriority(
        LookupElementBuilder.create("{")
            .withLookupString("export {")
            .withPresentableText("{ ... }")
            .withTailText(" (export form)", true)
            .bold(),
        90.0
    ));
  }

  /**
   * Suggests the {@code ledger} declaration template following an {@code export sealed} sequence.
   *
   * @param result the active completion result set
   */
  public static void addAfterSealedCompletions(@NotNull CompletionResultSet result) {
    LookupElementBuilder builder = createDeclarationLookupElement(
        CompactDeclarationType.LEDGER, "ledger", " <name>: <type>;", CompactLedgerInsertHandler.INSTANCE, false);
    result.addElement(PrioritizedLookupElement.withPriority(builder, 120.0));
  }

  /**
   * Suggests the {@code circuit} declaration template following an {@code export pure} sequence.
   *
   * @param result the active completion result set
   */
  public static void addAfterPureCompletions(@NotNull CompletionResultSet result) {
    LookupElementBuilder builder = createDeclarationLookupElement(
        CompactDeclarationType.CIRCUIT, "circuit", " <name>(...): <type> { ... }",
        new CompactDeclarationInsertHandler(CompactDeclarationType.CIRCUIT), false);
    result.addElement(PrioritizedLookupElement.withPriority(builder, 120.0));
  }

  /**
   * Suggests the {@code type} alias declaration template following an {@code export new} sequence.
   *
   * @param result the active completion result set
   */
  public static void addAfterNewCompletions(@NotNull CompletionResultSet result) {
    LookupElementBuilder builder = createDeclarationLookupElement(
        CompactDeclarationType.TYPE, "type", " <name> = <type>;",
        new CompactDeclarationInsertHandler(CompactDeclarationType.TYPE), false);
    result.addElement(PrioritizedLookupElement.withPriority(builder, 120.0));
  }

  /**
   * Appends an array of simple string keywords to the completion result set.
   *
   * @param result the active completion result set
   * @param values keyword tokens to register
   */
  public static void addAll(@NotNull CompletionResultSet result, String @NotNull [] values) {
    for (String value : values) {
      result.addElement(LookupElementBuilder.create(value));
    }
  }

  /**
   * Creates a pre-configured {@link LookupElement} for the {@code assert} statement keyword
   * wired to {@link CompactAssertInsertHandler#INSTANCE}.
   *
   * @return prioritized assert lookup element
   */
  public static @NotNull LookupElement createAssertLookupElement() {
    return PrioritizedLookupElement.withPriority(
        LookupElementBuilder.create("assert")
            .withPresentableText("assert")
            .withTailText("(condition, \"message\")", true)
            .withTypeText("statement")
            .bold()
            .withInsertHandler(CompactAssertInsertHandler.INSTANCE),
        10.0
    );
  }

  /**
   * Helper registering both exportable and bare declaration template variants for a declaration type.
   *
   * @param result         the completion result set
   * @param type           declaration type metadata
   * @param exportPriority priority score for the exported form
   * @param barePriority   priority score for the bare form
   * @param insertHandler  the insert handler responsible for launching the live template
   */
  private static void registerDeclarationTemplate(
      @NotNull CompletionResultSet result,
      @NotNull CompactDeclarationType type,
      double exportPriority,
      double barePriority,
      @NotNull InsertHandler<LookupElement> insertHandler) {

    String baseName = type.getBaseName();
    String tailText = getDeclarationTailText(type);

    if (type.isExportable()) {
      LookupElementBuilder exportBuilder = createDeclarationLookupElement(
          type, "export " + baseName, tailText, insertHandler, true);
      result.addElement(PrioritizedLookupElement.withPriority(exportBuilder, exportPriority));
    }

    LookupElementBuilder bareBuilder = createDeclarationLookupElement(
        type, baseName, tailText, insertHandler, false);
    result.addElement(PrioritizedLookupElement.withPriority(bareBuilder, barePriority));
  }

  /**
   * Helper registering bare declaration templates valid after a preceding {@code export} token.
   *
   * @param result        the completion result set
   * @param type          declaration type metadata
   * @param priority      priority weight
   * @param insertHandler the template insert handler
   */
  private static void registerAfterExportTemplate(
      @NotNull CompletionResultSet result,
      @NotNull CompactDeclarationType type,
      double priority,
      @NotNull InsertHandler<LookupElement> insertHandler) {

    String tailText = getDeclarationTailText(type);
    LookupElementBuilder builder = createDeclarationLookupElement(
        type, type.getBaseName(), tailText, insertHandler, false);
    result.addElement(PrioritizedLookupElement.withPriority(builder, priority));
  }

  /**
   * Constructs a styled {@link LookupElementBuilder} populated with custom lookup trigger aliases
   * generated by {@link CompactDeclarationTriggerResolver}.
   *
   * @param type          declaration type metadata
   * @param lookupName    primary lookup string
   * @param tailText      tail presentation preview string
   * @param insertHandler the template insert handler
   * @param isExport      whether this item represents an exported declaration
   * @return configured lookup element builder
   */
  private static @NotNull LookupElementBuilder createDeclarationLookupElement(
      @NotNull CompactDeclarationType type,
      @NotNull String lookupName,
      @NotNull String tailText,
      @NotNull InsertHandler<LookupElement> insertHandler,
      boolean isExport) {
    LookupElementBuilder builder = LookupElementBuilder.create(lookupName)
        .withPresentableText(lookupName)
        .withTailText(tailText, true)
        .withTypeText(type.getBaseName())
        .bold()
        .withInsertHandler(insertHandler);

    for (String lookup : CompactDeclarationTriggerResolver.generateLookupStrings(type, isExport)) {
      builder = builder.withLookupString(lookup);
    }
    return builder;
  }

  /**
   * Resolves the descriptive tail text indicating the structural signature of the declaration template.
   *
   * @param type declaration type metadata
   * @return tail text preview string
   */
  private static @NotNull String getDeclarationTailText(@NotNull CompactDeclarationType type) {
    return switch (type) {
      case CIRCUIT -> " <name>(...): <type> { ... }";
      case WITNESS -> " <name>(...): <type>;";
      case STRUCT, ENUM, MODULE, CONTRACT -> " <name> { ... }";
      case TYPE -> " <name> = <type>;";
      case LEDGER -> " <name>: <type>;";
      case CONST -> " <name>: <type> = <value>;";
    };
  }
}
