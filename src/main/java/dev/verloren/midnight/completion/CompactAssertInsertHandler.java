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
 * Insert handler for the {@code assert} statement keyword completion.
 *
 * <p>When the user selects {@code assert} from the code-completion popup, this handler:
 * <ol>
 *   <li>Inserts a space and the parenthesized assertion outline with a trailing semicolon:
 *       <code>assert ();</code></li>
 *   <li>Positions the caret inside the parentheses: <code>assert (&lt;caret&gt;);</code></li>
 *   <li>Registers a tab-out scope so pressing &lt;Tab&gt; jumps over the closing parenthesis
 *       and semicolon to the next statement.</li>
 *   <li>Triggers an auto-popup completion inside the parentheses so in-scope variables,
 *       constants, and functions are suggested immediately without extra keystrokes.</li>
 * </ol>
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

    // Check if '(' already follows the inserted keyword
    int offset = tailOffset;
    while (offset < chars.length() && Character.isWhitespace(chars.charAt(offset))) {
      offset++;
    }
    boolean hasParen = offset < chars.length() && chars.charAt(offset) == '(';

    int caretTarget;
    if (!hasParen) {
      document.insertString(tailOffset, " ();");
      caretTarget = tailOffset + 2; // inside " (<caret>);"
    } else {
      caretTarget = offset + 1; // inside existing "("
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
