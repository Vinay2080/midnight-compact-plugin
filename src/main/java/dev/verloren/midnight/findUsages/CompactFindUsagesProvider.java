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
    switch (element) {
      case CompactCircuitDefinition _ -> {
        return "circuit";
      }
      case CompactWitnessDeclaration _ -> {
        return "witness";
      }
      case CompactLedgerDeclaration _ -> {
        return "ledger";
      }
      case CompactStructDefinition _ -> {
        return "struct";
      }
      case CompactStructFieldImpl _ -> {
        return "struct field";
      }
      case CompactEnumDefinition _ -> {
        return "enum";
      }
      case CompactEnumMemberImpl compactEnumMember -> {
        return "enum member";
      }
      case CompactTypeDefinition _ -> {
        return "type";
      }
      case CompactModuleDefinition _ -> {
        return "module";
      }
      case CompactParameterImpl _ -> {
        return "parameter";
      }
      default -> {
      }
    }
    if (element instanceof CompactConstBindingImpl || element instanceof CompactPatternImpl) return "local";
    if (element instanceof CompactImportElementImpl) return "import";
    return "declaration";
  }

  @Override
  public @NotNull String getDescriptiveName(@NotNull PsiElement element) {
    if (element instanceof CompactNamedElement) {
      String name = ((CompactNamedElement)element).getName();
      return name == null ? "<unnamed>" : name;
    }
    return element.getText();
  }

  @Override
  public @NotNull String getNodeText(@NotNull PsiElement element, boolean useFullName) {
    return getDescriptiveName(element);
  }
}