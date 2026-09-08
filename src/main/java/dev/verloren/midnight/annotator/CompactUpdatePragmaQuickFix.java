package dev.verloren.midnight.annotator;

import com.intellij.codeInsight.intention.impl.BaseIntentionAction;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import dev.verloren.midnight.psi.CompactPragmaForm;
import dev.verloren.midnight.version.CompactVersionManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Quick-fix to update the pragma expression to match the active compiler.
 */
public class CompactUpdatePragmaQuickFix extends BaseIntentionAction implements LocalQuickFix {
  private final String activeVersion;

  public CompactUpdatePragmaQuickFix(@NotNull String activeVersion) {
    this.activeVersion = activeVersion;
  }

  @Override
  public @NotNull String getText() {
    String langVer = CompactVersionManager.getLanguageVersionForToolchain(activeVersion);
    return "Update pragma to match active compiler (>= " + langVer + ")";
  }

  @Override
  public @NotNull String getName() {
    return getText();
  }

  @Override
  public @NotNull String getFamilyName() {
    return "Compact pragma version";
  }

  @Override
  public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
    return true;
  }

  @Override
  public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
    PsiFile file = descriptor.getPsiElement() != null ? descriptor.getPsiElement().getContainingFile() : null;
    CompactPragmaForm pragma = descriptor.getPsiElement() != null
        ? PsiTreeUtil.getParentOfType(descriptor.getPsiElement(), CompactPragmaForm.class, false)
        : null;
    invokeWithPragma(project, file, pragma);
  }

  @Override
  public void invoke(@NotNull Project project, @Nullable Editor editor, @Nullable PsiFile file) throws IncorrectOperationException {
    invokeWithPragma(project, file, null);
  }

  private void invokeWithPragma(@NotNull Project project, @Nullable PsiFile file, @Nullable CompactPragmaForm explicitPragma) {
    if (file == null) {
      return;
    }
    CompactPragmaForm pragma = explicitPragma != null
        ? explicitPragma
        : PsiTreeUtil.findChildOfType(file, CompactPragmaForm.class);
    if (pragma == null) {
      return;
    }
    Document document = PsiDocumentManager.getInstance(project).getDocument(file);
    if (document == null) {
      return;
    }

    PsiElement id = pragma.getPragmaIdentifier();
    String idText = id != null ? id.getText() : "language_version";
    boolean isCompilerPragma = "compiler_version".equals(idText);
    String versionToSet = isCompilerPragma ? activeVersion : CompactVersionManager.getLanguageVersionForToolchain(activeVersion);
    String newPragma = "pragma " + idText + " >= " + versionToSet + ";";

    WriteCommandAction.runWriteCommandAction(project, () -> {
      document.replaceString(pragma.getTextRange().getStartOffset(), pragma.getTextRange().getEndOffset(), newPragma);
      PsiDocumentManager.getInstance(project).commitDocument(document);
    });

    CompactProblemUtil.clearProblemsAndRestart(project, file.getVirtualFile());
  }
}
