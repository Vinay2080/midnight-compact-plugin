package dev.verloren.midnight.completion;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import dev.verloren.midnight.CompactLanguage;
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
import java.util.Set;

/**
 * Code completion provider for Compact smart contracts.
 *
 * <p>Extends {@link CompletionContributor} and classifies the caret context via
 * {@link CompactCompletionContext#classify(PsiElement)} into keywords, types, values,
 * or members, populating the {@link CompletionResultSet} with contextually valid lookup items.</p>
 */
public class CompactCompletionContributor extends CompletionContributor {
  private static final String[] DECLARATION_KEYWORDS = {
      "pragma", "include", "import", "export", "module", "contract", "struct", "enum", "type", "ledger", "witness", "constructor", "circuit"
  };
  private static final String[] STATEMENT_KEYWORDS = {
      "const", "if", "for", "return", "assert", "emit"
  };
  private static final String[] VALUE_KEYWORDS = {
      "true", "false", "default", "disclose", "map", "fold", "pad", "slice", "assert", "emit"
  };
  private static final String[] BUILTIN_TYPES = {
      "Boolean", "Bytes", "Field", "Opaque", "Uint", "Vector", "JubjubScalar", "Secp256k1Base", "Secp256k1Scalar"
  };

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
      case KEYWORD:
        addAll(result, DECLARATION_KEYWORDS);
        addAll(result, STATEMENT_KEYWORDS);
        break;
      case TYPE:
        addAll(result, BUILTIN_TYPES);
        addNamed(result, CompactResolveUtil.collectTypeDeclarations(position));
        addPrefixed(result, CompactResolveUtil.prefixedImportNames(position, CompactResolveUtil.Namespace.TYPE));
        break;
      case MEMBER:
        addMemberCompletions(position, result);
        break;
      case VALUE:
      default:
        addValueCompletions(position, result);
        break;
    }
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

  private static void addMemberCompletions(@NotNull PsiElement position, @NotNull CompletionResultSet result) {
    CompactMemberExprImpl memberExpr = PsiTreeUtil.getParentOfType(position, CompactMemberExprImpl.class, false);

    if (memberExpr != null) {
      // 1. Check existing references (Enum / Struct field)
      if (memberExpr.getReference() instanceof CompactEnumMemberReference enumRef) {
        for (ResolveResult resolveResult : enumRef.multiResolve(false)) {
          if (resolveResult.getElement() instanceof CompactNamedElement named) {
            addNamed(result, named);
          }
        }
      } else if (memberExpr.getReference() instanceof CompactStructFieldReference structRef) {
        for (ResolveResult resolveResult : structRef.multiResolve(false)) {
          if (resolveResult.getElement() instanceof CompactNamedElement named) {
            addNamed(result, named);
          }
        }
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
      if (target instanceof CompactImportElementImpl importElem) {
        target = CompactResolveUtil.resolveImportElementSource(importElem);
      }
      if (target instanceof CompactStructDefinition structDef) {
        addNamed(result, structDef.getFields());
      }
      if (target instanceof CompactEnumDefinition enumDef) {
        addNamed(result, enumDef.getMembers());
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
      if (!"Void".equalsIgnoreCase(expectedType.name()) && !"void".equalsIgnoreCase(expectedType.name())) {
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

    // 3. Check if in is (<caret>) or assert(<caret>) condition context
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
      default -> (element).getType();
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
    if ("Void".equalsIgnoreCase(expectedName) || "void".equalsIgnoreCase(expectedName)) {
      return "Void".equalsIgnoreCase(candidateName) || "void".equalsIgnoreCase(candidateName);
    }
    if ("Void".equalsIgnoreCase(candidateName) || "void".equalsIgnoreCase(candidateName)) {
      return false;
    }

    // Direct equality
    if (expectedName.equals(candidateName)) {
      return true;
    }

    // Assignability method on candidate type
    if (candidateType.isAssignableTo(expectedType)) {
      return true;
    }

    // Numeric literal assignability
    if (candidateType instanceof dev.verloren.midnight.type.CompactNumericLiteralType) {
      return candidateType.isAssignableTo(expectedType);
    }

    // Uint bit-width subtyping (e.g., Uint <8> -> Uint <32>)
    dev.verloren.midnight.type.CompactUintType candidateUint = dev.verloren.midnight.type.CompactUintType.parse(candidateName);
    dev.verloren.midnight.type.CompactUintType expectedUint = dev.verloren.midnight.type.CompactUintType.parse(expectedName);
    if (candidateUint != null && expectedUint != null) {
      return candidateUint.isAssignableTo(expectedUint);
    }

    return false;
  }

  private static void addNamed(@NotNull CompletionResultSet result, @NotNull CompactNamedElement element) {
    String name = element.getName();
    if (name != null) {
      LookupElementBuilder builder = LookupElementBuilder.create(name);
      String typeName = element.getType().name();
      if (!"Unknown".equalsIgnoreCase(typeName) && !name.equals(typeName) && !"struct".equalsIgnoreCase(typeName) && !"enum".equalsIgnoreCase(typeName)) {
        builder = builder.withTypeText(typeName);
      }
      result.addElement(builder);
    }
  }
}
