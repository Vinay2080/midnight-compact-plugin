package dev.verloren.midnight.editor;

import com.intellij.lang.surroundWith.SurroundDescriptor;
import com.intellij.lang.surroundWith.Surrounder;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import dev.verloren.midnight.psi.CompactBlock;
import dev.verloren.midnight.psi.CompactExpression;
import dev.verloren.midnight.psi.CompactFile;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides statement and block surrounding descriptors for Compact.
 */
public class CompactSurroundDescriptor implements SurroundDescriptor {
  private static final Surrounder[] SURROUNDERS = new Surrounder[]{
          new CompactIfSurrounder(),
          new CompactBlockSurrounder()
  };

  @Override
  public PsiElement @NotNull [] getElementsToSurround(PsiFile file, int startOffset, int endOffset) {
    if (!(file instanceof CompactFile) || file.getTextLength() == 0) {
      return PsiElement.EMPTY_ARRAY;
    }

    // Clamp offsets
    startOffset = Math.clamp(startOffset, 0, file.getTextLength());
    endOffset = Math.clamp(endOffset, 0, file.getTextLength());

    // Caret at single point
    if (startOffset == endOffset) {
      PsiElement element = file.findElementAt(startOffset);
      if (element instanceof PsiWhiteSpace || element == null) {
        element = file.findElementAt(Math.max(0, startOffset - 1));
      }
      if (element == null) {
        return PsiElement.EMPTY_ARRAY;
      }
      PsiElement candidate = findSurroundCandidate(element);
      if (candidate != null) {
        return new PsiElement[]{candidate};
      }
      return PsiElement.EMPTY_ARRAY;
    }

    // Range selection
    PsiElement element1 = file.findElementAt(startOffset);
    PsiElement element2 = file.findElementAt(Math.max(0, endOffset - 1));

    if (element1 instanceof PsiWhiteSpace) {
      int next = element1.getTextRange().getEndOffset();
      if (next < file.getTextLength()) {
        element1 = file.findElementAt(next);
      }
    }
    if (element2 instanceof PsiWhiteSpace) {
      int prev = Math.max(0, element2.getTextRange().getStartOffset() - 1);
      element2 = file.findElementAt(prev);
    }

    if (element1 == null || element2 == null) {
      return PsiElement.EMPTY_ARRAY;
    }

    PsiElement cand1 = findSurroundCandidate(element1);
    PsiElement cand2 = findSurroundCandidate(element2);

    if (cand1 == null && cand2 == null) {
      return PsiElement.EMPTY_ARRAY;
    }
    if (cand1 == null) cand1 = cand2;
    if (cand2 == null) cand2 = cand1;

    if (cand1 == cand2) {
      return new PsiElement[]{cand1};
    }

    PsiElement parent = cand1.getParent();
    if (parent == null || parent != cand2.getParent()) {
      PsiElement commonParent = PsiTreeUtil.findCommonParent(cand1, cand2);
      if (commonParent == null) {
        return new PsiElement[]{cand1};
      }
      cand1 = findDirectChildOf(commonParent, cand1);
      cand2 = findDirectChildOf(commonParent, cand2);
      if (cand1 == null || cand2 == null) {
        return new PsiElement[]{cand1 != null ? cand1 : cand2};
      }
    }

    List<PsiElement> statements = new ArrayList<>();
    PsiElement current = cand1;
    while (current != null) {
      if (!(current instanceof PsiWhiteSpace) && !isBraceToken(current)) {
        statements.add(current);
      }
      if (current == cand2) {
        break;
      }
      current = current.getNextSibling();
    }

    return statements.isEmpty() ? PsiElement.EMPTY_ARRAY : statements.toArray(PsiElement.EMPTY_ARRAY);
  }

  @Override
  public Surrounder @NotNull [] getSurrounders() {
    return SURROUNDERS;
  }

  @Override
  public boolean isExclusive() {
    return false;
  }

  private static PsiElement findSurroundCandidate(@NotNull PsiElement leaf) {
    if (isBraceToken(leaf)) {
      return null;
    }

    PsiElement current = leaf;
    while (current != null && !(current instanceof PsiFile)) {
      PsiElement parent = current.getParent();
      if (parent instanceof CompactBlock) {
        if (!isBraceToken(current)) {
          return current;
        }
      }
      current = parent;
    }

    current = leaf;
    while (current != null && !(current instanceof PsiFile)) {
      if (current instanceof CompactExpression || current.getParent() instanceof CompactBlock || current.getParent() instanceof CompactFile) {
        if (!isBraceToken(current) && !(current instanceof PsiComment)) {
          return current;
        }
      }
      current = current.getParent();
    }

    return null;
  }

  private static PsiElement findDirectChildOf(@NotNull PsiElement parent, @NotNull PsiElement child) {
    PsiElement current = child;
    while (current != null && current.getParent() != parent) {
      current = current.getParent();
    }
    return current;
  }

  private static boolean isBraceToken(@NotNull PsiElement elem) {
    if (elem.getNode() == null) return false;
    return elem.getNode().getElementType() == CompactTokenTypes.LBRACE ||
            elem.getNode().getElementType() == CompactTokenTypes.RBRACE;
  }
}
