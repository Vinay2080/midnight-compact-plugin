package dev.verloren.midnight.completion;

import com.intellij.codeInsight.AutoPopupController;
import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.editorActions.TabOutScopesTracker;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.template.Template;
import com.intellij.codeInsight.template.TemplateManager;
import com.intellij.codeInsight.template.impl.ConstantNode;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Insert handler for {@code left} and {@code right} Either constructor helper circuits.
 * Expands typed generic arguments (e.g. {@code <ZswapCoinPublicKey, ContractAddress>})
 * and places the caret inside the parentheses {@code ()} with tab-out support.
 */
public class CompactEitherHelperInsertHandler implements InsertHandler<LookupElement> {

  private final @Nullable String leftType;
  private final @Nullable String rightType;
  private final boolean isGeneric;

  public CompactEitherHelperInsertHandler(@Nullable String leftType, @Nullable String rightType) {
    this.leftType = leftType;
    this.rightType = rightType;
    this.isGeneric = false;
  }

  public CompactEitherHelperInsertHandler(boolean isGeneric) {
    this.leftType = null;
    this.rightType = null;
    this.isGeneric = isGeneric;
  }

  @Override
  public void handleInsert(@NotNull InsertionContext context, @NotNull LookupElement item) {
    Editor editor = context.getEditor();
    Document document = context.getDocument();
    int tailOffset = context.getTailOffset();
    CharSequence chars = document.getCharsSequence();
    Project project = context.getProject();

    // Check if '<' or '(' already follows
    int offset = tailOffset;
    while (offset < chars.length() && Character.isWhitespace(chars.charAt(offset))) {
      offset++;
    }
    boolean hasAngle = offset < chars.length() && chars.charAt(offset) == '<';
    boolean hasParen = offset < chars.length() && chars.charAt(offset) == '(';

    TemplateManager templateManager = TemplateManager.getInstance(project);
    boolean lookupIncludesAngle = item.getLookupString().contains("<");

    if (leftType != null && rightType != null) {
      if (!lookupIncludesAngle && !hasAngle) {
        String angleText = "<" + leftType + ", " + rightType + ">";
        document.insertString(tailOffset, angleText);
        tailOffset += angleText.length();
      }
      int caretTarget;
      if (!hasParen) {
        document.insertString(tailOffset, "()");
        caretTarget = tailOffset + 1;
      } else {
        caretTarget = offset + 1;
      }
      editor.getCaretModel().moveToOffset(caretTarget);
      TabOutScopesTracker.getInstance().registerEmptyScopeAtCaret(editor);
      AutoPopupController.getInstance(project).scheduleAutoPopup(editor);
      return;
    }

    if (isGeneric && templateManager != null) {
      Template template = templateManager.createTemplate("", "");
      template.setToReformat(false);
      if (!lookupIncludesAngle && !hasAngle) {
        template.addTextSegment("<");
        template.addVariable("LEFT", new ConstantNode("Left"), new ConstantNode("Left"), true);
        template.addTextSegment(", ");
        template.addVariable("RIGHT", new ConstantNode("Right"), new ConstantNode("Right"), true);
        template.addTextSegment(">()");
      } else if (!hasParen) {
        template.addTextSegment("()");
      }
      template.addEndVariable();
      templateManager.startTemplate(editor, template);
      return;
    }

    // Default bare parenthesis handler
    CompactParenthesesInsertHandler.WITH_PARENS.handleInsert(context, item);
  }
}
