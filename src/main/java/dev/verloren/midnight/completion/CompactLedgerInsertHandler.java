package dev.verloren.midnight.completion;

import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.template.Template;
import com.intellij.codeInsight.template.TemplateManager;
import com.intellij.codeInsight.template.impl.ConstantNode;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import dev.verloren.midnight.ide.templates.CompactDeclarationNameGenerator;
import dev.verloren.midnight.ide.templates.CompactDeclarationType;
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
      if (editor != null) {
        editor.getCaretModel().moveToOffset(tailOffset);
      }
      chars = document.getCharsSequence();
    }

    // Check if line remainder already contains an identifier or colon
    int lineEnd = document.getLineEndOffset(document.getLineNumber(tailOffset));
    String lineSuffix = chars.subSequence(tailOffset, lineEnd).toString().trim();
    if (!lineSuffix.isEmpty() && (lineSuffix.contains(":") || lineSuffix.matches("^[a-zA-Z_].*"))) {
      if (tailOffset < chars.length() && !Character.isWhitespace(chars.charAt(tailOffset))) {
        document.insertString(tailOffset, " ");
        if (editor != null) {
          editor.getCaretModel().moveToOffset(tailOffset + 1);
        }
      }
      return;
    }

    // Launch live template for ' <name>: <type>;' with tab stops
    PsiElement psiContext = context.getFile().findElementAt(context.getStartOffset());
    String defaultName = CompactDeclarationNameGenerator.generateName(CompactDeclarationType.LEDGER, psiContext);

    TemplateManager templateManager = TemplateManager.getInstance(context.getProject());
    if (templateManager != null && editor != null) {
      Template template = templateManager.createTemplate("", "");
      template.setToReformat(true);
      template.addTextSegment(" ");
      template.addVariable("NAME", new ConstantNode(defaultName), true);
      template.addTextSegment(": ");
      template.addVariable("TYPE", new ConstantNode("State"), true);
      template.addTextSegment(";");
      templateManager.startTemplate(editor, template);
    } else {
      String insert = " " + defaultName + ": State;";
      document.insertString(tailOffset, insert);
      if (editor != null) {
        editor.getCaretModel().moveToOffset(tailOffset + insert.length());
      }
    }
  }
}
