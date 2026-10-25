package dev.verloren.midnight.editor;

import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegateAdapter;
import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.codeInsight.template.impl.TemplateManagerImpl;
import com.intellij.codeInsight.template.impl.TemplateState;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.CompactLanguage;
import dev.verloren.midnight.completion.CompactDeclarationInsertHandler;
import dev.verloren.midnight.completion.CompactLedgerInsertHandler;
import dev.verloren.midnight.ide.templates.CompactDeclarationTriggerResolver;
import dev.verloren.midnight.ide.templates.CompactDeclarationTriggerResolver.TriggerResult;
import dev.verloren.midnight.ide.templates.CompactDeclarationType;
import dev.verloren.midnight.psi.CompactCircuitDefinition;
import dev.verloren.midnight.psi.CompactConstructorDeclaration;
import dev.verloren.midnight.psi.CompactFile;
import org.jetbrains.annotations.NotNull;

/**
 * Editor enter-handler delegate that intercepts the Enter key on declaration trigger prefixes
 * (such as {@code "cir"}, {@code "ci"}, {@code "wit"}, {@code "excir"}, {@code "export circ"})
 * and expands them immediately into full declaration templates with interactive tab stops.
 *
 * <h3>Architectural Role &amp; Extension Point</h3>
 * <p>Registered as an {@code enterHandlerDelegate} extension in {@code plugin.xml}. When a developer
 * types an abbreviation for a declaration (e.g., {@code "circuit"}, {@code "witness"}, {@code "ledger"})
 * and presses Enter without explicitly selecting a completion lookup item, this delegate intercepts
 * the keystroke before the default editor enter handler creates a line break, replacing the prefix
 * with canonical code and launching the corresponding IntelliJ Live Template.</p>
 *
 * <h3>Trigger Resolution Pipeline</h3>
 * <pre>
 *   Keystroke [Enter] on EDT
 *            │
 *            ▼
 *   [CompactDeclarationEnterHandler.preprocessEnter]
 *            │
 *            ├─► Lookup popup active? ───────► YES ─► Result.Continue (let completion finish)
 *            ├─► Live template active? ──────► YES ─► Result.Continue (let template tab/advance)
 *            ├─► Suffix on line after caret? ► YES ─► Result.Continue (caret not at end of trigger)
 *            │
 *            ▼
 *   [CompactDeclarationTriggerResolver.resolve(prefix)]
 *            │
 *            ├─► No matching trigger? ───────► YES ─► Result.Continue
 *            │
 *            ▼
 *   [PSI Context Verification]
 *            │
 *            ├─► Inside comment / string? ───► YES ─► Result.Continue
 *            ├─► Export inside circuit body? ► YES ─► Result.Continue (illegal placement)
 *            │
 *            ▼
 *   [WriteCommandAction: Replace prefix &amp; Run Template]
 *            │
 *            ├─► Delete trigger string
 *            ├─► Insert canonical keyword
 *            ├─► CompactDeclarationInsertHandler / CompactLedgerInsertHandler
 *            │
 *            ▼
 *      Result.Stop (suppress default newline)
 * </pre>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>EDT (Event Dispatch Thread):</b> Executed synchronously on the EDT by the editor action system.</li>
 *   <li><b>Read/Write Actions:</b> Read actions are performed to check PSI context after committing the document.
 *       Modifications are explicitly wrapped in a {@link WriteCommandAction}.</li>
 * </ul>
 *
 * @see CompactDeclarationTriggerResolver
 * @see CompactDeclarationInsertHandler
 * @see CompactLedgerInsertHandler
 */
public class CompactDeclarationEnterHandler extends EnterHandlerDelegateAdapter {

