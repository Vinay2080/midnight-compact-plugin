package dev.verloren.midnight.inspection.fix;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.CompactFileType;
import dev.verloren.midnight.parser.CompactElementTypes;
import dev.verloren.midnight.psi.CompactFile;
import org.jetbrains.annotations.NotNull;

/**
 * Quick-fix to wrap an undisclosed witness or private expression with {@code disclose(...)}.
 */
public class CompactWrapWithDiscloseFix implements LocalQuickFix {

  @Override
  public @NotNull String getName() {
    return "Wrap with disclose(...)";
  }

  @Override
  public @NotNull String getFamilyName() {
    return "Wrap with disclose";
  }

  @Override
  public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
    PsiElement element = descriptor.getPsiElement();
    if (element == null) {
      return;
    }
    String text = element.getText();
    String wrappedCode = "circuit _dummy(): [] { disclose(" + text + "); }";
    CompactFile dummyFile = (CompactFile) PsiFileFactory.getInstance(project)
        .createFileFromText("dummy.compact", CompactFileType.INSTANCE, wrappedCode);
    for (PsiElement child : PsiTreeUtil.findChildrenOfType(dummyFile, PsiElement.class)) {
      if (child.getNode() != null && child.getNode().getElementType() == CompactElementTypes.DISCLOSE_EXPR) {
        element.replace(child);
        return;
      }
    }
  }
}
