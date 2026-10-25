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
 * Find Usages provider for Compact smart contract declarations.
 *
 * <h3>Subsystem Role &amp; Extension Point</h3>
 * <p>Registered in {@code plugin.xml} via the {@code com.intellij.lang.findUsagesProvider} extension point
 * for {@link dev.verloren.midnight.CompactLanguage}. Integrates the Compact language with IntelliJ's
 * symbol indexing and search infrastructure:
 * <ul>
 *   <li><b>Words Scanner:</b> Supplies a {@link DefaultWordsScanner} configured with {@link CompactLexer}
 *       to index identifier words, comments, and literals in the platform's inverted index ({@code IdIndex}),
 *       enabling fast pre-filtering of candidate files before parsing full ASTs.</li>
 *   <li><b>Target Qualification:</b> Filters eligible search targets ({@link #canFindUsagesFor}) ensuring
 *       only named Compact elements ({@link CompactNamedElement}) can initiate Find Usages queries.</li>
 *   <li><b>Descriptive Classification:</b> Maps AST nodes to user-friendly semantic type descriptions
 *       (e.g., "circuit", "witness", "ledger", "struct field", "enum member") in the Find Usages results window.</li>
 *   <li><b>Presentation Text:</b> Formats descriptive node labels and navigation presentation strings.</li>
 * </ul>
 * </p>
 *
 * <h3>Calling Pipeline &amp; Lifecycle</h3>
 * <p>Invoked across two distinct platform phases:
 * <ol>
 *   <li><b>Indexing Phase:</b> IntelliJ's background indexer queries {@link #getWordsScanner()} to tokenize
 *       Compact files and build the project's word index.</li>
 *   <li><b>Search &amp; Presentation Phase:</b> When the user triggers "Find Usages" (Alt+F7 / Cmd+Option+F7)
 *       or "Show Usages" (Ctrl+Alt+F7 / Cmd+Option+F7), {@code FindUsagesManager} verifies {@link #canFindUsagesFor},
 *       executes reference resolution, and calls {@link #getType} and {@link #getNodeText} to render results.</li>
 * </ol>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Execution Thread:</b> Query and presentation methods run on pooled background worker threads
 *       during search execution or on the Event Dispatch Thread (EDT) during UI presentation.</li>
 *   <li><b>Read Action:</b> Requires a platform Read Action ({@code @RequiresReadLock}) while inspecting
 *       PSI elements and resolving element names.</li>
 *   <li><b>Cancellation Support:</b> Respects standard IntelliJ progress indicators during long-running searches.</li>
 * </ul>
 *
 * <h3>Architecture &amp; Pipeline Diagram</h3>
 * <pre>
 *   [User presses Alt+F7 / Cmd+Option+F7]
 *                  │
 *                  ▼
 *      [CompactFindUsagesProvider]
 *                  │
 *                  ├── [WordsScanner] ──────────► Feeds IdIndex with CompactTokenTypes.IDENTIFIER
 *                  │                              (Skips COMMENTS &amp; LITERALS from word index)
 *                  │
 *                  ├── [canFindUsagesFor] ──────► Verifies target instanceof CompactNamedElement
 *                  │
 *                  └── [Results Tool Window]
 *                        ├── getType() ─────────► "circuit", "witness", "ledger", "struct", etc.
 *                        ├── getDescriptiveName() ► Element identifier name (or "&lt;unnamed&gt;")
 *                        └── getNodeText() ─────► Display label for tree node
 * </pre>
 *
 * @see dev.verloren.midnight.psi.CompactNamedElement
 * @see dev.verloren.midnight.lexer.CompactLexer
 * @see com.intellij.lang.findUsages.FindUsagesProvider
 * @see com.intellij.lang.cacheBuilder.DefaultWordsScanner
 */
public class CompactFindUsagesProvider implements FindUsagesProvider {

  /**
   * Supplies the word scanner utilized by IntelliJ's indexer to build fast word-lookup tables.
   *
   * <p>Tokenizes Compact source code using {@link CompactLexer}, indexing identifier tokens while
   * treating comments and string/numeric literals as secondary search domains.</p>
   *
   * @return a configured {@link DefaultWordsScanner} instance
   */
  @Override
  public @Nullable WordsScanner getWordsScanner() {
    // Step 1: Return a DefaultWordsScanner configured with Compact lexer and token category sets
    return new DefaultWordsScanner(
            new CompactLexer(),
            TokenSet.create(CompactTokenTypes.IDENTIFIER),
            CompactTokenSets.COMMENTS,
            CompactTokenSets.LITERALS
    );
  }

  /**
   * Determines whether the specified PSI element can be the subject of a Find Usages search.
   *
   * @param psiElement the PSI element under inspection
   * @return true if the element implements {@link CompactNamedElement}; false otherwise
   */
  @Override
  public boolean canFindUsagesFor(@NotNull PsiElement psiElement) {
    // Step 1: Only named declarations (circuits, witnesses, structs, fields, etc.) can be searched
    return psiElement instanceof CompactNamedElement;
  }

  /**
   * Provides an optional help topic ID for the Find Usages dialog.
   *
   * @param psiElement the PSI element being searched
   * @return null as no custom help topic is registered
   */
  @Override
  public @Nullable String getHelpId(@NotNull PsiElement psiElement) {
    return null;
  }

  /**
   * Computes a user-friendly type classification label for the given element in Find Usages tool windows.
   *
   * @param element the PSI element to classify
   * @return descriptive type string (e.g. "circuit", "witness", "ledger", "struct field", "enum member")
   */
  @Override
  public @NotNull String getType(@NotNull PsiElement element) {
    // Step 1: Match against major top-level and member declaration types
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
      case CompactEnumMemberImpl _ -> {
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

    // Step 2: Classify local constants, pattern bindings, and import targets
    if (element instanceof CompactConstBindingImpl || element instanceof CompactPatternImpl) return "local";
    if (element instanceof CompactImportElementImpl) return "import";

    // Step 3: Default fallback for any unspecified named declaration
    return "declaration";
  }

  /**
   * Returns the primary name of the element displayed in the Find Usages tool window header.
   *
   * @param element the PSI element being described
   * @return the element's identifier name, or "&lt;unnamed&gt;" if unnamed, or raw text fallback
   */
  @Override
  public @NotNull String getDescriptiveName(@NotNull PsiElement element) {
    // Step 1: Extract element name if named element
    if (element instanceof CompactNamedElement) {
      String name = ((CompactNamedElement) element).getName();
      return name == null ? "<unnamed>" : name;
    }
    // Step 2: Fallback to raw element text
    return element.getText();
  }

  /**
   * Formats the presentation text for the element node in the Find Usages results tree.
   *
   * @param element     the PSI element to represent
   * @param useFullName whether to include full qualified path (delegates to descriptive name)
   * @return display string for the element node
   */
  @Override
  public @NotNull String getNodeText(@NotNull PsiElement element, boolean useFullName) {
    return getDescriptiveName(element);
  }
}