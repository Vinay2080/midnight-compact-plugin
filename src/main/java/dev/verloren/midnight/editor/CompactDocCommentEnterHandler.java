package dev.verloren.midnight.editor;

import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegateAdapter;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiFile;
import dev.verloren.midnight.CompactLanguage;
import org.jetbrains.annotations.NotNull;

/**
 * Handles Enter key continuation inside Compact block comments ({@code /* ... *&#47;}).
 *
 * <p>Documentation comments ({@code /** ... *&#47;}) are handled natively by IntelliJ's platform
 * via {@link CompactCommenter} (which implements {@link com.intellij.lang.CodeDocumentationAwareCommenter}).</p>
 */
public class CompactDocCommentEnterHandler extends EnterHandlerDelegateAdapter {

  @Override
  public Result postProcessEnter(@NotNull PsiFile file, @NotNull Editor editor, @NotNull DataContext dataContext) {
    if (!file.getLanguage().isKindOf(CompactLanguage.INSTANCE)) {
      return Result.Continue;
    }

    Document document = editor.getDocument();
    int offset = editor.getCaretModel().getOffset();
    int lineNumber = document.getLineNumber(offset);
    if (lineNumber == 0) {
      return Result.Continue;
    }

    int lineStart = document.getLineStartOffset(lineNumber);
    int prevLineStart = document.getLineStartOffset(lineNumber - 1);
    int prevLineEnd = document.getLineEndOffset(lineNumber - 1);
    String prevLine = document.getCharsSequence().subSequence(prevLineStart, prevLineEnd).toString();
    String trimmedPrev = prevLine.trim();

    int lineEnd = document.getLineEndOffset(lineNumber);
    String currentLine = document.getCharsSequence().subSequence(lineStart, lineEnd).toString();
    String trimmedCurrent = currentLine.trim();

    // 1. If the previous line closed a comment and did NOT open a new one after '*/', do nothing
    int lastOpen = trimmedPrev.lastIndexOf("/*");
    int lastClose = trimmedPrev.lastIndexOf("*/");
    if (lastClose != -1 && (lastOpen == -1 || lastClose > lastOpen)) {
      return Result.Continue;
    }

    String openFragment = lastOpen != -1 ? trimmedPrev.substring(lastOpen) : "";

    // 2. If the platform (CodeDocumentationAwareCommenter) already handled the enter action
    // (e.g. for '/**', '/***', or continuation lines starting with '*'),
    // do NOT touch it! The platform's generation is already complete and clean.
    if ((openFragment.startsWith("/**") && trimmedCurrent.startsWith("*"))
        || (openFragment.isEmpty() && trimmedCurrent.startsWith("*"))) {
      return Result.Continue;
    }

    String indent = document.getCharsSequence().subSequence(lineStart, offset).toString();

    // 3. User opened a comment on the previous line ('/*' or '/**' where the platform didn't handle it)
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

    // 4. Continuing an unprefixed '*' line inside a plain block comment '/* ... */'
    // where the platform did not already insert an asterisk.
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

  private static boolean hasClosingCommentBelow(Document document, int fromLine) {
    int lineCount = document.getLineCount();
    for (int i = fromLine; i < lineCount; i++) {
      int start = document.getLineStartOffset(i);
      int end = document.getLineEndOffset(i);
      String line = document.getCharsSequence().subSequence(start, end).toString().trim();
      if (line.contains("*/")) {
        return true;
      }
      // If we encounter another comment, start before finding a closing '*/', stop
      if (line.startsWith("/*") || line.startsWith("//")) {
        return false;
      }
    }
    return false;
  }
}
