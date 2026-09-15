package dev.verloren.midnight.completion;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import dev.verloren.midnight.CompactLanguage;
import dev.verloren.midnight.ide.templates.CompactDeclarationTriggerResolver;
import dev.verloren.midnight.ide.templates.CompactDeclarationType;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import dev.verloren.midnight.psi.*;
import dev.verloren.midnight.reference.CompactEnumMemberReference;
import dev.verloren.midnight.reference.CompactStructFieldReference;
import dev.verloren.midnight.resolve.CompactResolveUtil;
import dev.verloren.midnight.type.CompactPrimitiveType;
import dev.verloren.midnight.type.CompactType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Code completion provider for Compact smart contracts.
 *
 * <p>Extends {@link CompletionContributor} and classifies the caret context via
 * {@link CompactCompletionContext#classify(PsiElement)} into keywords, types, values,
 * or members, populating the {@link CompletionResultSet} with contextually valid lookup items.</p>
 */
public class CompactCompletionContributor extends CompletionContributor {

  public static final String[] DECLARATION_KEYWORDS = {
      "pragma", "include", "import", "export", "module", "contract", "struct", "enum", "type", "witness", "constructor", "circuit", "ledger"
  };

  public static final String[] STATEMENT_KEYWORDS = {
      "const", "if", "for", "return", "assert", "emit"
  };

  public static final String[] VALUE_KEYWORDS = {
      "true", "false", "default", "disclose", "map", "fold", "pad", "slice", "assert", "emit"
  };

  public static final String[] BUILTIN_TYPES = {
      "Boolean", "Bytes", "Field", "Opaque", "Uint", "Vector", "State", "Counter", "Void",
      "JubjubScalar", "JubjubPoint", "Secp256k1Base", "Secp256k1Scalar", "Secp256k1Point"
  };

  @SuppressWarnings("this-escape")
  public CompactCompletionContributor() {
    extend(CompletionType.BASIC, PlatformPatterns.psiElement().withLanguage(CompactLanguage.INSTANCE), new CompletionProvider<>() {
      @Override
      protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet result) {
        addCompactCompletions(parameters.getPosition(), result);
      }
    });
  }

  private static void addCompactCompletions(@NotNull PsiElement position, @NotNull CompletionResultSet result) {
    switch (CompactCompletionContext.classify(position)) {
      case KEYWORD -> addDeclarationCompletions(result);
      case AFTER_EXPORT -> addAfterExportCompletions(result);
      case AFTER_SEALED -> addAfterSealedCompletions(result);
      case AFTER_PURE -> addAfterPureCompletions(result);
      case AFTER_NEW -> addAfterNewCompletions(result);
      case STATEMENT -> {
        addAll(result, STATEMENT_KEYWORDS);
        addValueCompletions(position, result);
      }
      case TYPE -> {
        addAll(result, BUILTIN_TYPES);
        addNamed(result, CompactResolveUtil.collectTypeDeclarations(position));
        addPrefixed(result, CompactResolveUtil.prefixedImportNames(position, CompactResolveUtil.Namespace.TYPE));
      }
      case MEMBER -> addMemberCompletions(position, result);
      case VALUE -> addValueCompletions(position, result);
      case NONE -> {}
    }
  }

  private static void addDeclarationCompletions(@NotNull CompletionResultSet result) {
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

  private static void addAfterExportCompletions(@NotNull CompletionResultSet result) {
    registerAfterExportTemplate(result, CompactDeclarationType.LEDGER, 120.0, CompactLedgerInsertHandler.INSTANCE);
    registerAfterExportTemplate(result, CompactDeclarationType.CIRCUIT, 120.0, new CompactDeclarationInsertHandler(CompactDeclarationType.CIRCUIT));
    registerAfterExportTemplate(result, CompactDeclarationType.STRUCT, 115.0, new CompactDeclarationInsertHandler(CompactDeclarationType.STRUCT));
    registerAfterExportTemplate(result, CompactDeclarationType.ENUM, 115.0, new CompactDeclarationInsertHandler(CompactDeclarationType.ENUM));
    registerAfterExportTemplate(result, CompactDeclarationType.TYPE, 115.0, new CompactDeclarationInsertHandler(CompactDeclarationType.TYPE));
    registerAfterExportTemplate(result, CompactDeclarationType.MODULE, 110.0, new CompactDeclarationInsertHandler(CompactDeclarationType.MODULE));
    registerAfterExportTemplate(result, CompactDeclarationType.CONTRACT, 110.0, new CompactDeclarationInsertHandler(CompactDeclarationType.CONTRACT));
    registerAfterExportTemplate(result, CompactDeclarationType.WITNESS, 105.0, new CompactDeclarationInsertHandler(CompactDeclarationType.WITNESS));

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

  private static void registerDeclarationTemplate(
      @NotNull CompletionResultSet result,
      @NotNull CompactDeclarationType type,
      double exportPriority,
      double barePriority,
      @NotNull InsertHandler<LookupElement> insertHandler) {

    String baseName = type.getBaseName();
    String tailText = getDeclarationTailText(type);

    if (type.isExportable()) {
      LookupElementBuilder exportBuilder = LookupElementBuilder.create("export " + baseName)
          .withPresentableText("export " + baseName)
          .withTailText(tailText, true)
          .withTypeText(baseName)
          .bold()
          .withInsertHandler(insertHandler);

      for (String lookup : CompactDeclarationTriggerResolver.generateLookupStrings(type, true)) {
        exportBuilder = exportBuilder.withLookupString(lookup);
      }
      result.addElement(PrioritizedLookupElement.withPriority(exportBuilder, exportPriority));
    }

    LookupElementBuilder bareBuilder = LookupElementBuilder.create(baseName)
        .withPresentableText(baseName)
        .withTailText(tailText, true)
        .withTypeText(baseName)
        .bold()
        .withInsertHandler(insertHandler);

    for (String lookup : CompactDeclarationTriggerResolver.generateLookupStrings(type, false)) {
      bareBuilder = bareBuilder.withLookupString(lookup);
    }
    result.addElement(PrioritizedLookupElement.withPriority(bareBuilder, barePriority));
  }

  private static void registerAfterExportTemplate(
      @NotNull CompletionResultSet result,
      @NotNull CompactDeclarationType type,
      double priority,
      @NotNull InsertHandler<LookupElement> insertHandler) {

    String baseName = type.getBaseName();
    String tailText = getDeclarationTailText(type);

    LookupElementBuilder builder = LookupElementBuilder.create(baseName)
        .withPresentableText(baseName)
        .withTailText(tailText, true)
        .withTypeText(baseName)
        .bold()
        .withInsertHandler(insertHandler);

    for (String lookup : CompactDeclarationTriggerResolver.generateLookupStrings(type, false)) {
      builder = builder.withLookupString(lookup);
    }
    result.addElement(PrioritizedLookupElement.withPriority(builder, priority));
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

  private static void addAfterSealedCompletions(@NotNull CompletionResultSet result) {
    LookupElementBuilder builder = LookupElementBuilder.create("ledger")
        .withPresentableText("ledger")
        .withTailText(" <name>: <type>;", true)
        .withTypeText("ledger")
        .bold()
        .withInsertHandler(CompactLedgerInsertHandler.INSTANCE);
    for (String lookup : CompactDeclarationTriggerResolver.generateLookupStrings(CompactDeclarationType.LEDGER, false)) {
      builder = builder.withLookupString(lookup);
    }
    result.addElement(PrioritizedLookupElement.withPriority(builder, 120.0));
  }

  private static void addAfterPureCompletions(@NotNull CompletionResultSet result) {
    LookupElementBuilder builder = LookupElementBuilder.create("circuit")
        .withPresentableText("circuit")
        .withTailText(" <name>(...): <type> { ... }", true)
        .withTypeText("circuit")
        .bold()
        .withInsertHandler(new CompactDeclarationInsertHandler(CompactDeclarationType.CIRCUIT));
    for (String lookup : CompactDeclarationTriggerResolver.generateLookupStrings(CompactDeclarationType.CIRCUIT, false)) {
      builder = builder.withLookupString(lookup);
    }
    result.addElement(PrioritizedLookupElement.withPriority(builder, 120.0));
  }

  private static void addAfterNewCompletions(@NotNull CompletionResultSet result) {
    LookupElementBuilder builder = LookupElementBuilder.create("type")
        .withPresentableText("type")
        .withTailText(" <name> = <type>;", true)
        .withTypeText("type")
        .bold()
        .withInsertHandler(new CompactDeclarationInsertHandler(CompactDeclarationType.TYPE));
    for (String lookup : CompactDeclarationTriggerResolver.generateLookupStrings(CompactDeclarationType.TYPE, false)) {
      builder = builder.withLookupString(lookup);
    }
    result.addElement(PrioritizedLookupElement.withPriority(builder, 120.0));
  }

  private static void addAll(@NotNull CompletionResultSet result, String @NotNull [] values) {
    for (String value : values) {
      result.addElement(LookupElementBuilder.create(value));
    }
  }

  private static void addNamed(@NotNull CompletionResultSet result, @NotNull Collection<? extends CompactNamedElement> elements) {
    Set<String> seen = new HashSet<>();
    for (CompactNamedElement element : elements) {
      String name = element.getName();
      if (name != null && seen.add(name)) {
        addNamed(result, element);
      }
    }
  }

  private static void addPrefixed(@NotNull CompletionResultSet result, @NotNull Collection<String> values) {
    for (String value : values) {
      result.addElement(LookupElementBuilder.create(value));
    }
  }

  private static void addResolvedNamed(@NotNull CompletionResultSet result, ResolveResult @NotNull [] resolveResults) {
    for (ResolveResult resolveResult : resolveResults) {
      if (resolveResult.getElement() instanceof CompactNamedElement named) {
        addNamed(result, named);
      }
    }
  }

  private static void addMemberCompletions(@NotNull PsiElement position, @NotNull CompletionResultSet result) {
    CompactMemberExprImpl memberExpr = PsiTreeUtil.getParentOfType(position, CompactMemberExprImpl.class, false);

    if (memberExpr != null) {
      // 1. Check existing references (Enum / Struct field)
      switch (Objects.requireNonNull(memberExpr.getReference())) {
        case CompactEnumMemberReference enumRef -> addResolvedNamed(result, enumRef.multiResolve(false));
        case CompactStructFieldReference structRef -> addResolvedNamed(result, structRef.multiResolve(false));
        default -> {}
      }

      CompactExpression baseExpr = memberExpr.getBaseExpression();
      if (baseExpr != null) {
        // Resolve type of base expression (e.g., cfg -> Config)
        CompactType baseType = baseExpr.getType();
        String typeName = baseType.name();
        if (!"Unknown".equalsIgnoreCase(typeName)) {
          addMembersFromTypeName(typeName, memberExpr, result);
        }

        // If base is an identifier or reference
        String baseText = baseExpr.getText();
        if (baseText != null && !baseText.isEmpty()) {
          addMembersFromBaseText(baseText, memberExpr, result);
        }
      }
      return;
    }

    // 2. Fallback when the caret is immediately after the DOT token and not enclosed in CompactMemberExprImpl
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

  private static void addMembersFromBaseText(
      @NotNull String baseText,
      @NotNull PsiElement context,
      @NotNull CompletionResultSet result
  ) {
    for (CompactNamedElement valueTarget : CompactResolveUtil.resolveValue(baseText, context)) {
      if (valueTarget instanceof CompactImportElementImpl importElem) {
        valueTarget = CompactResolveUtil.resolveImportElementSource(importElem);
      }
      if (valueTarget instanceof CompactTypeElement typeElem) {
        CompactType valType = typeElem.getType();
        String valTypeName = valType.name();
        if (!"Unknown".equalsIgnoreCase(valTypeName)) {
          addMembersFromTypeName(valTypeName, context, result);
        }
      }
    }
    addMembersFromTypeName(baseText, context, result);
  }

  private static void addMembersFromTypeName(
      @NotNull String typeName,
      @NotNull PsiElement context,
      @NotNull CompletionResultSet result
  ) {
    for (CompactNamedElement target : CompactResolveUtil.resolveType(typeName, context)) {
      CompactNamedElement unwrapped = (target instanceof CompactImportElementImpl importElem)
          ? CompactResolveUtil.resolveImportElementSource(importElem)
          : target;
      switch (Objects.requireNonNull(unwrapped)) {
        case CompactStructDefinition structDef -> addNamed(result, structDef.getFields());
        case CompactEnumDefinition enumDef -> addNamed(result, enumDef.getMembers());
        default -> {}
      }
    }
  }

  private static void addValueCompletions(@NotNull PsiElement position, @NotNull CompletionResultSet result) {
    CompactType expectedType = getExpectedType(position);

    if (expectedType != null && !CompactPrimitiveType.UNKNOWN.equals(expectedType)) {
      Collection<CompactNamedElement> allDecls = CompactResolveUtil.collectValueDeclarations(position);
      Set<String> seen = new HashSet<>();

      for (CompactNamedElement decl : allDecls) {
        CompactType declType = getCandidateType(decl);
        if (isTypeCompatible(declType, expectedType)) {
          String name = decl.getName();
          if (name != null && seen.add(name)) {
            addNamed(result, decl);
          }
        }
      }

      // Add compatible expression keywords with high priority
      if (isTypeCompatible(CompactPrimitiveType.BOOLEAN, expectedType)) {
        result.addElement(PrioritizedLookupElement.withPriority(
            LookupElementBuilder.create("true").bold(), 100.0));
        result.addElement(PrioritizedLookupElement.withPriority(
            LookupElementBuilder.create("false").bold(), 100.0));
      }
      if (!"Void".equalsIgnoreCase(expectedType.name())) {
        result.addElement(PrioritizedLookupElement.withPriority(
            LookupElementBuilder.create("default"), 50.0));
        result.addElement(PrioritizedLookupElement.withPriority(
            LookupElementBuilder.create("disclose"), 50.0));
      }
      return;
    }

    // Default / unrestricted value completion
    addNamed(result, CompactResolveUtil.collectValueDeclarations(position));
    addPrefixed(result, CompactResolveUtil.prefixedImportNames(position, CompactResolveUtil.Namespace.VALUE));
    addAll(result, VALUE_KEYWORDS);
  }

  public static @Nullable CompactType getExpectedType(@NotNull PsiElement position) {
    // 1. Check if in the return expression context
    if (isReturnContext(position)) {
      PsiElement enclosing = PsiTreeUtil.getParentOfType(position,
          CompactCircuitDefinition.class,
          CompactWitnessDeclaration.class,
          CompactConstructorDeclaration.class);
      CompactType callableRt = CompactPsiUtil.getCallableReturnType(enclosing);
      if (!CompactPrimitiveType.UNKNOWN.equals(callableRt)) {
        return callableRt;
      }
    }

    // 2. Check if in const x: Type = <caret> context
    CompactConstBindingImpl binding = PsiTreeUtil.getParentOfType(position, CompactConstBindingImpl.class);
    if (binding != null) {
      CompactTypeElement typeElem = PsiTreeUtil.findChildOfType(binding, CompactTypeElement.class);
      if (typeElem != null) {
        return typeElem.getType();
      }
    }

    // 3. Check if in the "is (<caret>)" or "assert(<caret>)" condition context
    PsiElement prev = PsiTreeUtil.prevVisibleLeaf(position);
    if (prev != null && prev.getNode() != null && prev.getNode().getElementType() == CompactTokenTypes.LPAREN) {
      PsiElement beforeParen = PsiTreeUtil.prevVisibleLeaf(prev);
      if (beforeParen != null && beforeParen.getNode() != null) {
        com.intellij.psi.tree.IElementType tt = beforeParen.getNode().getElementType();
        if (tt == CompactTokenTypes.IF || tt == CompactTokenTypes.ASSERT) {
          return CompactPrimitiveType.BOOLEAN;
        }
      }
    }

    return null;
  }

  private static boolean isReturnContext(@NotNull PsiElement position) {
    PsiElement prev = PsiTreeUtil.prevVisibleLeaf(position);
    if (prev != null && prev.getNode() != null && prev.getNode().getElementType() == CompactTokenTypes.RETURN) {
      return true;
    }
    PsiElement returnStmt = PsiTreeUtil.findFirstParent(position, false,
        p -> p.getNode() != null && p.getNode().getElementType() == dev.verloren.midnight.parser.CompactElementTypes.RETURN_STATEMENT);
    if (returnStmt != null) {
      return true;
    }
    for (PsiElement p = prev; p != null; p = PsiTreeUtil.prevVisibleLeaf(p)) {
      if (p.getNode() == null) break;
      com.intellij.psi.tree.IElementType tt = p.getNode().getElementType();
      if (tt == CompactTokenTypes.RETURN) {
        return true;
      }
      if (tt == CompactTokenTypes.SEMICOLON || tt == CompactTokenTypes.LBRACE || tt == CompactTokenTypes.RBRACE) {
        break;
      }
    }
    return false;
  }

  public static @NotNull CompactType getCandidateType(@NotNull CompactNamedElement element) {
    if (element instanceof CompactImportElementImpl importElem) {
      CompactNamedElement resolved = CompactResolveUtil.resolveImportElementSource(importElem);
      if (resolved != null) {
        return getCandidateType(resolved);
      }
    }
    CompactType callableRt = CompactPsiUtil.getCallableReturnType(element);
    if (!CompactPrimitiveType.UNKNOWN.equals(callableRt)) {
      return callableRt;
    }
    return switch (element) {
      case CompactParameterImpl param -> param.getType();
      case CompactConstBindingImpl constBinding -> constBinding.getType();
      case CompactPatternImpl pattern -> pattern.getType();
      case CompactStructFieldImpl field -> field.getType();
      case CompactEnumMemberImpl member -> member.getType();
      case CompactEnumDefinition enumDef ->
          new CompactPrimitiveType(enumDef.getName() != null ? enumDef.getName() : "Enum");
      default -> element.getType();
    };
  }

  public static boolean isTypeCompatible(@NotNull CompactType candidateType, @Nullable CompactType expectedType) {
    if (expectedType == null || CompactPrimitiveType.UNKNOWN.equals(expectedType)) {
      return true;
    }
    if (CompactPrimitiveType.UNKNOWN.equals(candidateType)) {
      return true;
    }

    String expectedName = expectedType.name();
    String candidateName = candidateType.name();

    // Void handling
    if ("Void".equalsIgnoreCase(expectedName)) {
      return "Void".equalsIgnoreCase(candidateName);
    }
    if ("Void".equalsIgnoreCase(candidateName)) {
      return false;
    }

    if (candidateType.isAssignableTo(expectedType) || expectedType.isAssignableTo(candidateType)) {
      return true;
    }

    if (expectedName.equalsIgnoreCase(candidateName)) {
      return true;
    }

    // Number literals / Field interoperability
    if ("Field".equalsIgnoreCase(expectedName) && ("Field".equalsIgnoreCase(candidateName) || "Uint".equalsIgnoreCase(candidateName))) {
      return true;
    }

    // Prefix matching for parameterized types (e.g., Uint<64> matches Uint)
    return (expectedName.startsWith("Uint") && candidateName.startsWith("Uint"))
        || (expectedName.startsWith("Vector") && candidateName.startsWith("Vector"));
  }

  private static void addNamed(@NotNull CompletionResultSet result, @NotNull CompactNamedElement element) {
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
    result.addElement(builder);
  }
}
