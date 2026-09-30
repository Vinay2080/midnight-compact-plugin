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
 * Completion provider for Compact keywords, declaration templates, and modifiers.
 */
public final class CompactKeywordCompletionProvider {

  public static final String[] DECLARATION_KEYWORDS = {
      "pragma", "include", "import", "export", "module", "contract",
      "struct", "enum", "type", "witness", "constructor", "circuit", "ledger"
  };

  public static final String[] STATEMENT_KEYWORDS = {
      "const", "if", "for", "return", "emit"
  };

  public static final String[] VALUE_KEYWORDS = {
      "true", "false", "default", "disclose", "map", "fold", "pad", "slice", "emit"
  };

  private CompactKeywordCompletionProvider() {}

  public static void addDeclarationCompletions(@NotNull CompletionResultSet result) {
    registerDeclarationTemplate(result, CompactDeclarationType.LEDGER, 125.0, 110.0, CompactLedgerInsertHandler.INSTANCE);
    registerDeclarationTemplate(result, CompactDeclarationType.CIRCUIT, 120.0, 105.0, new CompactDeclarationInsertHandler(CompactDeclarationType.CIRCUIT));
    registerDeclarationTemplate(result, CompactDeclarationType.STRUCT, 115.0, 105.0, new CompactDeclarationInsertHandler(CompactDeclarationType.STRUCT));
    registerDeclarationTemplate(result, CompactDeclarationType.ENUM, 115.0, 105.0, new CompactDeclarationInsertHandler(CompactDeclarationType.ENUM));
    registerDeclarationTemplate(result, CompactDeclarationType.TYPE, 115.0, 105.0, new CompactDeclarationInsertHandler(CompactDeclarationType.TYPE));
    registerDeclarationTemplate(result, CompactDeclarationType.MODULE, 110.0, 100.0, new CompactDeclarationInsertHandler(CompactDeclarationType.MODULE));
    registerDeclarationTemplate(result, CompactDeclarationType.CONTRACT, 110.0, 100.0, new CompactDeclarationInsertHandler(CompactDeclarationType.CONTRACT));
    registerDeclarationTemplate(result, CompactDeclarationType.WITNESS, 115.0, 100.0, new CompactDeclarationInsertHandler(CompactDeclarationType.WITNESS));

    addAll(result, DECLARATION_KEYWORDS);
  }

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

  public static void addAfterSealedCompletions(@NotNull CompletionResultSet result) {
    LookupElementBuilder builder = createDeclarationLookupElement(
        CompactDeclarationType.LEDGER, "ledger", " <name>: <type>;", CompactLedgerInsertHandler.INSTANCE, false);
    result.addElement(PrioritizedLookupElement.withPriority(builder, 120.0));
  }

  public static void addAfterPureCompletions(@NotNull CompletionResultSet result) {
    LookupElementBuilder builder = createDeclarationLookupElement(
        CompactDeclarationType.CIRCUIT, "circuit", " <name>(...): <type> { ... }",
        new CompactDeclarationInsertHandler(CompactDeclarationType.CIRCUIT), false);
    result.addElement(PrioritizedLookupElement.withPriority(builder, 120.0));
  }

  public static void addAfterNewCompletions(@NotNull CompletionResultSet result) {
    LookupElementBuilder builder = createDeclarationLookupElement(
        CompactDeclarationType.TYPE, "type", " <name> = <type>;",
        new CompactDeclarationInsertHandler(CompactDeclarationType.TYPE), false);
    result.addElement(PrioritizedLookupElement.withPriority(builder, 120.0));
  }

  public static void addAll(@NotNull CompletionResultSet result, String @NotNull [] values) {
    for (String value : values) {
      result.addElement(LookupElementBuilder.create(value));
    }
  }

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
