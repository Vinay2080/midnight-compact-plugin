package dev.verloren.midnight.intention;

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import dev.verloren.midnight.psi.CompactPragmaForm;
import dev.verloren.midnight.run.CompactToolchainUtil;
import dev.verloren.midnight.version.CompactSemVerUtil;
import dev.verloren.midnight.version.CompactVersionManager;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

/**
 * Context action to switch or download the required Compact compiler version based on pragma.
 */
public class CompactSwitchCompilerVersionIntention extends PsiElementBaseIntentionAction {

  @Override
  public @NotNull @Nls(capitalization = Nls.Capitalization.Sentence) String getFamilyName() {
    return "Compact compiler version";
  }

  @Override
  public boolean startInWriteAction() {
    return false;
  }

  @Override
  public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
    CompactPragmaForm pragma = PsiTreeUtil.getParentOfType(element, CompactPragmaForm.class, false);
    if (pragma == null) {
      return false;
    }

    String reqVer = pragma.getRequiredVersion();
    if (reqVer == null || reqVer.isEmpty()) {
      return false;
    }

    // Check if the currently active compiler already satisfies pragma
    String activeVer = CompactToolchainUtil.getActiveCompilerVersion(project);
    if (activeVer != null) {
      String constraint = pragma.getConstraintText();
      if (constraint == null || constraint.isEmpty()) {
        constraint = reqVer;
      }
      String langVer = CompactVersionManager.getLanguageVersionForToolchain(activeVer);
      if (CompactSemVerUtil.satisfiesConstraint(langVer, constraint)) {
        return false;
      }
    }

    String toolchainVer = CompactVersionManager.resolveToolchainVersionForLanguage(reqVer);
    if (CompactVersionManager.isVersionInstalled(toolchainVer)) {
      if (toolchainVer.equals(reqVer)) {
        setText("Switch project compiler to Compact " + reqVer);
      } else {
        setText("Switch project compiler to Compact " + reqVer + " (v" + toolchainVer + ")");
      }
    } else {
      if (toolchainVer.equals(reqVer)) {
        setText("Download and use Compact " + reqVer);
      } else {
        setText("Download and use Compact " + reqVer + " (v" + toolchainVer + ")");
      }
    }
    return true;
  }

  @Override
  public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element) throws IncorrectOperationException {
    CompactPragmaForm pragma = PsiTreeUtil.getParentOfType(element, CompactPragmaForm.class, false);
    if (pragma == null) {
      return;
    }
    String reqVer = pragma.getRequiredVersion();
    if (reqVer == null || reqVer.isEmpty()) {
      return;
    }

    String toolchainVer = CompactVersionManager.resolveToolchainVersionForLanguage(reqVer);
    VirtualFile vFile = element.getContainingFile() != null ? element.getContainingFile().getVirtualFile() : null;
    CompactVersionManager.ensureAndSwitchVersion(project, toolchainVer, vFile);
  }
}
