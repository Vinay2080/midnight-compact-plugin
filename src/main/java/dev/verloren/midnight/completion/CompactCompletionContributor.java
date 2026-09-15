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
    extend(
        CompletionType.BASIC,
        PlatformPatterns.psiElement()
            .withLanguage(CompactLanguage.INSTANCE)
            .andNot(PlatformPatterns.psiComment())\
            .andNot(PlatformPatterns.psiElement().inside(PlatformPatterns.psiComment())),
        new CompletionProvider<>() {
          @Override
          protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet result) {
            addCompactCompletions(parameters.getPosition(), result);
          }
        });
  }

  private static void addCompactCompletions(@NotNull PsiElement position, @NotNull CompletionResultSet result) {
    if (CompactCompletionContext.isComment(position)) {
      return;
    }

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
    registerAfterExportTemplate(result, CompactDeclarationType.LEDGER, 100.0, CompactLedgerInsertHandler.INSTANCE);
  }

  private static void addAfterPureCompletions(@NotNull CompletionResultSet result) {
    registerAfterExportTemplate(result, CompactDeclarationType.CIRCUIT, 100.0, new CompactDeclarationInsertHandler(CompactDeclarationType.CIRCUIT));
  }

  private static void addAfterNewCompletions(@NotNull CompletionResultSet result) {
    registerAfterExportTemplate(result, CompactDeclarationType.TYPE, 100.0, new CompactDeclarationInsertHandler(CompactDeclarationType.TYPE));
  }

  private static void addValueCompletions(@NotNull PsiElement position, @NotNull CompletionResultSet result) {
    addAll(result, VALUE_KEYWORDS);
    addNamed(result, CompactResolveUtil.collectValueDeclarations(position));
    addPrefixed(result, CompactResolveUtil.prefixedImportNames(position, CompactResolveUtil.Namespace.VALUE));
    addSmartReturnCompletions(position, result);
  }

  private static void addSmartReturnCompletions(@NotNull PsiElement position, @NotNull CompletionResultSet result) {
    CompactCircuitDefinition circuit = PsiTreeUtil.getParentOfType(position, CompactCircuitDefinition.class, false);
    if (circuit == null) {
      return;
    }
    PsiElement prevLeaf = CompactCompletionContext.prevNonCommentLeaf(position);
    if (prevLeaf == null || prevLeaf.getNode() == null || prevLeaf.getNode().getElementType() != CompactTokenTypes.RETURN) {
      return;
    }
    CompactType returnType = circuit.getReturnType();
    if (returnType == null) {
      return;
    }

    for (CompactNamedElement decl : CompactResolveUtil.collectValueDeclarations(position)) {
      String name = decl.getName();
      if (name == null) {
        continue;
      }
      CompactType declType = CompactType.from(decl);
      if (declType != null && declType.isAssignableFrom(returnType)) {
        result.addElement(PrioritizedLookupElement.withPriority(
            LookupElementBuilder.create(decl)
                .withIcon(decl.getIcon(0))
                .withTypeText(declType.getPresentableText())
                .bold(),
            120.0
        ));
      }
    }

    if (returnType instanceof CompactPrimitiveType primitive && primitive.getPrimitiveKind() == CompactPrimitiveType.Kind.BOOLEAN) {
      result.addElement(PrioritizedLookupElement.withPriority(LookupElementBuilder.create("true").bold(), 110.0));
      result.addElement(PrioritizedLookupElement.withPriority(LookupElementBuilder.create("false").bold(), 110.0));
    }
  }

  private static void addMemberCompletions(@NotNull PsiElement position, @NotNull CompletionResultSet result) {
    PsiElement dot = CompactCompletionContext.prevNonCommentLeaf(position);
    if (dot == null) {
      return;
    }
    PsiElement qualifier = CompactCompletionContext.prevNonCommentLeaf(dot);
    if (qualifier == null) {
      return;
    }

    if (qualifier.getNode() != null && qualifier.getNode().getElementType() == CompactTokenTypes.IDENTIFIER) {
      String name = qualifier.getText();
      for (CompactNamedElement typeDecl : CompactResolveUtil.collectTypeDeclarations(position)) {
        if (name.equals(typeDecl.getName()) && typeDecl instanceof CompactEnumDefinition enumDef) {
          for (CompactEnumMember member : enumDef.getMembers()) {
            String memberName = member.getName();
            if (memberName != null) {
              result.addElement(LookupElementBuilder.create(member)
                  .withIcon(member.getIcon(0))
                  .withTypeText(name));
            }
          }
          return;
        }
      }
    }

    CompactType qualifierType = CompactType.from(qualifier);
    if (qualifierType != null) {
      addTypeMemberCompletions(qualifierType, result);
      return;
    }

    if (qualifier.getNode() != null && qualifier.getNode().getElementType() == CompactTokenTypes.IDENTIFIER) {
      String name = qualifier.getText();
      for (CompactNamedElement valDecl : CompactResolveUtil.collectValueDeclarations(position)) {
        if (name.equals(valDecl.getName())) {
          CompactType valType = CompactType.from(valDecl);
          if (valType != null) {
            addTypeMemberCompletions(valType, result);
            return;
          }
        }
      }
    }

    for (PsiElement parent = position.getParent(); parent != null; parent = parent.getParent()) {
      if (parent instanceof CompactMemberExpr) {
        for (CompactNamedElement elem : resolveMembersForExpression((CompactMemberExpr) parent)) {
          String elemName = elem.getName();
          if (elemName != null) {
            result.addElement(LookupElementBuilder.create(elem).withIcon(elem.getIcon(0)));
          }
        }
        return;
      }
    }
  }

  private static void addTypeMemberCompletions(@NotNull CompactType type, @NotNull CompletionResultSet result) {
    for (CompactNamedElement member : type.getMembers()) {
      String memberName = member.getName();
      if (memberName != null) {
        LookupElementBuilder builder = LookupElementBuilder.create(member).withIcon(member.getIcon(0));
        CompactType memberType = CompactType.from(member);
        if (memberType != null) {
          builder = builder.withTypeText(memberType.getPresentableText());
        }
        result.addElement(builder);
      }
    }
  }

  private static Collection<CompactNamedElement> resolveMembersForExpression(CompactMemberExpr memberExpr) {
    Set<CompactNamedElement> members = new HashSet<>();
    for (CompactStructFieldReference ref : PsiTreeUtil.findChildrenOfType(memberExpr, CompactStructFieldReference.class)) {
      for (ResolveResult resolveResult : ref.multiResolve(false)) {
        PsiElement el = resolveResult.getElement();
        if (el instanceof CompactNamedElement) {
          members.add((CompactNamedElement) el);
        }
      }
    }
    for (CompactEnumMemberReference ref : PsiTreeUtil.findChildrenOfType(memberExpr, CompactEnumMemberReference.class)) {
      for (ResolveResult resolveResult : ref.multiResolve(false)) {
        PsiElement el = resolveResult.getElement();
        if (el instanceof CompactNamedElement) {
          members.add((CompactNamedElement) el);
        }
      }
    }
    return members;
  }

  private static void addAll(CompletionResultSet result, String[] items) {
    for (String item : items) {
      result.addElement(LookupElementBuilder.create(item).bold());
    }
  }

  private static void addNamed(CompletionResultSet result, Collection<? extends CompactNamedElement> elements) {
    for (CompactNamedElement element : elements) {
      String name = element.getName();
      if (name != null) {
        result.addElement(LookupElementBuilder.create(element).withIcon(element.getIcon(0)));
      }
    }
  }

  private static void addPrefixed(CompletionResultSet result, Collection<String> names) {
    for (String name : names) {
      result.addElement(LookupElementBuilder.create(name).bold());
    }
  }
}
