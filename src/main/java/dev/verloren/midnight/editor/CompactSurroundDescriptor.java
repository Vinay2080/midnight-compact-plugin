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
 * Descriptor that identifies enclosing statements and expressions eligible for "Surround With..."
 * actions (Ctrl+Alt+T / Cmd+Alt+T) in Compact source code.
 *
 * <h3>Architectural Role &amp; Extension Point</h3>
 * <p>Registered as a {@code lang.surroundDescriptor} extension for language {@code Compact} in {@code plugin.xml}.
 * Implements {@link SurroundDescriptor} to participate in IntelliJ's code surrounding action system. When a developer
 * selects a code fragment (or positions the caret on a statement) and invokes "Surround With", this descriptor:</p>
 * <ol>
 *   <li>Resolves the selected range into a normalized array of contiguous {@link PsiElement} statement or expression nodes.</li>
 *   <li>Filters out whitespace and structural braces that should not be enveloped.</li>
 *   <li>Supplies the available {@link Surrounder} implementations (e.g. {@code { ... }} block, {@code if (expr) { ... }}).</li>
 * </ol>
 *
 * <h3>Element Resolution Pipeline</h3>
 * <pre>
 *   Selection Range (startOffset, endOffset)
 *                     │
 *                     ▼
 *      [Clamp offsets to file boundaries]
 *                     │
 *            ┌────────┴────────┐
 *            │                 │
 *      Point Caret       Range Selection
 *            │                 │
 *            ▼                 ▼
 *    [Find leaf]      [Find leaf1 &amp; leaf2, skipping whitespace]
 *            │                 │
 *            ▼                 ▼
 *   [findSurround     [Resolve candidate1 &amp; candidate2]
 *     Candidate]               │
 *            │                 ▼
 *            │        [Compute common ancestor / direct children]
 *            │                 │
 *            │                 ▼
 *            │        [Collect contiguous sibling nodes]
 *            │                 │
 *            └────────┬────────┘
 *                     │
 *                     ▼
 *       PsiElement[] candidate elements to surround
 * </pre>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Read Action:</b> Invoked on EDT under a platform read lock. All AST and PSI traversals are read-only.</li>
 * </ul>
 *
 * @see CompactSurrounderBase
 * @see CompactBlockSurrounder
 * @see CompactIfSurrounder
 */
public class CompactSurroundDescriptor implements SurroundDescriptor {

  /**
   * Pre-allocated array of available surrounders for Compact statements.
   */
  private static final Surrounder[] SURROUNDERS = new Surrounder[]{
          new CompactIfSurrounder(),
          new CompactBlockSurrounder()
  };

  /**
   * Identifies and returns the array of PSI elements to be enclosed by a surrounder based on the
   * editor selection range or caret position.
   *
   * @param file the PSI file being edited
   * @param startOffset starting offset of the editor selection
   * @param endOffset ending offset of the editor selection
   * @return array of eligible {@link PsiElement} nodes, or {@link PsiElement#EMPTY_ARRAY} if none found
   */
  @Override
  public PsiElement @NotNull [] getElementsToSurround(PsiFile file, int startOffset, int endOffset) {
    // 1. Validate a file type and non-empty content
    if (!(file instanceof CompactFile) || file.getTextLength() == 0) {
      return PsiElement.EMPTY_ARRAY;
    }

    // 2. Clamp offsets within file text bounds
    startOffset = Math.clamp(startOffset, 0, file.getTextLength());
    endOffset = Math.clamp(endOffset, 0, file.getTextLength());

    // 3. Handle single point caret (no active text selection)
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

    // 4. Handle active text range selection: find boundary leaves
    PsiElement element1 = file.findElementAt(startOffset);
    PsiElement element2 = file.findElementAt(Math.max(0, endOffset - 1));

    // Skip leading and trailing whitespace tokens
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

    // 5. Locate candidate statements or expressions enclosing boundary leaves
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

    // 6. Harmonize parents: find a common parent if candidates reside at different depths
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

    // 7. Collect all contiguous siblings between cand1 and cand2, skipping whitespace and standalone braces
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

  /**
   * Returns the array of registered surrounder strategies.
   *
   * @return array of {@link Surrounder} implementations
   */
  @Override
  public Surrounder @NotNull [] getSurrounders() {
    return SURROUNDERS;
  }

  /**
   * Indicates whether this descriptor is exclusive, suppressing surrounders from other descriptors.
   *
   * @return {@code false} to permit co-existence with generic surrounders
   */
  @Override
  public boolean isExclusive() {
    return false;
  }

  /**
   * Climbs the PSI tree upward from a leaf element to locate the nearest statement, expression,
   * or block child suitable for surrounding.
   *
   * @param leaf the leaf element at or near the caret/selection
   * @return the candidate {@link PsiElement}, or {@code null} if none qualifies
   */
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

  /**
   * Climbs from {@code child} upward until reaching a node whose parent is {@code parent}.
   *
   * @param parent the ancestor element
   * @param child the descendant element
   * @return the direct child of {@code parent} that is an ancestor of {@code child}
   */
  private static PsiElement findDirectChildOf(@NotNull PsiElement parent, @NotNull PsiElement child) {
    PsiElement current = child;
    while (current != null && current.getParent() != parent) {
      current = current.getParent();
    }
    return current;
  }

  /**
   * Checks whether the given PSI element represents an isolated opening or closing curly brace token.
   *
   * @param elem the PSI element to test
   * @return {@code true} if {@code elem} is an {@code LBRACE} or {@code RBRACE} token; {@code false} otherwise
   */
  private static boolean isBraceToken(@NotNull PsiElement elem) {
    if (elem.getNode() == null) return false;
    return elem.getNode().getElementType() == CompactTokenTypes.LBRACE ||
            elem.getNode().getElementType() == CompactTokenTypes.RBRACE;
  }
}