  /**
   * Pre-processes an Enter keypress before the editor executes default line-splitting behavior.
   *
   * <p>If a recognized trigger abbreviation is present on the current line and the caret is positioned
   * directly at its tail, the trigger is erased, replaced with canonical declaration syntax, and expanded
   * into a Live Template.</p>
   *
   * @param file the PSI file being edited
   * @param editor the active editor instance
   * @param caretOffsetRef mutable reference to the current caret offset
   * @param caretAdvanceRef mutable reference indicating how many characters the caret should advance
   * @param dataContext editor data context
   * @param originalHandler the delegate handler that would normally execute Enter
   * @return {@link Result#Stop} if a declaration template was expanded; {@link Result#Continue} otherwise
   */
  @Override
  public Result preprocessEnter(
      @NotNull PsiFile file,
      @NotNull Editor editor,
      @NotNull Ref<Integer> caretOffsetRef,
      @NotNull Ref<Integer> caretAdvanceRef,
      @NotNull DataContext dataContext,
      EditorActionHandler originalHandler) {

    // 1. Language & file type boundary check: only process Compact source files
    if (!file.getLanguage().isKindOf(CompactLanguage.INSTANCE) || !(file instanceof CompactFile)) {
      return Result.Continue;
    }

    // 2. If an active lookup popup is open, allow completion to handle the Enter key
    if (LookupManager.getActiveLookup(editor) != null) {
      return Result.Continue;
    }

    // 3. If a live template is already active in this editor, let the template navigate/advance
    TemplateState templateState = TemplateManagerImpl.getTemplateState(editor);
    if (templateState != null) {
      return Result.Continue;
    }

    Project project = file.getProject();
    Document document = editor.getDocument();
    int caretOffset = editor.getCaretModel().getOffset();
    int lineNumber = document.getLineNumber(caretOffset);
    int lineStart = document.getLineStartOffset(lineNumber);
    int lineEnd = document.getLineEndOffset(lineNumber);

    // 4. Ensure line suffix is empty (caret is strictly at the end of the trigger text)
    String lineSuffix = document.getText(new TextRange(caretOffset, lineEnd)).trim();
    if (!lineSuffix.isEmpty()) {
      return Result.Continue;
    }

    // 5. Extract line prefix leading up to the caret
    String linePrefix = document.getText(new TextRange(lineStart, caretOffset));
    String trimmed = linePrefix.trim();
    if (trimmed.isEmpty()) {
      return Result.Continue;
    }

    // 6. Match trimmed line prefix against known Compact declaration trigger patterns
    TriggerResult triggerResult = CompactDeclarationTriggerResolver.resolve(trimmed);
    if (triggerResult == null) {
      return Result.Continue;
    }

    // 7. Check PSI context at caret: must not be in a comment or string literal or inside circuit/constructor body
    PsiDocumentManager.getInstance(project).commitDocument(document);
    PsiElement element = file.findElementAt(Math.max(0, caretOffset - 1));
    if (element != null) {
      String elType = element.getNode().getElementType().toString();
      if (elType.contains("COMMENT") || elType.contains("STRING")) {
        return Result.Continue;
      }
      // Top-level / module check: export declarations cannot be nested inside circuit or constructor bodies
      if (triggerResult.isExported()) {
        if (PsiTreeUtil.getParentOfType(element, CompactCircuitDefinition.class, CompactConstructorDeclaration.class) != null) {
          return Result.Continue;
        }
      }
    }

    int triggerStartInLine = linePrefix.indexOf(trimmed);
    int triggerStart = lineStart + triggerStartInLine;

    // 8. Execute document replacement and template injection within a registered undoable write command
    WriteCommandAction.runWriteCommandAction(project, "Expand Compact Declaration Template", null, () -> {
      // Delete the typed trigger prefix from the document
      document.deleteString(triggerStart, caretOffset);

      // Insert canonical keyword (prefixed with 'export ' if resolved as exported)
      String keyword = triggerResult.isExported()
          ? "export " + triggerResult.declarationType().getBaseName()
          : triggerResult.declarationType().getBaseName();

      document.insertString(triggerStart, keyword);
      int tailOffset = triggerStart + keyword.length();
      editor.getCaretModel().moveToOffset(tailOffset);
      PsiDocumentManager.getInstance(project).commitDocument(document);

      // Expand declaration template boilerplate with interactive variable tab stops
      if (triggerResult.declarationType() == CompactDeclarationType.LEDGER) {
        CompactLedgerInsertHandler.INSTANCE.insertTemplate(project, editor, tailOffset);
      } else {
        new CompactDeclarationInsertHandler(triggerResult.declarationType()).insertTemplate(project, editor, tailOffset);
      }
    });

    // 9. Stop propagation of the Enter event to prevent inserting an extraneous newline
    return Result.Stop;
  }
}
