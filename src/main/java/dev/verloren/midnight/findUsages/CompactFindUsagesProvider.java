package dev.verloren.midnight.findUsages;

import com.intellij.lang.cacheBuilder.DefaultWordsScanner;
import com.intellij.lang.cacheBuilder.WordsScanner;
import com.intellij.lang.findUsages.FindUsagesProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.TokenSet;
import dev.verloren.midnight.lexer.CompactLexer;
import dev.verloren.midnight.lexer.CompactTokenSets;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import dev.verloren.midnight.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Find Usages provider for Compact language declarations.
 *
 * <p>Integrates with IntelliJ's indexer using {@link DefaultWordsScanner} to identify words
 * matching {@link CompactTokenTypes#IDENTIFIER}. Formats descriptive type names
 * ("circuit", "witness", "struct", "enum", "type") in the Find Usages results tool window.</p>
 */
public class CompactFindUsagesProvider implements FindUsagesProvider {
  @Override
  public @Nullable WordsScanner getWordsScanner() {
    return new DefaultWordsScanner(
            new CompactLexer(),
            TokenSet.create(CompactTokenTypes.IDENTIFIER),
            CompactTokenSets.COMMENTS,
            CompactTokenSets.LITERALS
    );
  }

  @Override
  public boolean canFindUsagesFor(@NotNull PsiElement psiElement) {
    return psiElement instanceof CompactNamedElement;
  }

  @Override
  public @Nullable String getHelpId(@NotNull PsiElement psiElement) {
    return null;
  }

  @Override
  public @NotNull String getType(@NotNull PsiElement element) {
    if (element instanceof CompactCircuitDefinition) return "circuit";
    if (element instanceof CompactWitnessDeclaration) return "witness";
    if (element instanceof CompactLedgerDeclaration) return "ledger";
    if (element instanceof CompactStructDefinition) return "struct";
    if (element instanceof CompactStructFieldImpl) return "struct field";
    if (element instanceof CompactEnumDefinition) return "enum";
    if (element instanceof CompactEnumMemberImpl) return "enum member";
    if (element instanceof CompactTypeDefinition) return "type";
    if (element instanceof CompactModuleDefinition) return "module";
    if (element instanceof CompactParameterImpl) return "parameter";
    if (element instanceof CompactConstBindingImpl || element instanceof CompactPatternImpl) return "local";
    if (element instanceof CompactImportElementImpl) return "import";
    return "declaration";
  }

  @Override
  public @NotNull String getDescriptiveName(@NotNull PsiElement element) {
    if (element instanceof CompactNamedElement) {
      String name = ((CompactNamedElement) element).getName();
      return name == null ? "<unnamed>" : name;
    }
    return element.getText();
  }

  @Override
  public @NotNull String getNodeText(@NotNull PsiElement element, boolean useFullName) {
    return getDescriptiveName(element);
  }
}