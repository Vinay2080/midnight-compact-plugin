package dev.verloren.midnight.completion;

import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.template.Template;
import com.intellij.codeInsight.template.TemplateManager;
import com.intellij.codeInsight.template.impl.ConstantNode;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import dev.verloren.midnight.ide.templates.CompactDeclarationNameGenerator;
import dev.verloren.midnight.ide.templates.CompactDeclarationType;
import dev.verloren.midnight.ide.templates.CompactTypeExpression;
import org.jetbrains.annotations.NotNull;

/**
 * Insert handler for Compact {@code ledger} declarations that constructs and launches
 * an interactive live template containing tab stops for the ledger state variable name and type.
 *
 * <h3>IntelliJ Platform Lifecycle &amp; Threading Contract</h3>
 * <ul>
 *   <li><b>Subsystem Role:</b> {@link InsertHandler} implementation configured on {@code ledger}
 *       and {@code export ledger} lookup items by {@link dev.verloren.midnight.completion.providers.CompactKeywordCompletionProvider}.</li>
 *   <li><b>Calling Pipeline:</b> Invoked directly on the Event Dispatch Thread (EDT) by
 *       {@code com.intellij.codeInsight.completion.CodeCompletionHandlerBase} immediately after the user
 *       commits the ledger keyword item from the completion lookup window.</li>
 *   <li><b>Threading Model:</b> Synchronous EDT execution within an existing Write Action command.</li>
 *   <li><b>Headless &amp; Test Fallback:</b> Automatically degrades to direct document text insertion
 *       ({@code " <name>: State;"}) when running in a headless test environment without an active {@link TemplateManager}.</li>
 * </ul>
 *
 * <h3>Design Decisions &amp; Architectural Rationale</h3>
 * <ul>
 *   <li><b>Singleton Pattern:</b> Instantiated as an immutable, stateless singleton ({@link #INSTANCE})
 *       with a private constructor to eliminate object allocation during completion lookup passes.</li>
 *   <li><b>Collision-Resistant Naming:</b> Uses {@link CompactDeclarationNameGenerator} to generate
 *       auto-numbered ledger names (e.g. {@code ledger1}, {@code ledger2}) that avoid collisions with
 *       existing bindings in the file.</li>
 *   <li><b>Type Placeholder:</b> Seeds the type field with a {@link CompactTypeExpression} pre-filled
 *       with {@code "State"}, which integrates with type completion popups.</li>
 *   <li><b>Memory Leak Guards:</b> Zero UI or PSI instances stored in fields; transiently accessed via
 *       {@link InsertionContext}.</li>
 * </ul>
 *
 * <h3>Template Lifecycle Pipeline</h3>
 * <pre>{@code
 *   handleInsert(context, item)
 *              │
 *              ├── Step 1: Deduplicate preceding "export " if already typed
 *              │
 *              ├── Step 2: Guard check: existing identifier or ':' follows caret?
 *              │     └─► Suffix present: insert trailing space only, exit
 *              │
 *              ▼
 *   insertTemplate(project, editor, tailOffset)
 *              │
 *              ├── Step 3: Suggest collision-free ledger name via generator
 *              ├── Step 4: Build template: " $NAME$: $TYPE$;"
 *              └── Step 5: Start template [or fallback document insert]
 * }</pre>
 *
 * @see CompactDeclarationInsertHandler
 * @see CompactDeclarationNameGenerator
 * @see TemplateManager
 */
public class CompactLedgerInsertHandler implements InsertHandler<LookupElement> {

  /** Shared stateless singleton instance for ledger declaration insertion. */
  public static final CompactLedgerInsertHandler INSTANCE = new CompactLedgerInsertHandler();

  /** Private constructor enforcing singleton usage. */
  private CompactLedgerInsertHandler() {
  }

  /**
   * Handles post-completion document mutation and starts the ledger declaration live template.
   *
   * @param context insertion context containing editor, document, and offset metadata
   * @param item    the selected ledger lookup element
   */
  @Override
  @SuppressWarnings("DuplicatedCode")
  public void handleInsert(@NotNull InsertionContext context, @NotNull LookupElement item) {
    Editor editor = context.getEditor();
    Document document = context.getDocument();
    int startOffset = context.getStartOffset();
    int tailOffset = context.getTailOffset();
    CharSequence chars = document.getCharsSequence();

    // Step 1: Deduplicate export prefix if 'export' was already preceding on the line
    // and the chosen lookup item starts with 'export' (e.g. user typed 'export' then selected 'export ledger').
    int lineStart = document.getLineStartOffset(document.getLineNumber(startOffset));
    boolean hasPrecedingExport = CompactCompletionContext.hasPrecedingExportOnLine(chars, lineStart, startOffset);
    boolean itemHasExport = item.getLookupString().startsWith("export");

    if (hasPrecedingExport && itemHasExport) {
      document.deleteString(startOffset, startOffset + "export ".length());
      tailOffset -= "export ".length();
      context.setTailOffset(tailOffset);
      editor.getCaretModel().moveToOffset(tailOffset);
      chars = document.getCharsSequence();
    }

    // Step 2: Guard against overwriting existing declarations.
    // If an identifier or colon already follows the caret on the current line
    // (e.g. developer typed 'ledger' in front of the existing 'myState: State; '),
    // do NOT launch a template over it; simply ensure a single space separator is present.
    int lineEnd = document.getLineEndOffset(document.getLineNumber(tailOffset));
    String lineSuffix = chars.subSequence(tailOffset, lineEnd).toString().trim();
    if (!lineSuffix.isEmpty() && (lineSuffix.matches("^[a-zA-Z_].*") || lineSuffix.startsWith(":"))) {
      if (tailOffset < chars.length() && !Character.isWhitespace(chars.charAt(tailOffset))) {
        document.insertString(tailOffset, " ");
        editor.getCaretModel().moveToOffset(tailOffset + 1);
      }
      return;
    }

    // Step 3: Launch the interactive ledger live template
    insertTemplate(context.getProject(), editor, tailOffset);
  }

  /**
   * Programmatically launches the live ledger template at {@code tailOffset} in the given editor.
   *
   * @param project    current project
   * @param editor     active editor
   * @param tailOffset document offset directly following the ledger keyword
   */
  public void insertTemplate(@NotNull Project project, @NotNull Editor editor, int tailOffset) {
    PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
    PsiElement psiContext = psiFile != null ? psiFile.findElementAt(Math.max(0, tailOffset - 1)) : null;
    String defaultName = CompactDeclarationNameGenerator.generateName(CompactDeclarationType.LEDGER, psiContext);

    TemplateManager templateManager = TemplateManager.getInstance(project);
    if (templateManager != null) {
      Template template = templateManager.createTemplate("", "");
      template.setToReformat(true);
      template.addTextSegment(" ");
      template.addVariable("NAME", new ConstantNode(defaultName), true);
      template.addTextSegment(": ");
      template.addVariable("TYPE", new CompactTypeExpression("State"), true);
      template.addTextSegment(";");
      templateManager.startTemplate(editor, template);
    } else {
      String insert = " " + defaultName + ": State;";
      editor.getDocument().insertString(tailOffset, insert);
      editor.getCaretModel().moveToOffset(tailOffset + insert.length());
    }
  }
}
