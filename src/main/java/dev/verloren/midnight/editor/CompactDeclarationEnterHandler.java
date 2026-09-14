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
 * Handles Enter key on declaration template trigger prefixes (e.g. {@code cir}, {@code ci},
 * {@code wi}, {@code wit}, {@code excir}, {@code exci}, {@code export circ}, etc.)
 * directly expanding them into full declaration boilerplates with live template placeholders.
 */
public class CompactDeclarationEnterHandler extends EnterHandlerDelegateAdapter {

  @Override
  public Result preprocessEnter(
      @NotNull PsiFile file,
      @NotNull Editor editor,
      @NotNull Ref<Integer> caretOffsetRef,
      @NotNull Ref<Integer> caretAdvanceRef,
      @NotNull DataContext dataContext,
      EditorActionHandler originalHandler) {

    if (!file.getLanguage().isKindOf(CompactLanguage.INSTANCE) || !(file instanceof CompactFile)) {
      return Result.Continue;
    }

    // If an active lookup popup is open, allow completion to handle the Enter key
    if (LookupManager.getActiveLookup(editor) != null) {
      return Result.Continue;
    }

    // If a live template is already active in this editor, let the template navigate/advance
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

    // Ensure line suffix is empty (caret is at the end of the trigger)
    String lineSuffix = document.getText(new TextRange(caretOffset, lineEnd)).trim();
    if (!lineSuffix.isEmpty()) {
      return Result.Continue;
    }

    String linePrefix = document.getText(new TextRange(lineStart, caretOffset));
    String trimmed = linePrefix.trim();
    if (trimmed.isEmpty()) {
      return Result.Continue;
    }

    TriggerResult triggerResult = CompactDeclarationTriggerResolver.resolve(trimmed);
    if (triggerResult == null) {
      return Result.Continue;
    }

    // Check PSI context at caret: must not be in a comment or string literal or inside circuit/constructor body
    PsiDocumentManager.getInstance(project).commitDocument(document);
    PsiElement element = file.findElementAt(Math.max(0, caretOffset - 1));
    if (element != null) {
      String elType = element.getNode().getElementType().toString();
      if (elType.contains("COMMENT") || elType.contains("STRING")) {
        return Result.Continue;
      }
      // Top-level / module check: export declarations cannot be inside circuit or constructor bodies
      if (triggerResult.isExported()) {
        if (PsiTreeUtil.getParentOfType(element, CompactCircuitDefinition.class, CompactConstructorDeclaration.class) != null) {
          return Result.Continue;
        }
      }
    }

    int triggerStartInLine = linePrefix.indexOf(trimmed);
    int triggerStart = lineStart + triggerStartInLine;

    WriteCommandAction.runWriteCommandAction(project, "Expand Compact Declaration Template", null, () -> {
      // Delete the typed trigger prefix
      document.deleteString(triggerStart, caretOffset);

      // Insert the canonical keyword (with export prefix if applicable)
      String keyword = triggerResult.isExported()
          ? "export " + triggerResult.declarationType().getBaseName()
          : triggerResult.declarationType().getBaseName();

      document.insertString(triggerStart, keyword);
      int tailOffset = triggerStart + keyword.length();
      editor.getCaretModel().moveToOffset(tailOffset);
      PsiDocumentManager.getInstance(project).commitDocument(document);

      // Expand declaration template boilerplate
      if (triggerResult.declarationType() == CompactDeclarationType.LEDGER) {
        CompactLedgerInsertHandler.INSTANCE.insertTemplate(project, editor, tailOffset);
      } else {
        new CompactDeclarationInsertHandler(triggerResult.declarationType()).insertTemplate(project, editor, tailOffset);
      }
    });

    return Result.Stop;
  }
}
