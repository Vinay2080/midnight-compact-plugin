package dev.verloren.midnight.completion;

import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.template.Template;
import com.intellij.codeInsight.template.TemplateManager;
import com.intellij.codeInsight.template.impl.ConstantNode;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import dev.verloren.midnight.ide.templates.CompactDeclarationNameGenerator;
import dev.verloren.midnight.ide.templates.CompactDeclarationType;
import dev.verloren.midnight.ide.templates.CompactTypeExpression;
import org.jetbrains.annotations.NotNull;

/**
 * Insert handler for ledger declarations that completes the declaration skeleton
 * with live template fields for the ledger name and type.
 *
 * <p>Supports both exported ({@code export ledger}) and bare ({@code ledger}) forms,
 * deduplicating any redundant leading {@code export} when already present.</p>
 */
public class CompactLedgerInsertHandler implements InsertHandler<LookupElement> {

  public static final CompactLedgerInsertHandler INSTANCE = new CompactLedgerInsertHandler();

  private CompactLedgerInsertHandler() {
  }

  @Override
  public void handleInsert(@NotNull InsertionContext context, @NotNull LookupElement item) {
    Editor editor = context.getEditor();
    Document document = context.getDocument();
    int startOffset = context.getStartOffset();
    int tailOffset = context.getTailOffset();
    CharSequence chars = document.getCharsSequence();

    // Deduplicate export if 'export' was already preceding on the line and item starts with 'export'
    int lineStart = document.getLineStartOffset(document.getLineNumber(startOffset));
    boolean hasPrecedingExport = CompactCompletionContext.hasPrecedingExportOnLine(chars, lineStart, startOffset);
    boolean itemHasExport = item.getLookupString().startsWith("export");

    if (hasPrecedingExport && itemHasExport) {
      document.deleteString(startOffset, startOffset + "export ".length());
      tailOffset -= "export ".length();
      context.setTailOffset(tailOffset);
      editor.getCaretModel().moveToOffset(tailOffset);
      chars = document.getCharsSequence();
    }

    // Guard: check if an identifier or colon already follows the caret on the current line
    int lineEnd = document.getLineEndOffset(document.getLineNumber(tailOffset));
    String lineSuffix = chars.subSequence(tailOffset, lineEnd).toString().trim();
    if (!lineSuffix.isEmpty() && (lineSuffix.matches("^[a-zA-Z_].*") || lineSuffix.startsWith(":"))) {
      if (tailOffset < chars.length() && !Character.isWhitespace(chars.charAt(tailOffset))) {
        document.insertString(tailOffset, " ");
        editor.getCaretModel().moveToOffset(tailOffset + 1);
      }
      return;
    }

    insertTemplate(context.getProject(), editor, tailOffset);
  }

  /**
   * Programmatically launches the live ledger template at {@code tailOffset} in the given editor.
   *
   * @param project    current project
   * @param editor     active editor
   * @param tailOffset document offset directly following the ledger keyword
   */
  public void insertTemplate(@NotNull Project project, @NotNull Editor editor, int tailOffset) {
    PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
    PsiElement psiContext = psiFile != null ? psiFile.findElementAt(Math.max(0, tailOffset - 1)) : null;
    String defaultName = CompactDeclarationNameGenerator.generateName(CompactDeclarationType.LEDGER, psiContext);

    TemplateManager templateManager = TemplateManager.getInstance(project);
    if (templateManager != null) {
      Template template = templateManager.createTemplate("", "");
      template.setToReformat(true);
      template.addTextSegment(" ");
      template.addVariable("NAME", new ConstantNode(defaultName), true);
      template.addTextSegment(": ");
      template.addVariable("TYPE", new CompactTypeExpression("State"), true);
      template.addTextSegment(";");
      templateManager.startTemplate(editor, template);
    } else {
      String insert = " " + defaultName + ": State;";
      editor.getDocument().insertString(tailOffset, insert);
      editor.getCaretModel().moveToOffset(tailOffset + insert.length());
    }
  }
}
