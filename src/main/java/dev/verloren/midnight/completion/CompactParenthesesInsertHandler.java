package dev.verloren.midnight.completion;

import com.intellij.codeInsight.AutoPopupController;
import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.editorActions.TabOutScopesTracker;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.template.impl.TemplateManagerImpl;
import com.intellij.codeInsight.template.impl.TemplateState;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * Insert handler for callable identifiers (e.g. {@code left()}, {@code right()}) that
 * appends parentheses {@code ()}, positions the caret inside the parentheses, registers a tab-out
 * scope, and triggers auto-popup.
 */
public final class CompactParenthesesInsertHandler implements InsertHandler<LookupElement> {

  public static final CompactParenthesesInsertHandler WITH_PARENS = new CompactParenthesesInsertHandler();

  private CompactParenthesesInsertHandler() {}

  @Override
  @SuppressWarnings("DuplicatedCode")
  public void handleInsert(@NotNull InsertionContext context, @NotNull LookupElement item) {
    Editor editor = context.getEditor();
    Document document = context.getDocument();
    int tailOffset = context.getTailOffset();
    Project project = context.getProject();
    CharSequence chars = document.getCharsSequence();

    int offset = tailOffset;
    while (offset < chars.length() && Character.isWhitespace(chars.charAt(offset))) {
      offset++;
    }
    boolean hasParen = offset < chars.length() && chars.charAt(offset) == '(';

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

    TemplateState templateState = TemplateManagerImpl.getTemplateState(editor);
    if (templateState != null) {
      ApplicationManager.getApplication().invokeLater(() -> {
        if (!editor.isDisposed()) {
          WriteCommandAction.runWriteCommandAction(project, () -> {
            if (!editor.isDisposed()) {
              editor.getCaretModel().moveToOffset(caretTarget);
              TabOutScopesTracker.getInstance().registerEmptyScopeAtCaret(editor);
              AutoPopupController.getInstance(project).scheduleAutoPopup(editor);
            }
          });
        }
      });
    }
  }
}
