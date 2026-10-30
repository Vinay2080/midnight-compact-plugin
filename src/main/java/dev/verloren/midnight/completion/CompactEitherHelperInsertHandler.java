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
 * Insert handler for Compact {@code left} and {@code right} Either constructor helper circuits.
 *
 * <p>Expands typed generic arguments (e.g. {@code <ZswapCoinPublicKey, ContractAddress>})
 * or provides interactive generic type placeholders and places the caret inside the call
 * parentheses {@code ()} with tab-out scope registration and auto-popup completion.</p>
 *
 * <h3>IntelliJ Platform Lifecycle &amp; Threading Contract</h3>
 * <ul>
 *   <li><b>Subsystem Role:</b> {@link InsertHandler} implementation configured on {@code left} and
 *       {@code right} lookup items by {@link dev.verloren.midnight.completion.providers.CompactValueCompletionProvider}.</li>
 *   <li><b>Calling Pipeline:</b> Executed by {@code com.intellij.codeInsight.completion.CodeCompletionHandlerBase}
 *       immediately upon item selection in the editor lookup popup.</li>
 *   <li><b>Threading Model:</b> Runs synchronously on the Event Dispatch Thread (EDT) within an
 *       active Write Action command.</li>
 *   <li><b>Delegation Architecture:</b> Reuses {@link CompactParenthesesInsertHandler#WITH_PARENS} as
 *       a fallback delegate when neither typed nor generic template expansion is required.</li>
 * </ul>
 *
 * <h3>Design Decisions &amp; Architectural Rationale</h3>
 * <ul>
 *   <li><b>Two Modes of Operation:</b>
 *     <ol>
 *       <li><i>Statically Typed:</i> When the expected context type is known (e.g. {@code Either<A, B>}),
 *           instantiated with concrete {@code leftType} and {@code rightType} strings to insert exact types directly.</li>
 *       <li><i>Interactive Generic:</i> When invoked without a surrounding type constraint, creates a live template
 *           allowing the developer to tab between {@code Left} and {@code Right} type parameters.</li>
 *     </ol>
 *   </li>
 *   <li><b>Memory Leak Guards:</b> Stores only immutable {@link String} names and a boolean flag;
 *       no PSI or UI components are retained in instance fields.</li>
 * </ul>
 *
 * <h3>Decision &amp; Expansion Flow</h3>
 * <pre>{@code
 *   handleInsert(context, item)
 *              │
 *              ├── Lookahead past whitespace for '<' and '('
 *              │
 *              ├── Typed Mode (leftType != null && rightType != null)
 *              │     ├── Insert "<Left, Right>" if missing
 *              │     ├── Insert "()" if missing
 *              │     └── Position caret inside () -> TabOut -> AutoPopup
 *              │
 *              ├── Generic Mode (isGeneric == true)
 *              │     └── Launch Live Template: <$LEFT$, $RIGHT$>()
 *              │
 *              └── Default Bare Call Mode
 *                    └── CompactParenthesesInsertHandler.WITH_PARENS
 * }</pre>
 *
 * @see CompactEitherInsertHandler
 * @see CompactParenthesesInsertHandler
 * @see TabOutScopesTracker
 * @see AutoPopupController
 */
public class CompactEitherHelperInsertHandler implements InsertHandler<LookupElement> {

  /** Concrete left variant type name, or {@code null} if untyped. */
  private final @Nullable String leftType;

  /** Concrete right variant type name, or {@code null} if untyped. */
  private final @Nullable String rightType;

  /** Whether to construct an interactive live template with variable placeholders. */
  private final boolean isGeneric;

  /**
   * Constructs a typed helper insert handler with explicit left and right type arguments.
   *
   * @param leftType  type name for the Left variant (e.g. {@code "Coin"})
   * @param rightType type name for the Right variant (e.g. {@code "ContractAddress"})
   */
  public CompactEitherHelperInsertHandler(@Nullable String leftType, @Nullable String rightType) {
    this.leftType = leftType;
    this.rightType = rightType;
    this.isGeneric = false;
  }

  /**
   * Constructs a generic or bare helper insert handler.
   *
   * @param isGeneric {@code true} to produce interactive template variables, {@code false} for bare parens
   */
  public CompactEitherHelperInsertHandler(boolean isGeneric) {
    this.leftType = null;
    this.rightType = null;
    this.isGeneric = isGeneric;
  }

  /**
   * Handles post-completion document mutation for {@code left(...)} and {@code right(...)} helper circuits.
   *
   * @param context insertion context containing editor and document state
   * @param item    selected lookup element
   */
  @Override
  public void handleInsert(@NotNull InsertionContext context, @NotNull LookupElement item) {
    Editor editor = context.getEditor();
    Document document = context.getDocument();
    int tailOffset = context.getTailOffset();
    CharSequence chars = document.getCharsSequence();
    Project project = context.getProject();

    // Step 1: Lookahead forward past whitespace to test if '<' or '(' already follows the caret
    int offset = tailOffset;
    while (offset < chars.length() && Character.isWhitespace(chars.charAt(offset))) {
      offset++;
    }
    boolean hasAngle = offset < chars.length() && chars.charAt(offset) == '<';
    boolean hasParen = offset < chars.length() && chars.charAt(offset) == '(';

    TemplateManager templateManager = TemplateManager.getInstance(project);
    boolean lookupIncludesAngle = item.getLookupString().contains("<");

    // Step 2: Handle statically typed expansion when both type arguments are known
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

    // Step 3: Handle generic live template expansion with interactive tab stops
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

    // Step 4: Default fallback to bare parenthesis insertion handler
    CompactParenthesesInsertHandler.WITH_PARENS.handleInsert(context, item);
  }
}
