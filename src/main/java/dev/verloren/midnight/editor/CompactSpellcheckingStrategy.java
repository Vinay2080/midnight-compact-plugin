package dev.verloren.midnight.editor;

import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import com.intellij.spellchecker.tokenizer.SpellcheckingStrategy;
import com.intellij.spellchecker.tokenizer.Tokenizer;
import dev.verloren.midnight.lexer.CompactTokenSets;
import dev.verloren.midnight.psi.CompactLiteralExprImpl;
import dev.verloren.midnight.psi.CompactReferenceExpr;
import org.jetbrains.annotations.NotNull;

/**
 * Provides spellchecking support for Compact comments, string literals, and identifiers.
 */
public class CompactSpellcheckingStrategy extends SpellcheckingStrategy {
  @Override
  public @NotNull Tokenizer<?> getTokenizer(PsiElement element) {
    if (element instanceof PsiComment) {
      return TEXT_TOKENIZER;
    }
    if (element instanceof CompactLiteralExprImpl) {
      String text = element.getText();
      if (text.startsWith("\"") || text.startsWith("'")) {
        return TEXT_TOKENIZER;
      }
      return EMPTY_TOKENIZER;
    }
    if (element instanceof PsiNamedElement || element instanceof CompactReferenceExpr) {
      return TEXT_TOKENIZER;
    }
    if (element != null && CompactTokenSets.KEYWORDS.contains(element.getNode().getElementType())) {
      return EMPTY_TOKENIZER;
    }
    return super.getTokenizer(element);
  }
}
