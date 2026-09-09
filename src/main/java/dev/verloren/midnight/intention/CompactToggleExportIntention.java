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
 * In-editor intention to toggle the {@code export} modifier on top-level declarations.
 *
 * <p>Available anywhere on the declaration header line (e.g. {@code contract}, {@code circuit},
 * {@code struct}, {@code enum}, {@code module}, {@code ledger}, {@code witness}, {@code type}).</p>
 */
public class CompactToggleExportIntention extends PsiElementBaseIntentionAction {

  @Override
  public @NotNull @Nls(capitalization = Nls.Capitalization.Sentence) String getFamilyName() {
    return "Compact export modifier";
  }

  @Override
  public boolean startInWriteAction() {
    return true;
  }

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

  @Override
  public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
    PsiElement decl = findExportableDeclaration(element);
    if (decl == null) {
      return false;
    }

    // If declaration has a body brace, allow action anywhere on header line(s) before descending into body
    ASTNode lbrace = decl.getNode().findChildByType(CompactTokenTypes.LBRACE);
    if (lbrace != null) {
      Document doc = editor.getDocument();
      int caretLine = doc.getLineNumber(element.getTextOffset());
      int lbraceLine = doc.getLineNumber(lbrace.getTextRange().getStartOffset());
      if (caretLine > lbraceLine) {
        return false;
      }
    }

    boolean isExported = decl.getNode().findChildByType(CompactTokenTypes.EXPORT) != null;
    if (isExported) {
      setText("Remove 'export' modifier");
    } else {
      setText("Add 'export' modifier");
    }
    return true;
  }

  @Override
  public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element) throws IncorrectOperationException {
    PsiElement decl = findExportableDeclaration(element);
    if (decl == null) {
      return;
    }

    Document document = editor.getDocument();
    ASTNode exportNode = decl.getNode().findChildByType(CompactTokenTypes.EXPORT);

    if (exportNode != null) {
      // Remove 'export '
      int start = exportNode.getTextRange().getStartOffset();
      int end = exportNode.getTextRange().getEndOffset();
      CharSequence chars = document.getCharsSequence();
      while (end < chars.length() && Character.isWhitespace(chars.charAt(end)) && chars.charAt(end) != '\n') {
        end++;
      }
      document.deleteString(start, end);
    } else {
      // Insert 'export ' at the start of the declaration
      int offset = decl.getTextRange().getStartOffset();
      document.insertString(offset, "export ");
    }
  }
}
