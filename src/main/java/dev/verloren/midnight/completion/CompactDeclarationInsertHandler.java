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
 * Universal insert handler for Compact declaration keyword completions.
 *
 * <p>Completes the declaration structure with live template fields, pre-filling
 * the declaration identifier with the lowest available numbered name for that type
 * (e.g. {@code circuit1}, {@code witness1}, {@code struct1}).</p>
 */
public class CompactDeclarationInsertHandler implements InsertHandler<LookupElement> {

  private final CompactDeclarationType declarationType;

  public CompactDeclarationInsertHandler(@NotNull CompactDeclarationType declarationType) {
    this.declarationType = declarationType;
  }

  public @NotNull CompactDeclarationType getDeclarationType() {
    return declarationType;
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
      if (editor != null) {
        editor.getCaretModel().moveToOffset(tailOffset);
      }
      chars = document.getCharsSequence();
    }

    // Guard: check if an identifier, signature, or colon already follows the caret on the current line
    int lineEnd = document.getLineEndOffset(document.getLineNumber(tailOffset));
    String lineSuffix = chars.subSequence(tailOffset, lineEnd).toString().trim();
    if (!lineSuffix.isEmpty() && (lineSuffix.matches("^[a-zA-Z_].*")
        || lineSuffix.startsWith("(")
        || lineSuffix.startsWith("{")
        || lineSuffix.startsWith(":"))) {
      if (tailOffset < chars.length() && !Character.isWhitespace(chars.charAt(tailOffset))) {
        document.insertString(tailOffset, " ");
        if (editor != null) {
          editor.getCaretModel().moveToOffset(tailOffset + 1);
        }
      }
      return;
    }

    PsiElement psiContext = context.getFile().findElementAt(context.getStartOffset());
    String suggestedName = CompactDeclarationNameGenerator.generateName(declarationType, psiContext);

    TemplateManager templateManager = TemplateManager.getInstance(context.getProject());
    if (templateManager != null && editor != null) {
      Template template = templateManager.createTemplate("", "");
      template.setToReformat(true);
      buildTemplate(template, suggestedName);
      templateManager.startTemplate(editor, template);
    } else {
      String fallback = buildFallbackString(suggestedName);
      document.insertString(tailOffset, fallback);
      if (editor != null) {
        editor.getCaretModel().moveToOffset(tailOffset + fallback.length());
      }
    }
  }

  private void buildTemplate(@NotNull Template template, @NotNull String suggestedName) {
    switch (declarationType) {
      case CIRCUIT -> {
        template.addTextSegment(" ");
        template.addVariable("NAME", new ConstantNode(suggestedName), true);
        template.addTextSegment("(");
        template.addVariable("PARAMS", new ConstantNode(""), true);
        template.addTextSegment("): ");
        template.addVariable("RET", new ConstantNode("Void"), true);
        template.addTextSegment(" {\n  ");
        template.addEndVariable();
        template.addTextSegment("\n}");
      }
      case WITNESS -> {
        template.addTextSegment(" ");
        template.addVariable("NAME", new ConstantNode(suggestedName), true);
        template.addTextSegment("(");
        template.addVariable("PARAMS", new ConstantNode(""), true);
        template.addTextSegment("): ");
        template.addVariable("RET", new ConstantNode("Field"), true);
        template.addTextSegment(";");
      }
      case STRUCT -> {
        template.addTextSegment(" ");
        template.addVariable("NAME", new ConstantNode(suggestedName), true);
        template.addTextSegment(" {\n  ");
        template.addEndVariable();
        template.addTextSegment("\n}");
      }
      case ENUM -> {
        template.addTextSegment(" ");
        template.addVariable("NAME", new ConstantNode(suggestedName), true);
        template.addTextSegment(" {\n  ");
        template.addEndVariable();
        template.addTextSegment("\n}");
      }
      case MODULE -> {
        template.addTextSegment(" ");
        template.addVariable("NAME", new ConstantNode(suggestedName), true);
        template.addTextSegment(" {\n  ");
        template.addEndVariable();
        template.addTextSegment("\n}");
      }
      case CONTRACT -> {
        template.addTextSegment(" ");
        template.addVariable("NAME", new ConstantNode(suggestedName), true);
        template.addTextSegment(" {\n  ");
        template.addEndVariable();
        template.addTextSegment("\n}");
      }
      case TYPE -> {
        template.addTextSegment(" ");
        template.addVariable("NAME", new ConstantNode(suggestedName), true);
        template.addTextSegment(" = ");
        template.addVariable("TYPE", new ConstantNode("Field"), true);
        template.addTextSegment(";");
      }
      case LEDGER -> {
        template.addTextSegment(" ");
        template.addVariable("NAME", new ConstantNode(suggestedName), true);
        template.addTextSegment(": ");
        template.addVariable("TYPE", new ConstantNode("State"), true);
        template.addTextSegment(";");
      }
      case CONST -> {
        template.addTextSegment(" ");
        template.addVariable("NAME", new ConstantNode(suggestedName), true);
        template.addTextSegment(" = ");
        template.addEndVariable();
        template.addTextSegment(";");
      }
    }
  }

  private @NotNull String buildFallbackString(@NotNull String suggestedName) {
    return switch (declarationType) {
      case CIRCUIT -> " " + suggestedName + "(): Void {\n}";
      case WITNESS -> " " + suggestedName + "(): Field;";
      case STRUCT -> " " + suggestedName + " {\n}";
      case ENUM -> " " + suggestedName + " {\n}";
      case MODULE -> " " + suggestedName + " {\n}";
      case CONTRACT -> " " + suggestedName + " {\n}";
      case TYPE -> " " + suggestedName + " = Field;";
      case LEDGER -> " " + suggestedName + ": State;";
      case CONST -> " " + suggestedName + " = 0;";
    };
  }
}
