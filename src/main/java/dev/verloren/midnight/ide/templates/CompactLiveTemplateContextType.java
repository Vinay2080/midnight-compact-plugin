package dev.verloren.midnight.ide.templates;

import com.intellij.codeInsight.template.TemplateActionContext;
import com.intellij.codeInsight.template.TemplateContextType;
import com.intellij.psi.PsiFile;
import dev.verloren.midnight.CompactLanguage;
import dev.verloren.midnight.psi.CompactFile;
import org.jetbrains.annotations.NotNull;

/**
 * Defines the execution context for Compact live templates.
 */
public class CompactLiveTemplateContextType extends TemplateContextType {
  @SuppressWarnings("deprecation")
  public CompactLiveTemplateContextType() {
    super("COMPACT_CODE", "Compact");
  }

  @Override
  public boolean isInContext(@NotNull TemplateActionContext templateActionContext) {
    PsiFile file = templateActionContext.getFile();
    return file instanceof CompactFile || file.getLanguage().isKindOf(CompactLanguage.INSTANCE);
  }
}
