package dev.verloren.midnight.completion;

import com.intellij.codeInsight.AutoPopupController;
import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.editorActions.TabOutScopesTracker;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.template.impl.TemplateManagerImpl;
import com.intellij.codeInsight.template.impl.TemplateState;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * Insert handler for {@code assert} completion that automatically appends parentheses {@code ()},
 * positions the caret inside the parentheses so the user can immediately specify the condition
 * and failure message (e.g. {@code assert(_x1 != _x2, "Cannot use the same number twice");}),
 * registers a tab-out scope, and triggers auto-popup completion for argument expressions.
 */
public final class CompactAssertInsertHandler implements InsertHandler<LookupElement> {

  public static final CompactAssertInsertHandler INSTANCE = new CompactAssertInsertHandler();

  private CompactAssertInsertHandler() {}

  @Override
  @SuppressWarnings("DuplicatedCode")
  public void handleInsert(@NotNull InsertionContext context, @NotNull LookupElement item) {
    Editor editor = context.getEditor();
    Document document = context.getDocument();
    int tailOffset = context.getTailOffset();
    Project project = context.getProject();
    CharSequence chars = document.getCharsSequence();

    // Check if '(' already follows the inserted assert keyword
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
          editor.getCaretModel().moveToOffset(caretTarget);
          TabOutScopesTracker.getInstance().registerEmptyScopeAtCaret(editor);
          AutoPopupController.getInstance(project).scheduleAutoPopup(editor);
        }
      });
    }
  }
}
