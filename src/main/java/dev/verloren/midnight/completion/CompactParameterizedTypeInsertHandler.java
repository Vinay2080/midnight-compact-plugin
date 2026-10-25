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
 * {@code Vector}, and {@code Opaque}) that automatically constructs generic angle brackets
 * {@code <>} (or quoted domain brackets {@code <"">}), positions the caret inside,
 * registers tab-out scope navigation, and triggers auto-popup completion for sizes or arguments.
 *
 * <h3>IntelliJ Platform Lifecycle &amp; Threading Contract</h3>
 * <ul>
 *   <li><b>Subsystem Role:</b> {@link InsertHandler} implementation attached to parameterised type
 *       lookup elements by {@link dev.verloren.midnight.completion.providers.CompactTypeCompletionProvider}.</li>
 *   <li><b>Calling Pipeline:</b> Invoked directly on the Event Dispatch Thread (EDT) by IntelliJ's
 *       {@code com.intellij.codeInsight.completion.CodeCompletionHandlerBase} immediately after the type
 *       lookup item is selected.</li>
 *   <li><b>Threading Model:</b> Executed synchronously on the EDT within an active Write Action command.
 *       Asynchronous caret re-anchoring is scheduled via {@link ApplicationManager#getApplication()}'s
 *       {@code invokeLater} wrapped in a {@link WriteCommandAction}.</li>
 *   <li><b>Cancellation &amp; Lifecycle Safety:</b> Verifies {@code !editor.isDisposed()} prior to running
 *       deferred caret updates and auto-popup invocations.</li>
 * </ul>
 *
 * <h3>Design Decisions &amp; Architectural Rationale</h3>
 * <ul>
 *   <li><b>Pre-Allocated Singletons:</b> Provides {@link #BRACKETS} (for standard types like {@code Bytes},
 *       {@code Uint}, {@code Vector}) and {@link #OPAQUE_BRACKETS} (for string-domain types like {@code Opaque<"">}),
 *       eliminating allocation churn during frequent type completion requests.</li>
 *   <li><b>Domain Quotation Support:</b> Automatically inserts inner quotation marks {@code <"">} when completing
 *       {@code Opaque} and positions the caret between the quotes ({@code <"|">}), guiding developers toward
 *       the mandatory string domain literal.</li>
 *   <li><b>Memory Leak Guards:</b> Zero UI or PSI instances stored in fields; only the primitive {@code boolean isOpaque}
 *       configuration flag is retained.</li>
 * </ul>
 *
 * <h3>Insertion &amp; Re-Anchoring Pipeline</h3>
 * <pre>{@code
 *   Lookup Selection [Bytes, Uint, Opaque, Vector]
 *          │
 *          ▼
 *   handleInsert(context) ──► Lookahead past whitespace for '<'
 *          │                          │
 *          ├─ Missing '<' ────────────┴─► Found '<'
 *          ▼                                │
 *     Insert "<>" or "<\"\">"               │
 *          │                                │
 *          └───────────────┬────────────────┘
 *                          ▼
 *             Position Caret at <|> or <"|">
 *                          │
 *       ┌──────────────────┴──────────────────┐
 *       ▼                                     ▼
 *   Register Tab-Out Scope          Schedule Auto-Popup
 *       │
 *       ▼
 *   TemplateState Active? ──► [invokeLater] Re-anchor Caret inside <>
 * }</pre>
 *
 * @see dev.verloren.midnight.completion.providers.CompactTypeCompletionProvider
 * @see TabOutScopesTracker
 * @see AutoPopupController
 * @see TemplateManagerImpl
 */
public class CompactParameterizedTypeInsertHandler implements InsertHandler<LookupElement> {

  /** Shared singleton insert handler for standard parameterised types inserting {@code <>}. */
  public static final CompactParameterizedTypeInsertHandler BRACKETS =
      new CompactParameterizedTypeInsertHandler(false);

  /** Shared singleton insert handler for opaque types inserting quoted domain brackets {@code <"">}. */
  public static final CompactParameterizedTypeInsertHandler OPAQUE_BRACKETS =
      new CompactParameterizedTypeInsertHandler(true);

  /** Whether this handler configures an opaque type requiring interior quotation marks. */
  private final boolean isOpaque;

  /**
   * Constructs an insert handler with the specified quotation formatting.
   *
   * @param isOpaque {@code true} to insert {@code <"">}, {@code false} to insert {@code <>}
   */
  public CompactParameterizedTypeInsertHandler(boolean isOpaque) {
    this.isOpaque = isOpaque;
  }

  /**
   * Performs document modification, caret repositioning, tab-out registration, and auto-popup triggers.
   *
   * @param context insertion context containing editor, document, and offset metadata
   * @param item    the selected type lookup element
   */
  @Override
  @SuppressWarnings("DuplicatedCode")
  public void handleInsert(@NotNull InsertionContext context, @NotNull LookupElement item) {
    Editor editor = context.getEditor();
    Document document = context.getDocument();
    int tailOffset = context.getTailOffset();
    Project project = context.getProject();
    CharSequence chars = document.getCharsSequence();

    // Step 1: Lookahead forward past whitespace to check if '<' already follows
    // the inserted type name (e.g. when replacing a prefix before existing angle brackets).
    // This prevents inserting duplicate brackets like Bytes <><32>.
    int offset = tailOffset;
    while (offset < chars.length() && Character.isWhitespace(chars.charAt(offset))) {
      offset++;
    }
    boolean hasAngle = offset < chars.length() && chars.charAt(offset) == '<';

    // Step 2: Conditionally insert angle brackets and compute target caret offset
    int caretTarget;
    if (!hasAngle) {
      String insertString = isOpaque ? "<\"\">" : "<>";
      int caretDelta = isOpaque ? 2 : 1; // inside <"|"> or inside <|>
      document.insertString(tailOffset, insertString);
      caretTarget = tailOffset + caretDelta;
    } else {
      caretTarget = offset + (isOpaque ? 2 : 1);
    }

    // Step 3: Move the caret into position between the angle brackets
    editor.getCaretModel().moveToOffset(caretTarget);

    // Step 4: Register Tab-Out scope to enable [Tab] key navigation past closing '>'
    TabOutScopesTracker.getInstance().registerEmptyScopeAtCaret(editor);

    // Step 5: Schedule auto-popup to immediately suggest valid type sizes or type parameters
    AutoPopupController.getInstance(project).scheduleAutoPopup(editor);

    // Step 6: Live Template Synchronisation.
    // If a live template is currently active in the editor (e.g. from declaration templates),
    // TemplateState advances or finishes synchronously on lookup selection, which would otherwise
    // move the caret past the end of the template (after the semicolon or brace).
    // We schedule caret repositioning onto the EDT via invokeLater to guarantee the cursor remains
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
