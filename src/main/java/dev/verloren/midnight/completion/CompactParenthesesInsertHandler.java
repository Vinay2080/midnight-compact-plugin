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
 * Insert handler for callable Compact identifiers (e.g. {@code left()}, {@code right()}, and circuit calls) that:
 * <ul>
 *   <li>Automatically appends parentheses {@code ()} if not already present.</li>
 *   <li>Positions the caret directly inside the parentheses ({@code (|)}).</li>
 *   <li>Performs lookahead past whitespace to avoid inserting duplicate {@code ()} when editing existing calls.</li>
 *   <li>Registers a tab-out scope via {@link TabOutScopesTracker} so pressing {@code Tab} steps over the closing parenthesis.</li>
 *   <li>Schedules an auto-popup via {@link AutoPopupController} to immediately suggest argument expressions.</li>
 *   <li>Synchronises with active live templates ({@link TemplateState}) to prevent premature cursor displacement.</li>
 * </ul>
 *
 * <h3>IntelliJ Platform Lifecycle &amp; Threading Contract</h3>
 * <ul>
 *   <li><b>Subsystem Role:</b> {@link InsertHandler} implementation attached to callable lookup elements
 *       by {@link dev.verloren.midnight.completion.providers.CompactValueCompletionProvider} and
 *       {@link dev.verloren.midnight.completion.providers.CompactMemberAccessCompletionProvider}.</li>
 *   <li><b>Calling Pipeline:</b> Invoked on the Event Dispatch Thread (EDT) by IntelliJ's
 *       {@code com.intellij.codeInsight.completion.CodeCompletionHandlerBase} immediately following lookup selection.</li>
 *   <li><b>Threading Model:</b> Runs synchronously on the EDT within an active Write Action command.
 *       Asynchronous caret re-anchoring is scheduled via {@link ApplicationManager#getApplication()}'s
 *       {@code invokeLater} wrapped in a {@link WriteCommandAction}.</li>
 *   <li><b>Cancellation &amp; Lifecycle Safety:</b> Verifies {@code !editor.isDisposed()} prior to running
 *       deferred caret updates and auto-popup invocations.</li>
 * </ul>
 *
 * <h3>Design Decisions &amp; Architectural Rationale</h3>
 * <ul>
 *   <li><b>Singleton Pattern:</b> Instantiated as an immutable, stateless singleton ({@link #WITH_PARENS})
 *       with a private constructor to eliminate garbage collection overhead during high-frequency typing
 *       and completion queries.</li>
 *   <li><b>Memory Leak Prevention:</b> Strictly retains zero references to {@link Project},
 *       {@link Editor}, {@link Document}, or {@link LookupElement} instances within field state;
 *       all context is transiently consumed through {@link InsertionContext}.</li>
 * </ul>
 *
 * <h3>Invocation &amp; Re-Anchoring Pipeline</h3>
 * <pre>{@code
 *   Lookup Selection [callable()]
 *          │
 *          ▼
 *   handleInsert(context) ──► Lookahead past whitespace for '('
 *          │                          │
 *          ├─ Missing '(' ────────────┴─► Found '('
 *          ▼                                │
 *     Insert "()" & compute target          │
 *          │                                │
 *          └───────────────┬────────────────┘
 *                          ▼
 *             Position Caret at (|)
 *                          │
 *       ┌──────────────────┴──────────────────┐
 *       ▼                                     ▼
 *   Register Tab-Out Scope          Schedule Auto-Popup
 *       │
 *       ▼
 *   TemplateState Active? ──► [invokeLater] Re-anchor Caret inside ()
 * }</pre>
 *
 * @see InsertHandler
 * @see InsertionContext
 * @see TabOutScopesTracker
 * @see AutoPopupController
 * @see TemplateManagerImpl
 */
public final class CompactParenthesesInsertHandler implements InsertHandler<LookupElement> {

  /**
   * Reusable singleton instance to avoid redundant object allocations across completion cycles.
   */
  public static final CompactParenthesesInsertHandler WITH_PARENS = new CompactParenthesesInsertHandler();

  private CompactParenthesesInsertHandler() {}

  @Override
  @SuppressWarnings("DuplicatedCode")
  public void handleInsert(@NotNull InsertionContext context, @NotNull LookupElement item) {
    Editor editor = context.getEditor();
    int tailOffset = context.getTailOffset();
    Project project = context.getProject();

    int caretTarget = ensureParenthesesAndGetCaret(context.getDocument(), tailOffset);

    editor.getCaretModel().moveToOffset(caretTarget);
    TabOutScopesTracker.getInstance().registerEmptyScopeAtCaret(editor);
    AutoPopupController.getInstance(project).scheduleAutoPopup(editor);

    TemplateState templateState = TemplateManagerImpl.getTemplateState(editor);
    if (templateState != null) {
      scheduleTemplateCaretSync(project, editor, caretTarget);
    }
  }

  private static int ensureParenthesesAndGetCaret(@NotNull Document document, int tailOffset) {
    CharSequence chars = document.getCharsSequence();
    int offset = tailOffset;
    while (offset < chars.length() && Character.isWhitespace(chars.charAt(offset))) {
      offset++;
    }
    if (offset < chars.length() && chars.charAt(offset) == '(') {
      return offset + 1;
    }
    document.insertString(tailOffset, "()");
    return tailOffset + 1;
  }

  private static void scheduleTemplateCaretSync(
      @NotNull Project project,
      @NotNull Editor editor,
      int caretTarget
  ) {
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
