package dev.verloren.midnight.annotator;

import com.intellij.codeInsight.intention.impl.BaseIntentionAction;
import com.intellij.codeInsight.intention.preview.IntentionPreviewUtils;
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
 * Quick-fix and intention action to update a file's {@code pragma} directive to match the active compiler.
 *
 * <h3>IntelliJ Platform Subsystem Role & Calling Pipeline</h3>
 * <p>
 * Implements both {@link LocalQuickFix} and {@link com.intellij.codeInsight.intention.IntentionAction}:
 * <ul>
 *   <li><b>Dual Usage:</b> Attached to {@link dev.verloren.midnight.inspection.CompactPragmaVersionInspection}
 *       and available in the Alt+Enter intention actions menu.</li>
 *   <li><b>Document Mutation:</b> Rewrites the pragma statement to {@code pragma <id> >= <activeVersion>;}
 *       inside a {@link WriteCommandAction}.</li>
 *   <li><b>Post-Fix Analysis Trigger:</b> Calls {@link CompactProblemUtil#clearProblemsAndRestart}
 *       to immediately wipe stale warnings and refresh compiler analysis.</li>
 * </ul>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Write Action Mandatory:</b> Document text replacement runs on the Event Dispatch Thread (EDT)
 *       wrapped inside {@link WriteCommandAction#runWriteCommandAction}.</li>
 * </ul>
 *
 * @see dev.verloren.midnight.psi.CompactPragmaForm
 * @see dev.verloren.midnight.annotator.CompactProblemUtil
 */
public class CompactUpdatePragmaQuickFix extends BaseIntentionAction implements LocalQuickFix {

  /**
   * The version of the active compiler to set in the pragma directive.
   */
  private final String activeVersion;

  /**
   * Constructs the quick-fix with the active compiler version.
   *
   * @param activeVersion compiler version string
   */
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

  /**
   * Performs the document rewrite and triggers daemon re-analysis.
   */
  private void invokeWithPragma(@NotNull Project project, @Nullable PsiFile file, @Nullable CompactPragmaForm explicitPragma) {
    if (file == null) {
      return;
    }
    // Step 1: Locate the pragma element in the file
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

    // Step 2: Formulate the replacement pragma string
    PsiElement id = pragma.getPragmaIdentifier();
    String idText = id != null ? id.getText() : "language_version";
    boolean isCompilerPragma = "compiler_version".equals(idText);
    String versionToSet = isCompilerPragma ? activeVersion : CompactVersionManager.getLanguageVersionForToolchain(activeVersion);
    String newPragma = "pragma " + idText + " >= " + versionToSet + ";";

    // Step 3: Mutate document text inside a WriteCommandAction
    WriteCommandAction.runWriteCommandAction(project, () -> {
      document.replaceString(pragma.getTextRange().getStartOffset(), pragma.getTextRange().getEndOffset(), newPragma);
      PsiDocumentManager.getInstance(project).commitDocument(document);
    });

    // Step 4: Refresh diagnostics (unless generating intention preview)
    if (!IntentionPreviewUtils.isIntentionPreviewActive()) {
      CompactProblemUtil.clearProblemsAndRestart(project, file.getVirtualFile());
    }
  }
}
