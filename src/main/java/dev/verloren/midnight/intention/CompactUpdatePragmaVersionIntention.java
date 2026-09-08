package dev.verloren.midnight.intention;

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import dev.verloren.midnight.annotator.CompactProblemUtil;
import dev.verloren.midnight.psi.CompactPragmaForm;
import dev.verloren.midnight.run.CompactToolchainUtil;
import dev.verloren.midnight.version.CompactSemVerUtil;
import dev.verloren.midnight.version.CompactVersionManager;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

/**
 * Context action to update the contract's pragma statement to match the currently active compiler version.
 */
public class CompactUpdatePragmaVersionIntention extends PsiElementBaseIntentionAction {

  @Override
  public @NotNull @Nls(capitalization = Nls.Capitalization.Sentence) String getFamilyName() {
    return "Compact pragma version";
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

    String activeVer = CompactToolchainUtil.getActiveCompilerVersion(project);
    if (activeVer == null || activeVer.isEmpty()) {
      return false;
    }

    PsiElement id = pragma.getPragmaIdentifier();
    String idText = id != null ? id.getText() : "language_version";
    boolean isCompilerPragma = "compiler_version".equals(idText);

    String targetVer = isCompilerPragma ? activeVer : CompactVersionManager.getLanguageVersionForToolchain(activeVer);
    String constraint = pragma.getConstraintText();
    if (constraint == null || constraint.isEmpty()) {
      constraint = pragma.getRequiredVersion();
    }
    if (constraint != null) {
      if (CompactSemVerUtil.satisfiesConstraint(targetVer, constraint)) {
        return false;
      }
    }

    setText("Update pragma to match active compiler (>= " + targetVer + ")");
    return true;
  }

  @Override
  public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element) throws IncorrectOperationException {
    CompactPragmaForm pragma = PsiTreeUtil.getParentOfType(element, CompactPragmaForm.class, false);
    if (pragma == null) {
      return;
    }
    String activeVer = CompactToolchainUtil.getActiveCompilerVersion(project);
    if (activeVer == null || activeVer.isEmpty()) {
      return;
    }

    PsiElement id = pragma.getPragmaIdentifier();
    String idText = id != null ? id.getText() : "language_version";
    boolean isCompilerPragma = "compiler_version".equals(idText);

    String targetVer = isCompilerPragma ? activeVer : CompactVersionManager.getLanguageVersionForToolchain(activeVer);
    Document document = editor.getDocument();
    String newPragma = "pragma " + idText + " >= " + targetVer + ";";
    WriteCommandAction.runWriteCommandAction(project, () -> {
      document.replaceString(pragma.getTextRange().getStartOffset(), pragma.getTextRange().getEndOffset(), newPragma);
      PsiDocumentManager.getInstance(project).commitDocument(document);
    });

    VirtualFile vFile = element.getContainingFile() != null ? element.getContainingFile().getVirtualFile() : null;
    CompactProblemUtil.clearProblemsAndRestart(project, vFile);
  }
}
