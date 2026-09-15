package dev.verloren.midnight.ide.templates;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.codeInsight.template.Expression;
import com.intellij.codeInsight.template.ExpressionContext;
import com.intellij.codeInsight.template.Result;
import com.intellij.codeInsight.template.TextResult;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import dev.verloren.midnight.completion.CompactCompletionContributor;
import dev.verloren.midnight.psi.CompactNamedElement;
import dev.verloren.midnight.resolve.CompactResolveUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Live template expression providing data type completions (built-in primitive types,
 * in-scope user-defined types, and imported types) for live template variables.
 */
public class CompactTypeExpression extends Expression {
  private final String defaultType;

  public CompactTypeExpression(@NotNull String defaultType) {
    this.defaultType = defaultType;
  }

  @Override
  public @NotNull Result calculateResult(ExpressionContext context) {
    return new TextResult(defaultType);
  }

  @Override
  public @NotNull Result calculateQuickResult(ExpressionContext context) {
    return new TextResult(defaultType);
  }

  @Override
  public LookupElement @Nullable [] calculateLookupItems(ExpressionContext context) {
    Project project = context.getProject();
    if (context.getEditor() != null) {
      PsiDocumentManager.getInstance(project).commitDocument(context.getEditor().getDocument());
    }
    PsiElement element = context.getPsiElementAtStartOffset();

    Set<String> seen = new LinkedHashSet<>();
    List<LookupElement> items = new ArrayList<>();

    // 1. Initial default item (e.g. State, Void, Field)
    if (!defaultType.isEmpty() && seen.add(defaultType)) {
      items.add(LookupElementBuilder.create(defaultType).bold());
    }

    // 2. Built-in types
    for (String builtin : CompactCompletionContributor.BUILTIN_TYPES) {
      if (seen.add(builtin)) {
        items.add(LookupElementBuilder.create(builtin).bold());
      }
    }

    // 3. In-scope user-defined types (structs, enums, type aliases)
    if (element != null) {
      for (CompactNamedElement decl : CompactResolveUtil.collectTypeDeclarations(element)) {
        String name = decl.getName();
        if (name != null && !name.isEmpty() && seen.add(name)) {
          items.add(LookupElementBuilder.create(name).withTypeText("type"));
        }
      }
      for (String imported : CompactResolveUtil.prefixedImportNames(element, CompactResolveUtil.Namespace.TYPE)) {
        if (seen.add(imported)) {
          items.add(LookupElementBuilder.create(imported).withTypeText("import"));
        }
      }
    }

    return items.toArray(LookupElement.EMPTY_ARRAY);
  }
}
