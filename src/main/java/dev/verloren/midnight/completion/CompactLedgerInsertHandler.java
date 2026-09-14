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
 * <p>Ensures that {@code export} is present in front of the ledger declaration,
 * then sets up interactive template variables for {@code <name>: <type>;} with an
 * auto-incremented default name (e.g. {@code ledger1}, {@code ledger2}).</p>
 */
public class CompactLedgerInsertHandler implements InsertHandler<LookupElement> {

  public static final CompactLedgerInsertHandler INSTANCE = new CompactLedgerInsertHandler();

  @Override
  public void handleInsert(@NotNull InsertionContext context, @NotNull LookupElement item) {
    Editor editor = context.getEditor();
    Document document = context.getDocument();
    int tailOffset = context.getTailOffset();
    CharSequence chars = document.getCharsSequence();

    // Check line prefix: ensure 'export ' is in front of 'ledger'
    int lineStart = document.getLineStartOffset(document.getLineNumber(tailOffset));
    String linePrefix = chars.subSequence(lineStart, tailOffset).toString().trim();
    if (!linePrefix.startsWith("export")) {
      // Find start of the word that was just completed
      int wordStart = tailOffset - item.getLookupString().length();
      if (wordStart >= lineStart && !linePrefix.contains("export")) {
        document.insertString(wordStart, "export ");
        tailOffset += "export ".length();
        context.setTailOffset(tailOffset);
        if (editor != null) {
          editor.getCaretModel().moveToOffset(tailOffset);
        }
      }
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
