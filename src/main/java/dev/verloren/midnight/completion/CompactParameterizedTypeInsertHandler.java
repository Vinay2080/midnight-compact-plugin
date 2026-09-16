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
 * Insert handler for parameterised Compact types (such as {@code Bytes}, {@code Uint},
 * {@code Vector}, and {@code Opaque}) that automatically appends angle brackets {@code <>},
 * positions the caret inside the brackets so the user can immediately specify the size/parameters,
 * registers a tab-out scope, and triggers auto-popup completion for size options.
 */
public class CompactParameterizedTypeInsertHandler implements InsertHandler<LookupElement> {

  public static final CompactParameterizedTypeInsertHandler BRACKETS =
      new CompactParameterizedTypeInsertHandler(false);
  public static final CompactParameterizedTypeInsertHandler OPAQUE_BRACKETS =
      new CompactParameterizedTypeInsertHandler(true);

  private final boolean isOpaque;

  public CompactParameterizedTypeInsertHandler(boolean isOpaque) {
    this.isOpaque = isOpaque;
  }

  @Override
  @SuppressWarnings("DuplicatedCode")
  public void handleInsert(@NotNull InsertionContext context, @NotNull LookupElement item) {
    Editor editor = context.getEditor();
    Document document = context.getDocument();
    int tailOffset = context.getTailOffset();
    Project project = context.getProject();
    CharSequence chars = document.getCharsSequence();

    // Check if '<' already follows the inserted type name
    int offset = tailOffset;
    while (offset < chars.length() && Character.isWhitespace(chars.charAt(offset))) {
      offset++;
    }
    boolean hasAngle = offset < chars.length() && chars.charAt(offset) == '<';

    int caretTarget;
    if (!hasAngle) {
      String insertString = isOpaque ? "<\"\">" : "<>";
      int caretDelta = isOpaque ? 2 : 1; // inside <"|"> or inside <|>
      document.insertString(tailOffset, insertString);
      caretTarget = tailOffset + caretDelta;
    } else {
      caretTarget = offset + (isOpaque ? 2 : 1);
    }

    editor.getCaretModel().moveToOffset(caretTarget);
    TabOutScopesTracker.getInstance().registerEmptyScopeAtCaret(editor);
    AutoPopupController.getInstance(project).scheduleAutoPopup(editor);

    // If a live template is currently active in the editor (e.g. from declaration templates),
    // TemplateState advances or finishes synchronously on lookup selection, which would otherwise
    // move the caret past the end of the template (after the semicolon).
    // We schedule caret repositioning onto the EDT via invokeLater to ensure the cursor remains
    // inside the angle brackets with the size suggestions visible.
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
