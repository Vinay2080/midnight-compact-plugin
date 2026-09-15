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
 * Universal insert handler for top-level declarations that creates and starts
 * an interactive live template containing tab stops for the declaration name, parameters,
 * return type, and body.
 *
 * <p>Uses {@link CompactDeclarationNameGenerator} to generate auto-numbered declaration
 * names (e.g. {@code circuit1}, {@code circuit2}, {@code witness1}) that avoid collisions
 * with existing declarations in the file or imported modules.</p>
 */
public record CompactDeclarationInsertHandler(@NotNull CompactDeclarationType declarationType) implements InsertHandler<LookupElement> {

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

    // Guard: check if an identifier, signature, or colon already follows the caret on the current line
    int lineEnd = document.getLineEndOffset(document.getLineNumber(tailOffset));
    String lineSuffix = chars.subSequence(tailOffset, lineEnd).toString().trim();
    if (!lineSuffix.isEmpty() && (lineSuffix.matches("^[a-zA-Z_].*")
        || lineSuffix.startsWith("(")
        || lineSuffix.startsWith("{")
        || lineSuffix.startsWith(":"))) {
      if (tailOffset < chars.length() && !Character.isWhitespace(chars.charAt(tailOffset))) {
        document.insertString(tailOffset, " ");
        editor.getCaretModel().moveToOffset(tailOffset + 1);
      }
      return;
    }

    insertTemplate(context.getProject(), editor, tailOffset);
  }

  /**
   * Programmatically launches the live declaration template at {@code tailOffset} in the given editor.\n   *
   * @param project    current project
   * @param editor     active editor
   * @param tailOffset document offset directly following the declaration keyword
   */
  public void insertTemplate(@NotNull Project project, @NotNull Editor editor, int tailOffset) {
    PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
    PsiElement psiContext = psiFile != null ? psiFile.findElementAt(Math.max(0, tailOffset - 1)) : null;
    String suggestedName = CompactDeclarationNameGenerator.generateName(declarationType, psiContext);

    TemplateManager templateManager = TemplateManager.getInstance(project);
    if (templateManager != null) {
      Template template = templateManager.createTemplate("", "");
      template.setToReformat(true);
      buildTemplate(template, suggestedName);
      templateManager.startTemplate(editor, template);
    } else {
      String fallback = buildFallbackString(suggestedName);
      editor.getDocument().insertString(tailOffset, fallback);
      editor.getCaretModel().moveToOffset(tailOffset + fallback.length());
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
        template.addVariable("RET", new CompactTypeExpression("Void"), true);
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
        template.addVariable("RET", new CompactTypeExpression("Field"), true);
        template.addTextSegment(";");
      }
      case STRUCT, ENUM, MODULE, CONTRACT -> {
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
        template.addVariable("TYPE", new CompactTypeExpression("Field"), true);
        template.addTextSegment(";");
      }
      case CONST -> {
        template.addTextSegment(" ");
        template.addVariable("NAME", new ConstantNode(suggestedName), true);
        template.addTextSegment(": ");
        template.addVariable("TYPE", new CompactTypeExpression("Field"), true);
        template.addTextSegment(" = ");
        template.addVariable("VALUE", new ConstantNode("0"), true);
        template.addTextSegment(";");
      }
      case LEDGER -> {
        template.addTextSegment(" ");
        template.addVariable("NAME", new ConstantNode(suggestedName), true);
        template.addTextSegment(": ");
        template.addVariable("TYPE", new CompactTypeExpression("State"), true);
        template.addTextSegment(";");
      }
    }
  }

  private @NotNull String buildFallbackString(@NotNull String suggestedName) {
    return switch (declarationType) {
      case CIRCUIT -> " " + suggestedName + "(): Void {\n  \n}";
      case WITNESS -> " " + suggestedName + "(): Field;";
      case STRUCT, ENUM, MODULE, CONTRACT -> " " + suggestedName + " {\n  \n}";
      case TYPE -> " " + suggestedName + " = Field;";
      case CONST -> " " + suggestedName + ": Field = 0;";
      case LEDGER -> " " + suggestedName + ": State;";
    };
  }
}
