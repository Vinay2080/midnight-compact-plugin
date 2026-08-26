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
 * Base class for statement surrounders providing common document replacement and reformatting.
 */
public abstract class CompactSurrounderBase implements Surrounder {

  @Override
  public boolean isApplicable(PsiElement @NotNull [] elements) {
    return elements.length > 0;
  }

  @Override
  public @Nullable TextRange surroundElements(@NotNull Project project,
                                              @NotNull Editor editor,
                                              PsiElement @NotNull [] elements) throws IncorrectOperationException {
    if (elements.length == 0) {
      return null;
    }

    PsiFile file = elements[0].getContainingFile();
    int startOffset = elements[0].getTextRange().getStartOffset();
    int endOffset = elements[elements.length - 1].getTextRange().getEndOffset();

    Document document = editor.getDocument();
    String selectedText = document.getText(new TextRange(startOffset, endOffset)).trim();

    String replacement = generateReplacement(selectedText);
    document.replaceString(startOffset, endOffset, replacement);

    PsiDocumentManager.getInstance(project).commitDocument(document);

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

    return postProcess(editor, document, startOffset, replacement);
  }

  protected abstract @NotNull String generateReplacement(@NotNull String selectedText);

  protected @Nullable TextRange postProcess(@NotNull Editor editor,
                                            @NotNull Document document,
                                            int startOffset,
                                            @NotNull String replacement) {
    return new TextRange(startOffset, startOffset + replacement.length());
  }
}
