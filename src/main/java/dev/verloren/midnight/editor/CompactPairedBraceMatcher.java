package dev.verloren.midnight.editor;

import com.intellij.lang.BracePair;
import com.intellij.lang.PairedBraceMatcher;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Registers paired delimiter tokens ({@code {}}, {@code ()}, {@code []}, {@code <>}) for Compact.
 *
 * <p>Enables editor delimiter highlighting, auto-insertion of closing braces, and structural navigation.</p>
 */
public class CompactPairedBraceMatcher implements PairedBraceMatcher {
  private static final BracePair[] PAIRS = new BracePair[]{
      new BracePair(CompactTokenTypes.LBRACE, CompactTokenTypes.RBRACE, true),
      new BracePair(CompactTokenTypes.LPAREN, CompactTokenTypes.RPAREN, false),
      new BracePair(CompactTokenTypes.LBRACKET, CompactTokenTypes.RBRACKET, false),
      new BracePair(CompactTokenTypes.LT, CompactTokenTypes.GT, false)
  };

  @Override
  public BracePair @NotNull [] getPairs() {
    return PAIRS;
  }

  @Override
  public boolean isPairedBracesAllowedBeforeType(@NotNull IElementType lbraceType, @Nullable IElementType contextType) {
    return true;
  }

  @Override
  public int getCodeConstructStart(PsiFile file, int openingBraceOffset) {
    return openingBraceOffset;
  }
}
