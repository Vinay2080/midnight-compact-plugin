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
 * Configures spellchecking tokenization strategies for comments, string literals, and identifiers in Compact.
 *
 * <h3>Architectural Role &amp; Extension Point</h3>
 * <p>Registered as a {@code spellchecker.support} extension for language {@code Compact} in {@code plugin.xml}.
 * Implements {@link SpellcheckingStrategy} to integrate with IntelliJ's Grazi / Grazie-lite spellchecking engine.
 * Selects appropriate tokenizers for PSI elements, ensuring that natural language text in comments and string
 * literals is spellchecked (with word-boundary splitting on camelCase and snake_case identifiers) while keywords
 * and numeric literals are ignored.</p>
 *
 * <h3>Tokenization Strategy Matrix</h3>
 * <table border="1">
 *   <caption>Spellchecking Tokenizer Routing</caption>
 *   <tr><th>PSI Element Type</th><th>Tokenizer Strategy</th><th>Rationale</th></tr>
 *   <tr><td>{@link PsiComment}</td><td>{@link #TEXT_TOKENIZER}</td><td>Comments contain prose/documentation requiring spellchecking.</td></tr>
 *   <tr><td>{@link CompactLiteralExprImpl} (String)</td><td>{@link #TEXT_TOKENIZER}</td><td>String literals ({@code "..."} / {@code '...'}) contain user-facing messages.</td></tr>
 *   <tr><td>{@link CompactLiteralExprImpl} (Numeric/Other)</td><td>{@link #EMPTY_TOKENIZER}</td><td>Numeric, boolean, and hex literals do not contain natural language words.</td></tr>
 *   <tr><td>{@link PsiNamedElement} / {@link CompactReferenceExpr}</td><td>{@link #TEXT_TOKENIZER}</td><td>Identifiers are split into sub-words (camelCase/snake_case) for dictionary checking.</td></tr>
 *   <tr><td>Keywords ({@link CompactTokenSets#KEYWORDS})</td><td>{@link #EMPTY_TOKENIZER}</td><td>Language keywords (e.g. {@code circuit}, {@code witness}, {@code disclose}) are reserved syntax.</td></tr>
 * </table>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Read Action:</b> Invoked on background inspection threads under a platform read lock.</li>
 *   <li><b>Thread Safety:</b> Stateless and thread-safe.</li>
 * </ul>
 *
 * @see SpellcheckingStrategy
 */
public class CompactSpellcheckingStrategy extends SpellcheckingStrategy {

  /**
   * Selects and returns the appropriate {@link Tokenizer} for spellchecking the specified PSI element.
   *
   * @param element the candidate PSI element
   * @return {@link #TEXT_TOKENIZER} for spellcheckable text/identifiers, or {@link #EMPTY_TOKENIZER} to ignore
   */
  @Override
  public @NotNull Tokenizer<?> getTokenizer(PsiElement element) {
    // 1. Line and block comments contain prose and documentation
    if (element instanceof PsiComment) {
      return TEXT_TOKENIZER;
    }

    // 2. String literals: only check if delimited by quotes; ignore numbers, booleans, and null
    if (element instanceof CompactLiteralExprImpl) {
      String text = element.getText();
      if (text.startsWith("\"") || text.startsWith("'")) {
        return TEXT_TOKENIZER;
      }
      return EMPTY_TOKENIZER;
    }

    // 3. Named declarations and reference identifiers (camelCase and snake_case decomposed into words)
    if (element instanceof PsiNamedElement || element instanceof CompactReferenceExpr) {
      return TEXT_TOKENIZER;
    }

    // 4. Compact keywords: suppress spellchecking errors on language keywords
    if (element != null && CompactTokenSets.KEYWORDS.contains(element.getNode().getElementType())) {
      return EMPTY_TOKENIZER;
    }

    // 5. Default fallback to superclass tokenizer
    return super.getTokenizer(element);
  }

  /**
   * Indicates whether full text-level spellchecking should be enabled for this language.
   *
   * @return {@code true}
   */
  @Override
  public boolean useTextLevelSpellchecking() {
    return true;
  }
}
