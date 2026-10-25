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
 *
 * <h3>IntelliJ Platform Subsystem Role & Calling Pipeline</h3>
 * <p>
 * Offered by {@link dev.verloren.midnight.inspection.CompactUndisclosedWitnessInspection} when private
 * witness data is assigned to public on-chain ledger state without an explicit disclosure boundary:
 * <ul>
 *   <li><b>Intention / Quick-Fix Action:</b> Displayed in the Alt+Enter menu.</li>
 *   <li><b>Synthetic AST Parsing:</b> Uses {@link PsiFileFactory} to parse a synthetic {@code disclose(...)}
 *       expression in a dummy Compact file, extracts the resulting {@link CompactElementTypes#DISCLOSE_EXPR}
 *       node, and replaces the target element with the new wrapped subtree.</li>
 * </ul>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Write Action Mandatory:</b> {@link #applyFix(Project, ProblemDescriptor)} is executed on the
 *       Event Dispatch Thread (EDT) inside a {@link com.intellij.openapi.command.WriteCommandAction}.</li>
 * </ul>
 *
 * @see dev.verloren.midnight.inspection.CompactUndisclosedWitnessInspection
 * @see dev.verloren.midnight.parser.CompactElementTypes#DISCLOSE_EXPR
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

  /**
   * Replaces the violating expression with an identical expression enclosed in {@code disclose(...)}.
   *
   * @param project    current active project
   * @param descriptor problem descriptor identifying the undisclosed expression
   */
  @Override
  public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
    PsiElement element = descriptor.getPsiElement();
    if (element == null) {
      return;
    }
    // Step 1: Synthesize source code containing disclose(...) wrapping the target expression text
    String text = element.getText();
    String wrappedCode = "circuit _dummy(): [] { disclose(" + text + "); }";

    // Step 2: Parse synthetic snippet into an in-memory dummy CompactFile
    CompactFile dummyFile = (CompactFile) PsiFileFactory.getInstance(project)
        .createFileFromText("dummy.compact", CompactFileType.INSTANCE, wrappedCode);

    // Step 3: Extract the generated DISCLOSE_EXPR element and replace the original element in the AST
    for (PsiElement child : PsiTreeUtil.findChildrenOfType(dummyFile, PsiElement.class)) {
      if (child.getNode() != null && child.getNode().getElementType() == CompactElementTypes.DISCLOSE_EXPR) {
        element.replace(child);
        return;
      }
    }
  }
}
