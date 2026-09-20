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
import org.jetbrains.annotations.NotNull;

/**
 * Insert handler for completing {@code Either} struct literals in value contexts.
 * Starts an interactive live template allowing developers to tab through
 * {@code is_left}, {@code left}, and {@code right} fields.
 */
public class CompactEitherInsertHandler implements InsertHandler<LookupElement> {

  public static final CompactEitherInsertHandler INSTANCE = new CompactEitherInsertHandler();

  private static final String FALLBACK_SNIPPET = " { is_left: true, left: , right: default }";
  private static final int FALLBACK_CARET_OFFSET = " { is_left: true, left: ".length();

  @Override
  public void handleInsert(@NotNull InsertionContext context, @NotNull LookupElement item) {
    Editor editor = context.getEditor();
    Document document = context.getDocument();
    int tailOffset = context.getTailOffset();
    CharSequence chars = document.getCharsSequence();

    // Check if '{' already follows
    int offset = tailOffset;
    while (offset < chars.length() && Character.isWhitespace(chars.charAt(offset))) {
      offset++;
    }
    boolean hasBrace = offset < chars.length() && chars.charAt(offset) == '{';

    if (!hasBrace) {
      Project project = context.getProject();
      TemplateManager templateManager = TemplateManager.getInstance(project);
      if (templateManager != null) {
        Template template = templateManager.createTemplate("", "");
        template.setToReformat(false);
        template.addTextSegment(" { is_left: ");
        template.addVariable("IS_LEFT", new ConstantNode("true"), true);
        template.addTextSegment(", left: ");
        template.addVariable("LEFT", new ConstantNode(""), true);
        template.addTextSegment(", right: ");
        template.addVariable("RIGHT", new ConstantNode("default"), true);
        template.addTextSegment(" }");
        template.addEndVariable();
        templateManager.startTemplate(editor, template);
      } else {
        document.insertString(tailOffset, FALLBACK_SNIPPET);
        editor.getCaretModel().moveToOffset(tailOffset + FALLBACK_CARET_OFFSET);
      }
    }
  }
}
