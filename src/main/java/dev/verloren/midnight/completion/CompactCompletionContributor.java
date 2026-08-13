package dev.verloren.midnight.completion;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import dev.verloren.midnight.CompactLanguage;
import dev.verloren.midnight.psi.CompactEnumDefinition;
import dev.verloren.midnight.psi.CompactEnumMemberImpl;
import dev.verloren.midnight.psi.CompactMemberExprImpl;
import dev.verloren.midnight.psi.CompactNamedElement;
import dev.verloren.midnight.reference.CompactEnumMemberReference;
import dev.verloren.midnight.resolve.CompactResolveUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

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
        addEnumMembers(position, result);
        break;
      case VALUE:
      default:
        addNamed(result, CompactResolveUtil.collectValueDeclarations(position));
        addPrefixed(result, CompactResolveUtil.prefixedImportNames(position, CompactResolveUtil.Namespace.VALUE));
        addAll(result, VALUE_KEYWORDS);
        break;
    }
  }

  private static void addAll(@NotNull CompletionResultSet result, String @NotNull [] values) {
    for (String value : values) {
      result.addElement(LookupElementBuilder.create(value));
    }
  }

  private static void addNamed(@NotNull CompletionResultSet result, @NotNull Collection<? extends CompactNamedElement> elements) {
    for (CompactNamedElement element : elements) {
      addNamed(result, element);
    }
  }

  private static void addPrefixed(@NotNull CompletionResultSet result, @NotNull Collection<String> values) {
    for (String value : values) {
      result.addElement(LookupElementBuilder.create(value));
    }
  }

  private static void addEnumMembers(@NotNull PsiElement position, @NotNull CompletionResultSet result) {
    CompactMemberExprImpl memberExpr = PsiTreeUtil.getParentOfType(position, CompactMemberExprImpl.class, false);
    if (memberExpr == null || !(memberExpr.getReference() instanceof CompactEnumMemberReference)) {
      return;
    }
    ResolveResult[] resolveResults = ((CompactEnumMemberReference) memberExpr.getReference()).multiResolve(false);
    for (ResolveResult resolveResult : resolveResults) {
      if (resolveResult.getElement() instanceof CompactEnumMemberImpl) {
        addNamed(result, (CompactNamedElement) resolveResult.getElement());
      }
    }
    PsiElement base = memberExpr.getFirstChild();
    if (base == null) {
      return;
    }
    for (CompactNamedElement target : CompactResolveUtil.resolveType(base.getText(), memberExpr)) {
      if (target instanceof CompactEnumDefinition) {
        addNamed(result, PsiTreeUtil.findChildrenOfType(target, CompactEnumMemberImpl.class));
      }
    }
  }

  private static void addNamed(@NotNull CompletionResultSet result, @NotNull CompactNamedElement element) {
    String name = element.getName();
    if (name != null) {
      result.addElement(LookupElementBuilder.create(name));
    }
  }
}