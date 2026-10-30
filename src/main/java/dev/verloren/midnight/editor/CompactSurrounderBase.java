package dev.verloren.midnight.editor;

import com.intellij.lang.surroundWith.Surrounder;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Abstract base class providing common document transformation, code style reformatting, and caret
 * positioning mechanics for Compact statement surrounders.
 *
 * <h3>Architectural Role</h3>
 * <p>Implements the {@link Surrounder} interface to standardize how statement-level surround actions
 * modify the editor document. Subclasses define only the template title and the text generation logic
 * via {@link #generateReplacement(String)}, while this base class coordinates the document replacement,
 * PSI re-commit, code style indentation formatting, and post-surround caret or selection adjustment.</p>
 *
 * <h3>Surround Execution Lifecycle</h3>
 * <pre>
 *   User Selects "Surround With..." Option on EDT
 *                         │
 *                         ▼
 *         [CompactSurrounderBase.surroundElements]
 *                         │
 *                         ├─► Calculate startOffset &amp; endOffset from elements
 *                         ├─► Extract selectedText from Document
 *                         │
 *                         ▼
 *         [generateReplacement(selectedText)]
 *                         │
 *                         ▼
 *         [document.replaceString(startOffset, endOffset, replacement)]
 *                         │
 *                         ▼
 *         [PsiDocumentManager.commitDocument]
 *                         │
 *                         ▼
 *         [CodeStyleManager.reformatText]
 *                         │
 *                         ▼
 *         [postProcess(editor, document, startOffset, replacement)]
 *                         │
 *                         ▼
 *             Return target TextRange
 * </pre>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>EDT (Event Dispatch Thread):</b> Invoked synchronously on the EDT by the IntelliJ action system.</li>
 *   <li><b>Write Action:</b> Runs within an active write command action managed by the platform.</li>
 * </ul>
 *
 * @see CompactSurroundDescriptor
 * @see CompactBlockSurrounder
 * @see CompactIfSurrounder
 */
public abstract class CompactSurrounderBase implements Surrounder {

  /**
   * Checks whether this surrounder can be applied to the given array of PSI elements.
   *
   * @param elements the candidate PSI elements selected for surrounding
   * @return {@code true} if at least one element is provided; {@code false} otherwise
   */
  @Override
  public boolean isApplicable(PsiElement @NotNull [] elements) {
    return elements.length > 0;
  }

  /**
   * Encloses the specified elements within the surrounding template syntax, reformats the affected
   * document range according to Compact code style settings, and delegates to {@link #postProcess}
   * for caret/selection positioning.
   *
   * @param project the current project context
   * @param editor the active editor instance
   * @param elements array of PSI elements to surround
   * @return the text range to select or position the caret at, or {@code null} if aborted
   * @throws IncorrectOperationException if an illegal document modification occurs
   */
  @Override
  public @Nullable TextRange surroundElements(@NotNull Project project,
                                              @NotNull Editor editor,
                                              PsiElement @NotNull [] elements) throws IncorrectOperationException {
    // 1. Guard against an empty element array
    if (elements.length == 0) {
      return null;
    }

    // 2. Compute the full span covered by the elements
    PsiFile file = elements[0].getContainingFile();
    int startOffset = elements[0].getTextRange().getStartOffset();
    int endOffset = elements[elements.length - 1].getTextRange().getEndOffset();

    // 3. Extract the original selected text
    Document document = editor.getDocument();
    String selectedText = document.getText(new TextRange(startOffset, endOffset)).trim();

    // 4. Generate a replacement string via concrete subclass implementation
    String replacement = generateReplacement(selectedText);
    document.replaceString(startOffset, endOffset, replacement);

    // 5. Commit document changes to synchronize the PSI tree with the document buffer
    PsiDocumentManager.getInstance(project).commitDocument(document);

    // 6. Automatically reformat the newly inserted code block using Compact code style rules
    if (file != null && file.isValid()) {
      try {
        CodeStyleManager.getInstance(project).reformatText(
                file,
                startOffset,
                startOffset + replacement.length()
        );
      } catch (Exception ignored) {
      }
    }

    // 7. Execute post-processing for caret placement or template condition highlighting
    return postProcess(editor, document, startOffset, replacement);
  }

  /**
   * Generates the replacement string enveloping the given selected text block.
   *
   * @param selectedText the original trimmed text of the surrounded elements
   * @return the transformed code snippet
   */
  protected abstract @NotNull String generateReplacement(@NotNull String selectedText);

  /**
   * Post-processes the editor after replacement and reformatting, positioning the caret or setting
   * an active selection range (e.g. highlighting condition placeholders).
   *
   * @param editor the active editor instance
   * @param document the modified editor document
   * @param startOffset starting offset of the replacement in the document
   * @param replacement the replacement string inserted
   * @return the final target text range to select or position caret at
   */
  protected @Nullable TextRange postProcess(@NotNull Editor editor,
                                            @NotNull Document document,
                                            int startOffset,
                                            @NotNull String replacement) {
    return new TextRange(startOffset, startOffset + replacement.length());
  }
}
