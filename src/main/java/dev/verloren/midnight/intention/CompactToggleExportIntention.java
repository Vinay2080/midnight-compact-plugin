package dev.verloren.midnight.intention;

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import dev.verloren.midnight.psi.*;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * In-editor intention action to toggle the {@code export} modifier on top-level declarations.
 *
 * <h3>IntelliJ Platform Subsystem Role & Calling Pipeline</h3>
 * <p>
 * Registered under {@code <intentionAction>} in {@code plugin.xml}:
 * <ul>
 *   <li><b>Contextual Scope:</b> Available on the header line of exportable declarations
 *       ({@code circuit}, {@code contract}, {@code struct}, {@code enum}, {@code module},
 *       {@code type}, {@code ledger}, and {@code witness}).</li>
 *   <li><b>Grammar Constraint:</b> Compact block-scoped constants ({@code const}) cannot be exported
 *       and are explicitly excluded.</li>
 *   <li><b>Smart Whitespace Cleanup:</b> When removing {@code export}, uses
 *       {@link CompactPsiUtil#deleteNodeWithTrailingWhitespace} to avoid leaving trailing double spaces.
 *       When adding, inserts {@code "export "} cleanly at the start offset of the declaration node.</li>
 * </ul>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Read Action for Availability:</b> {@link #isAvailable} runs under a Read Action ({@code @RequiresReadLock})
 *       on the EDT.</li>
 *   <li><b>Write Action for Invocation:</b> {@link #invoke} mutates the editor {@link Document} on the EDT
 *       within the platform's default write action.</li>
 * </ul>
 *
 * @see dev.verloren.midnight.psi.CompactPsiUtil#deleteNodeWithTrailingWhitespace
 * @see dev.verloren.midnight.lexer.CompactTokenTypes#EXPORT
 */
public class CompactToggleExportIntention extends PsiElementBaseIntentionAction {

  @Override
  public @NotNull @Nls(capitalization = Nls.Capitalization.Sentence) String getFamilyName() {
    return "Compact export modifier";
  }

  /**
   * Identifies an enclosing declaration capable of receiving the export modifier.
   */
  @Nullable
  private PsiElement findExportableDeclaration(@NotNull PsiElement element) {
    return PsiTreeUtil.getParentOfType(
        element,
        CompactCircuitDefinition.class,
        CompactExternalContractDeclaration.class,
        CompactStructDefinition.class,
        CompactEnumDefinition.class,
        CompactModuleDefinition.class,
        CompactTypeDefinition.class,
        CompactLedgerDeclaration.class,
        CompactWitnessDeclaration.class
    );
  }

  /**
   * Checks whether the caret is on the header line of an exportable declaration.
   */
  @Override
  public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
    PsiElement decl = findExportableDeclaration(element);
    if (decl == null) {
      return false;
    }

    // Step 1: Bound availability to header line(s) before opening brace
    ASTNode lbrace = decl.getNode().findChildByType(CompactTokenTypes.LBRACE);
    if (lbrace != null) {
      Document doc = editor.getDocument();
      int caretLine = doc.getLineNumber(element.getTextOffset());
      int lbraceLine = doc.getLineNumber(lbrace.getTextRange().getStartOffset());
      if (caretLine > lbraceLine) {
        return false;
      }
    }

    // Step 2: Dynamically toggle text based on whether 'export' is already present
    boolean isExported = decl.getNode().findChildByType(CompactTokenTypes.EXPORT) != null;
    if (isExported) {
      setText("Remove 'export' modifier");
    } else {
      setText("Add 'export' modifier");
    }
    return true;
  }

  /**
   * Toggles the presence of the export modifier in the document.
   */
  @Override
  public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element) throws IncorrectOperationException {
    PsiElement decl = findExportableDeclaration(element);
    if (decl == null) {
      return;
    }

    Document document = editor.getDocument();
    ASTNode exportNode = decl.getNode().findChildByType(CompactTokenTypes.EXPORT);

    if (exportNode != null) {
      // Step 3: Remove 'export' and trailing whitespace
      CompactPsiUtil.deleteNodeWithTrailingWhitespace(document, exportNode);
    } else {
      // Step 4: Insert 'export ' at the start of the declaration
      int offset = decl.getTextRange().getStartOffset();
      document.insertString(offset, "export ");
    }
  }
}
