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
 * Universal insert handler for top-level Compact declarations that initializes and launches
 * an interactive live template containing tab stops for declaration names, parameter lists,
 * return types, and code block bodies.
 *
 * <h3>IntelliJ Platform Lifecycle &amp; Threading Contract</h3>
 * <ul>
 *   <li><b>Subsystem Role:</b> {@link InsertHandler} implementation attached to top-level declaration
 *       lookup elements (e.g. {@code circuit}, {@code witness}, {@code struct}, {@code enum}, {@code contract})
 *       created by {@link dev.verloren.midnight.completion.providers.CompactKeywordCompletionProvider}.</li>
 *   <li><b>Calling Pipeline:</b> Invoked on the Event Dispatch Thread (EDT) by IntelliJ's
 *       {@code com.intellij.codeInsight.completion.CodeCompletionHandlerBase} immediately after the user
 *       commits a declaration keyword item from the completion lookup window.</li>
 *   <li><b>Threading Model:</b> Synchronous EDT execution within an existing Write Action. Integrates
 *       with {@link TemplateManager} to launch interactive multi-variable live templates.</li>
 *   <li><b>Headless &amp; Test Fallback:</b> Detects when {@link TemplateManager} is unavailable (e.g., in unit
 *       test or headless execution environments) and gracefully falls back to injecting static skeleton text
 *       via {@link #buildFallbackString(String)}.</li>
 * </ul>
 *
 * <h3>Design Decisions &amp; Architectural Rationale</h3>
 * <ul>
 *   <li><b>Java Record Immutability:</b> Implemented as a concise, immutable record parameterized by
 *       {@link CompactDeclarationType}, making handler instances reusable across completion items.</li>
 *   <li><b>Dynamic Collision-Resistant Naming:</b> Collaborates with {@link CompactDeclarationNameGenerator}
 *       to inspect the surrounding PSI scope and propose unique, non-colliding default names
 *       (e.g., {@code circuit1}, {@code witness2}).</li>
 *   <li><b>Memory Leak Guards:</b> Zero storage of {@link Project}, {@link Editor}, or {@link PsiFile}
 *       references in record components.</li>
 * </ul>
 *
 * <h3>Insertion &amp; Template Lifecycle Pipeline</h3>
 * <pre>{@code
 *   handleInsert(context, item)
 *              │
 *              ├── Step 1: Deduplicate preceding "export " if already typed on line
 *              │
 *              ├── Step 2: Guard check: existing identifier / signature follows caret?
 *              │     └─► Suffix present: insert trailing space only, exit without template
 *              │
 *              ▼
 *   insertTemplate(project, editor, tailOffset)
 *              │
 *              ├── Step 3: Resolve collision-free declaration name via CompactDeclarationNameGenerator
 *              ├── Step 4: Construct Live Template with tab stops (NAME -> PARAMS -> RET -> BODY)
 *              └── Step 5: TemplateManager.startTemplate() [or fallback document insertion]
 * }</pre>
 *
 * @param declarationType the target Compact declaration construct (circuit, struct, ledger, etc.)
 * @see CompactDeclarationType
 * @see CompactDeclarationNameGenerator
 * @see TemplateManager
 * @see Template
 */
public record CompactDeclarationInsertHandler(@NotNull CompactDeclarationType declarationType) implements InsertHandler<LookupElement> {

  /**
   * Handles post-completion document mutation and starts the declaration live template.
   *
   * @param context insertion context containing editor, document, and offset information
   * @param item    the selected lookup element
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
    // and the chosen lookup item itself starts with 'export' (e.g. user typed 'export' then picked 'export circuit').
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

    // Step 2: Guard against overwriting or mangling existing declarations.
    // If an identifier, parameter list, brace, or colon already follows the caret on the current line
    // (e.g. developer typed 'circuit' in front of the existing 'myFunc (): Void { ... }'),
    // do NOT launch a template over it; simply ensure a single space separator is present.
    int lineEnd = document.getLineEndOffset(document.getLineNumber(tailOffset));
    String lineSuffix = chars.subSequence(tailOffset, lineEnd).toString().trim();
    if (!lineSuffix.isEmpty() && (lineSuffix.matches("^[a-zA-Z_].*")
        || lineSuffix.startsWith("(")
        || lineSuffix.startsWith("{")
        || lineSuffix.startsWith(":"))) {
      if (tailOffset < chars.length() && !Character.isWhitespace(chars.charAt(tailOffset))) {
        document.insertString(tailOffset, " ");
        editor.getCaretModel().moveToOffset(tailOffset + 1);
      }
      return;
    }

    // Step 3: Launch the interactive declaration live template
    insertTemplate(context.getProject(), editor, tailOffset);
  }

  /**
   * Programmatically launches the live declaration template at {@code tailOffset} in the given editor.
   *
   * @param project    current project
   * @param editor     active editor
   * @param tailOffset document offset directly following the declaration keyword
   */
  public void insertTemplate(@NotNull Project project, @NotNull Editor editor, int tailOffset) {
    PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
    PsiElement psiContext = psiFile != null ? psiFile.findElementAt(Math.max(0, tailOffset - 1)) : null;
    String suggestedName = CompactDeclarationNameGenerator.generateName(declarationType, psiContext);

    TemplateManager templateManager = TemplateManager.getInstance(project);
    if (templateManager != null) {
      Template template = templateManager.createTemplate("", "");
      template.setToReformat(true);
      buildTemplate(template, suggestedName);
      templateManager.startTemplate(editor, template);
    } else {
      String fallback = buildFallbackString(suggestedName);
      editor.getDocument().insertString(tailOffset, fallback);
      editor.getCaretModel().moveToOffset(tailOffset + fallback.length());
    }
  }

  /**
   * Constructs the template structure, variables, and tab-stop segments appropriate for {@link #declarationType}.
   *
   * @param template      the mutable live template builder
   * @param suggestedName the contextual default declaration name
   */
  private void buildTemplate(@NotNull Template template, @NotNull String suggestedName) {
    switch (declarationType) {
      case CIRCUIT -> {
        template.addTextSegment(" ");
        template.addVariable("NAME", new ConstantNode(suggestedName), true);
        template.addTextSegment("(");
        template.addVariable("PARAMS", new ConstantNode(""), true);
        template.addTextSegment("): ");
        template.addVariable("RET", new CompactTypeExpression("Void"), true);
        template.addTextSegment(" {\n  ");
        template.addEndVariable();
        template.addTextSegment("\n}");
      }
      case WITNESS -> {
        template.addTextSegment(" ");
        template.addVariable("NAME", new ConstantNode(suggestedName), true);
        template.addTextSegment("(");
        template.addVariable("PARAMS", new ConstantNode(""), true);
        template.addTextSegment("): ");
        template.addVariable("RET", new CompactTypeExpression("Field"), true);
        template.addTextSegment(";");
      }
      case STRUCT, ENUM, MODULE, CONTRACT -> {
        template.addTextSegment(" ");
        template.addVariable("NAME", new ConstantNode(suggestedName), true);
        template.addTextSegment(" {\n  ");
        template.addEndVariable();
        template.addTextSegment("\n}");
      }
      case TYPE -> {
        template.addTextSegment(" ");
        template.addVariable("NAME", new ConstantNode(suggestedName), true);
        template.addTextSegment(" = ");
        template.addVariable("TYPE", new CompactTypeExpression("Field"), true);
        template.addTextSegment(";");
      }
      case CONST -> {
        template.addTextSegment(" ");
        template.addVariable("NAME", new ConstantNode(suggestedName), true);
        template.addTextSegment(": ");
        template.addVariable("TYPE", new CompactTypeExpression("Field"), true);
        template.addTextSegment(" = ");
        template.addVariable("VALUE", new ConstantNode("0"), true);
        template.addTextSegment(";");
      }
      case LEDGER -> {
        template.addTextSegment(" ");
        template.addVariable("NAME", new ConstantNode(suggestedName), true);
        template.addTextSegment(": ");
        template.addVariable("TYPE", new CompactTypeExpression("State"), true);
        template.addTextSegment(";");
      }
    }
  }

  /**
   * Generates a static declaration snippet used when {@link TemplateManager} is unavailable.
   *
   * @param suggestedName default declaration name
   * @return formatted skeleton string
   */
  private @NotNull String buildFallbackString(@NotNull String suggestedName) {
    return switch (declarationType) {
      case CIRCUIT -> " " + suggestedName + "(): Void {\n  \n}";
      case WITNESS -> " " + suggestedName + "(): Field;";
      case STRUCT, ENUM, MODULE, CONTRACT -> " " + suggestedName + " {\n  \n}";
      case TYPE -> " " + suggestedName + " = Field;";
      case CONST -> " " + suggestedName + ": Field = 0;";
      case LEDGER -> " " + suggestedName + ": State;";
    };
  }
}
