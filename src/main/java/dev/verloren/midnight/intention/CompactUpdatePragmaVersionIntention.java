package dev.verloren.midnight.intention;

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.codeInsight.intention.preview.IntentionPreviewUtils;
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
 * Context intention action to update the file's {@code pragma} statement to match the currently active compiler version.
 *
 * <h3>IntelliJ Platform Subsystem Role & Calling Pipeline</h3>
 * <p>
 * Registered under {@code <intentionAction>} in {@code plugin.xml}:
 * <ul>
 *   <li><b>Contextual Availability:</b> Available in the Alt+Enter context actions popup when the caret
 *       is located on a {@code pragma language_version} or {@code pragma compiler_version} form whose
 *       constraint is NOT satisfied by the active compiler.</li>
 *   <li><b>Pragma Synchronization:</b> Rewrites the directive to {@code pragma <id> >= <activeVersion>;}
 *       inside a {@link WriteCommandAction}.</li>
 *   <li><b>Diagnostics Flush:</b> Calls {@link CompactProblemUtil#clearProblemsAndRestart} to refresh
 *       inspections and external annotator diagnostics immediately.</li>
 * </ul>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>No Automatic Write Action:</b> Overrides {@link #startInWriteAction()} to return {@code false}
 *       because the action manages its own explicit {@link WriteCommandAction} and performs post-processing restarts.</li>
 *   <li><b>Read Action for Availability:</b> {@link #isAvailable} runs under {@code @RequiresReadLock} on the EDT.</li>
 * </ul>
 *
 * @see dev.verloren.midnight.psi.CompactPragmaForm
 * @see dev.verloren.midnight.annotator.CompactProblemUtil
 */
public class CompactUpdatePragmaVersionIntention extends PsiElementBaseIntentionAction {

  @Override
  public @NotNull @Nls(capitalization = Nls.Capitalization.Sentence) String getFamilyName() {
    return "Compact pragma version";
  }

  /**
   * Overridden to false because this action wraps its document replacement in an explicit WriteCommandAction.
   */
  @Override
  public boolean startInWriteAction() {
    return false;
  }

  /**
   * Checks whether the pragma form is unsatisfied by the project's active compiler.
   */
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
      // If the active version already satisfies the constraint, no update is needed
      if (CompactSemVerUtil.satisfiesConstraint(targetVer, constraint)) {
        return false;
      }
    }

    setText("Update pragma to match active compiler (>= " + targetVer + ")");
    return true;
  }

  /**
   * Replaces the pragma text and restarts daemon code analysis.
   */
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

    // Step 1: Replace pragma string inside WriteCommandAction
    WriteCommandAction.runWriteCommandAction(project, () -> {
      document.replaceString(pragma.getTextRange().getStartOffset(), pragma.getTextRange().getEndOffset(), newPragma);
      PsiDocumentManager.getInstance(project).commitDocument(document);
    });

    // Step 2: Clear stale problems and restart daemon analysis (unless previewing intention)
    if (!IntentionPreviewUtils.isIntentionPreviewActive()) {
      VirtualFile vFile = element.getContainingFile() != null ? element.getContainingFile().getVirtualFile() : null;
      CompactProblemUtil.clearProblemsAndRestart(project, vFile);
    }
  }
}
