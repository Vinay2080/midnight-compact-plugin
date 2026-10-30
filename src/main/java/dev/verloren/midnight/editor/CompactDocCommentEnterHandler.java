package dev.verloren.midnight.editor;

import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegateAdapter;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiFile;
import dev.verloren.midnight.CompactLanguage;
import org.jetbrains.annotations.NotNull;

/**
 * Editor enter-handler delegate that provides intelligent continuation and automatic closing
 * for Compact multi-line block comments ({@code /* ... *&#47;}).
 *
 * <h3>Architectural Role &amp; Extension Point</h3>
 * <p>Registered as an {@code enterHandlerDelegate} extension in {@code plugin.xml}. In the IntelliJ
 * platform architecture, documentation comments ({@code /** ... *&#47;}) are handled automatically
 * by platform core mechanics when a {@link com.intellij.lang.CodeDocumentationAwareCommenter}
 * is registered. However, standard C-style block comments ({@code /* ... *&#47;}) do not receive
 * automatic asterisk continuation or delimiter completion by default. This delegate bridges that gap,
 * ensuring seamless developer ergonomics when writing either doc comments or standard block comments.</p>
 *
 * <h3>Comment Continuation Logic</h3>
 * <pre>
 *   Keystroke [Enter] on EDT (newline inserted)
 *                 │
 *                 ▼
 *   [CompactDocCommentEnterHandler.postProcessEnter]
 *                 │
 *                 ├─► Previous line closed comment ('*&#47;') without re-opening? ─► YES ─► Continue
 *                 ├─► Platform already prefixed current line with '*'? ────────► YES ─► Continue
 *                 │
 *                 ├─► Previous line opened '/*' (unclosed)?
 *                 │     ├─► Current line already has '*&#47;'? ─► Insert '*' and move caret
 *                 │     ├─► Has '*&#47;' below in the file? ────────► Insert '*' and move caret
 *                 │     └─► No '*&#47;' below in the file? ─────────► Insert "* \n" + indent + " *&#47;"
 *                 │
 *                 └─► Previous line had '*' prefix &amp; current line doesn't?
 *                       └─► Prepend '*' and move caret
 * </pre>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>EDT (Event Dispatch Thread):</b> Invoked synchronously on the EDT by the editor action system
 *       within an active write command after the Enter key has been processed.</li>
 *   <li><b>Document Mutations:</b> Document modifications occur directly within the surrounding command
 *       lifecycle managed by IntelliJ's editor infrastructure.</li>
 * </ul>
 *
 * @see CompactCommenter
 */
public class CompactDocCommentEnterHandler extends EnterHandlerDelegateAdapter {

  /**
   * Post-processes document text immediately after an Enter keypress has inserted a newline.
   *
   * <p>Inspects the previous line to determine if an unclosed block comment was opened or if a block
   * comment line is being continued, inserting an aligned asterisk prefix and closing delimiter as appropriate.</p>
   *
   * @param file the PSI file being edited
   * @param editor the active editor instance
   * @param dataContext editor data context
   * @return {@link Result#Stop} if comment syntax was injected and the caret repositioned; {@link Result#Continue} otherwise
   */
  @Override
  public Result postProcessEnter(@NotNull PsiFile file, @NotNull Editor editor, @NotNull DataContext dataContext) {
    // 1. Language boundary check: only process Compact source files
    if (!file.getLanguage().isKindOf(CompactLanguage.INSTANCE)) {
      return Result.Continue;
    }

    Document document = editor.getDocument();
    int offset = editor.getCaretModel().getOffset();
    int lineNumber = document.getLineNumber(offset);
    if (lineNumber == 0) {
      return Result.Continue;
    }

    // 2. Extract content of the previous line and current line
    int lineStart = document.getLineStartOffset(lineNumber);
    int prevLineStart = document.getLineStartOffset(lineNumber - 1);
    int prevLineEnd = document.getLineEndOffset(lineNumber - 1);
    String prevLine = document.getCharsSequence().subSequence(prevLineStart, prevLineEnd).toString();
    String trimmedPrev = prevLine.trim();

    int lineEnd = document.getLineEndOffset(lineNumber);
    String currentLine = document.getCharsSequence().subSequence(lineStart, lineEnd).toString();
    String trimmedCurrent = currentLine.trim();

    // 3. If the previous line closed a comment and did not open a new one after '*/', do nothing
    int lastOpen = trimmedPrev.lastIndexOf("/*");
    int lastClose = trimmedPrev.lastIndexOf("*/");
    if (lastClose != -1 && (lastOpen == -1 || lastClose > lastOpen)) {
      return Result.Continue;
    }

    String openFragment = lastOpen != -1 ? trimmedPrev.substring(lastOpen) : "";

    // 4. If the platform (CodeDocumentationAwareCommenter) already handled the enter action
    // (e.g. for '/**', '/***', or continuation lines starting with '*'), do NOT touch it
    if ((openFragment.startsWith("/**") && trimmedCurrent.startsWith("*"))
        || (openFragment.isEmpty() && trimmedCurrent.startsWith("*"))) {
      return Result.Continue;
    }

    String indent = document.getCharsSequence().subSequence(lineStart, offset).toString();

    // 5. User opened a comment on the previous line ('/*' or '/**' where the platform didn't handle it)
    if (openFragment.startsWith("/*")) {
      // Check if there is already a closing '*/' on the current line
      if ("*/".equals(trimmedCurrent) || trimmedCurrent.endsWith("*/")) {
        document.insertString(offset, " * ");
        editor.getCaretModel().moveToOffset(offset + 3);
        return Result.Stop;
      }

      // Check if a closing '*/' exists further down in this comment block
      boolean hasClosingBelow = hasClosingCommentBelow(document, lineNumber);
      if (!hasClosingBelow) {
        document.insertString(offset, " * \n" + indent + " */");
        editor.getCaretModel().moveToOffset(offset + 3);
      } else {
        document.insertString(offset, " * ");
        editor.getCaretModel().moveToOffset(offset + 3);
      }
      return Result.Stop;
    }

    // 6. Continuing an unprefixed '*' line inside a plain block comment '/* ... */'
    // where the platform did not already insert an asterisk
    if (trimmedPrev.startsWith("*") && !trimmedCurrent.startsWith("*")) {
      int insertionOffset = offset;
      if (prevLine.endsWith("* ")) {
        document.deleteString(prevLineEnd - 1, prevLineEnd);
        insertionOffset--;
      }
      document.insertString(insertionOffset, "* ");
      editor.getCaretModel().moveToOffset(insertionOffset + 2);
      return Result.Stop;
    }

    return Result.Continue;
  }

  /**
   * Scans forward through lines in the document starting at {@code fromLine} to determine if a
   * closing block-comment delimiter ({@code "*&#47;"}) exists before encountering another comment boundary.
   *
   * @param document the editor document
   * @param fromLine zero-based line index from which to begin scanning downward
   * @return {@code true} if an unclosed block comment has a matching closing delimiter downstream; {@code false} otherwise
   */
  private static boolean hasClosingCommentBelow(Document document, int fromLine) {
    int lineCount = document.getLineCount();
    for (int i = fromLine; i < lineCount; i++) {
      int start = document.getLineStartOffset(i);
      int end = document.getLineEndOffset(i);
      String line = document.getCharsSequence().subSequence(start, end).toString().trim();
      if (line.contains("*/")) {
        return true;
      }
      // If we encounter another comment, start before finding a closing '*/', stop scanning
      if (line.startsWith("/*") || line.startsWith("//")) {
        return false;
      }
    }
    return false;
  }
}
