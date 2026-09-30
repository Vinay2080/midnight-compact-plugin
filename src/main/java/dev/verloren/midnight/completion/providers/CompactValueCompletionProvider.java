package dev.verloren.midnight.completion.providers;

import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.PrioritizedLookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.completion.CompactParameterizedTypeInsertHandler;
import dev.verloren.midnight.intention.CompactSpecifyTypeExplicitlyIntention;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import dev.verloren.midnight.parser.CompactElementTypes;
import dev.verloren.midnight.psi.CompactCircuitDefinition;
import dev.verloren.midnight.psi.CompactConstBindingImpl;
import dev.verloren.midnight.psi.CompactConstructorDeclaration;
import dev.verloren.midnight.psi.CompactEnumDefinition;
import dev.verloren.midnight.psi.CompactEnumMemberImpl;
import dev.verloren.midnight.psi.CompactImportElementImpl;
import dev.verloren.midnight.psi.CompactNamedElement;
import dev.verloren.midnight.psi.CompactParameterImpl;
import dev.verloren.midnight.psi.CompactPatternImpl;
import dev.verloren.midnight.psi.CompactPsiUtil;
import dev.verloren.midnight.psi.CompactStructFieldImpl;
import dev.verloren.midnight.psi.CompactTypeElement;
import dev.verloren.midnight.psi.CompactWitnessDeclaration;
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
 */
public final class CompactValueCompletionProvider {

  private CompactValueCompletionProvider() {}

  public static void addValueCompletions(@NotNull PsiElement position, @NotNull CompletionResultSet result) {
    CompactType expectedType = getExpectedType(position);
    if (expectedType != null && !CompactPrimitiveType.UNKNOWN.equals(expectedType)) {
      addRestrictedValueCompletions(position, expectedType, result);
    } else {
      addUnrestrictedValueCompletions(position, result);
    }
  }

  private static void addRestrictedValueCompletions(
      @NotNull PsiElement position,
      @NotNull CompactType expectedType,
      @NotNull CompletionResultSet result
  ) {
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

    if (isTypeCompatible(CompactPrimitiveType.BOOLEAN, expectedType)) {
      result.addElement(PrioritizedLookupElement.withPriority(
          LookupElementBuilder.create("true").withTypeText("Boolean").bold(), 100.0));
      result.addElement(PrioritizedLookupElement.withPriority(
          LookupElementBuilder.create("false").withTypeText("Boolean").bold(), 100.0));
    }
    if (!"Void".equalsIgnoreCase(expectedType.name())) {
      addDefaultCompletionsForExpected(expectedType, result);
    }

    boolean isEitherExpected = expectedType.name().startsWith("Either");
    CompactStructLiteralCompletionProvider.addEitherAndHelperCompletions(
        result, isEitherExpected ? expectedType : null, isEitherExpected ? 115.0 : 30.0);

    addPrefixed(result, CompactResolveUtil.prefixedImportNames(position, CompactResolveUtil.Namespace.VALUE));
    addValueKeywordsExceptSpecial(result);
    result.addElement(CompactKeywordCompletionProvider.createAssertLookupElement());
  }

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

  private static void addValueKeywordsExceptSpecial(@NotNull CompletionResultSet result) {
    for (String keyword : CompactKeywordCompletionProvider.VALUE_KEYWORDS) {
      if ("true".equals(keyword) || "false".equals(keyword) || "default".equals(keyword) || "disclose".equals(keyword)) {
        continue;
      }
      result.addElement(PrioritizedLookupElement.withPriority(LookupElementBuilder.create(keyword), 10.0));
    }
  }

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

  public static void addPrefixed(@NotNull CompletionResultSet result, @NotNull Collection<String> values) {
    for (String value : values) {
      result.addElement(LookupElementBuilder.create(value));
    }
  }

  public static @Nullable CompactType getExpectedType(@NotNull PsiElement position) {
    PsiElement prev = PsiTreeUtil.prevVisibleLeaf(position);
    if (prev != null && prev.getNode() != null && prev.getNode().getElementType() == CompactTokenTypes.COLON) {
      PsiElement idBeforeColon = PsiTreeUtil.prevVisibleLeaf(prev);
      if (idBeforeColon != null && "is_left".equals(idBeforeColon.getText())) {
        return CompactPrimitiveType.BOOLEAN;
      }
    }
    if (prev != null && prev.getNode() != null && prev.getNode().getElementType() == CompactTokenTypes.LPAREN) {
      PsiElement beforeParen = PsiTreeUtil.prevVisibleLeaf(prev);
      if (beforeParen != null && beforeParen.getNode() != null) {
        IElementType tt = beforeParen.getNode().getElementType();
        if (tt == CompactTokenTypes.IF || tt == CompactTokenTypes.ASSERT) {
          return CompactPrimitiveType.BOOLEAN;
        }
      }
    }
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
    CompactConstBindingImpl binding = PsiTreeUtil.getParentOfType(position, CompactConstBindingImpl.class);
    if (binding != null) {
      CompactTypeElement typeElem = CompactSpecifyTypeExplicitlyIntention.getDeclaredTypeElement(binding);
      if (typeElem != null) {
        return typeElem.getType();
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
        p -> p.getNode() != null && p.getNode().getElementType() == CompactElementTypes.RETURN_STATEMENT);
    if (returnStmt != null) {
      return true;
    }
    for (PsiElement p = prev; p != null; p = PsiTreeUtil.prevVisibleLeaf(p)) {
      if (p.getNode() == null) break;
      IElementType tt = p.getNode().getElementType();
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
    if ("Field".equalsIgnoreCase(expectedName) && ("Field".equalsIgnoreCase(candidateName) || "Uint".equalsIgnoreCase(candidateName))) {
      return true;
    }
    return (expectedName.startsWith("Uint") && candidateName.startsWith("Uint"))
        || (expectedName.startsWith("Vector") && candidateName.startsWith("Vector"));
  }
}
