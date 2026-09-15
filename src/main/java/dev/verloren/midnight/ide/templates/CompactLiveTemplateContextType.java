package dev.verloren.midnight.ide.templates;

import com.intellij.codeInsight.template.TemplateActionContext;
import com.intellij.codeInsight.template.TemplateContextType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiUtilCore;
import dev.verloren.midnight.CompactLanguage;
import dev.verloren.midnight.completion.CompactCompletionContext;
import dev.verloren.midnight.psi.CompactFile;
import org.jetbrains.annotations.NotNull;

/**
 * Defines the execution context for Compact live templates.
 *
 * <p>Suppresses live template expansions inside comments and documentation blocks.</p>
 */
public class CompactLiveTemplateContextType extends TemplateContextType {

  public CompactLiveTemplateContextType() {
    super("Compact");
  }

  @Override
  public boolean isInContext(@NotNull TemplateActionContext templateActionContext) {
    PsiFile file = templateActionContext.getFile();
    if (!(file instanceof CompactFile || file.getLanguage().isKindOf(CompactLanguage.INSTANCE))) {
      return false;
    }
    int offset = templateActionContext.getStartOffset();
    if (!PsiUtilCore.getLanguageAtOffset(file, offset).isKindOf(CompactLanguage.INSTANCE)) {
      return false;
    }
    PsiElement element = file.findElementAt(offset);
    if (element == null && offset > 0) {
      element = file.findElementAt(offset - 1);
    }
    return element == null || !CompactCompletionContext.isComment(element);
  }
}
