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
 * Completion provider for Compact member access (struct fields, enum members, and dot-expressions).
 */
public final class CompactMemberAccessCompletionProvider {

  private CompactMemberAccessCompletionProvider() {}

  public static void addMemberCompletions(@NotNull PsiElement position, @NotNull CompletionResultSet result) {
    CompactMemberExprImpl memberExpr = PsiTreeUtil.getParentOfType(position, CompactMemberExprImpl.class, false);
    if (memberExpr != null) {
      addMemberCompletionsFromMemberExpr(memberExpr, result);
      return;
    }
    addMemberCompletionsFromDotFallback(position, result);
  }

  private static void addMemberCompletionsFromMemberExpr(
      @NotNull CompactMemberExprImpl memberExpr,
      @NotNull CompletionResultSet result
  ) {
    switch (Objects.requireNonNull(memberExpr.getReference())) {
      case CompactEnumMemberReference enumRef -> addResolvedNamed(result, enumRef.multiResolve(false));
      case CompactStructFieldReference structRef -> addResolvedNamed(result, structRef.multiResolve(false));
      default -> {}
    }

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

  public static void addMembersFromBaseText(
      @NotNull String baseText,
      @NotNull PsiElement context,
      @NotNull CompletionResultSet result
  ) {
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
    addMembersFromTypeName(baseText, context, result);
  }

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

  public static void addResolvedNamed(@NotNull CompletionResultSet result, ResolveResult @NotNull [] resolveResults) {
    for (ResolveResult resolveResult : resolveResults) {
      if (resolveResult.getElement() instanceof CompactNamedElement named) {
        addNamed(result, named, 60.0);
      }
    }
  }

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
