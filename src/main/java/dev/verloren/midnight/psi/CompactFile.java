package dev.verloren.midnight.psi;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.CompactFileType;
import dev.verloren.midnight.CompactLanguage;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

/**
 * Root PSI element representing a complete Compact source file ({@code .compact}).
 *
 * <h3>IntelliJ Platform Lifecycle &amp; Subsystem Role</h3>
 * <p>Serves as the root of the PSI tree for every Compact file opened or indexed in the project.
 * Instantiated by {@link dev.verloren.midnight.parser.CompactParserDefinition#createFile(FileViewProvider)}
 * when IntelliJ's {@link com.intellij.psi.PsiManager} creates a PSI representation from a
 * {@link FileViewProvider}.
 * <ul>
 *   <li><b>Top-Level Declaration Container:</b> Houses all file-level pragmas, includes, imports,
 *       modules, structs, enums, circuits, contracts, and const bindings.</li>
 *   <li><b>Symbol Resolution Scope:</b> Serves as the top-level boundary for symbol searches,
 *       providing quick access to top-level declarations via {@link #getTopLevelDeclarations()}.</li>
 *   <li><b>File Indexing &amp; Highlighting:</b> Acts as the entry point for {@link com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl}
 *       and indexing services.</li>
 * </ul>
 * </p>
 *
 * <h3>Calling Pipeline</h3>
 * <p>Invoked across core IDE subsystems:
 * <ul>
 *   <li>{@link dev.verloren.midnight.parser.CompactParserDefinition#createFile} during file parsing.</li>
 *   <li>{@link dev.verloren.midnight.resolve.CompactResolveUtil} during multi-file import resolution and symbol lookups.</li>
 *   <li>{@link dev.verloren.midnight.completion.providers.CompactIdentifierCompletionProvider} when enumerating file declarations.</li>
 *   <li>{@link dev.verloren.midnight.navigation.CompactGotoDeclarationHandler} when resolving top-level symbols.</li>
 * </ul>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Execution Thread:</b> Read methods run on pooled background threads (parsing, daemon highlighting, indexing)
 *       or EDT (editor actions).</li>
 *   <li><b>Read Action:</b> All query methods (including {@link #getTopLevelDeclarations()} and {@link #getDeclarations()})
 *       strictly require an active platform Read Action ({@code @RequiresReadLock}).</li>
 *   <li><b>Write Action:</b> Modifying file content or AST children requires a Write Action wrapped in
 *       a {@link com.intellij.openapi.command.WriteCommandAction} on the EDT.</li>
 * </ul>
 *
 * <h3>Design Decisions &amp; Memory Safety</h3>
 * <ul>
 *   <li><b>No Persistent Element References:</b> To prevent memory leaks, instances of {@link CompactFile}
 *       are never stored in static fields, global registries, or long-lived caches.</li>
 *   <li><b>Dynamic Top-Level Filtering:</b> {@link #getTopLevelDeclarations()} evaluates children on demand,
 *       delegating to {@link dev.verloren.midnight.resolve.CompactResolveUtil#isTopLevelFileDeclaration} to
 *       ensure accurate symbol sets across incremental file updates.</li>
 * </ul>
 *
 * @see PsiFileBase
 * @see CompactFileType
 * @see CompactLanguage
 * @see dev.verloren.midnight.parser.CompactParserDefinition
 */
public class CompactFile extends PsiFileBase {

  /**
   * Constructs a Compact PSI file root bound to the specified view provider.
   *
   * @param viewProvider the file view provider managing document buffers and virtual files
   */
  public CompactFile(@NotNull FileViewProvider viewProvider) {
    super(viewProvider, CompactLanguage.INSTANCE);
  }

  /**
   * Returns the Compact file type descriptor.
   *
   * @return {@link CompactFileType#INSTANCE}
   */
  @Override
  public @NotNull FileType getFileType() {
    return CompactFileType.INSTANCE;
  }

  /**
   * Collects all named declarations that reside at the top level of this file.
   *
   * <p>Excludes local declarations (such as block-scoped constants, parameters, and pattern variables)
   * while including top-level circuits, modules, structs, enums, witnesses, and global constants.</p>
   *
   * @return list of top-level named elements in the file; never {@code null}
   */
  public @NotNull List<CompactNamedElement> getTopLevelDeclarations() {
    List<CompactNamedElement> result = new java.util.ArrayList<>();
    // Step 1: Iterate over immediate child elements of the file
    for (PsiElement child : getChildren()) {
      if (child instanceof CompactNamedElement named) {
        // Step 2: Directly check if child is a top-level declaration
        if (dev.verloren.midnight.resolve.CompactResolveUtil.isTopLevelFileDeclaration(named)) {
          result.add(named);
        }
      } else {
        // Step 3: Inspect wrapper nodes (e.g. export statements or definitions) for top-level named children
        for (CompactNamedElement named : PsiTreeUtil.findChildrenOfType(child, CompactNamedElement.class)) {
          if (dev.verloren.midnight.resolve.CompactResolveUtil.isTopLevelFileDeclaration(named)) {
            result.add(named);
          }
        }
      }
    }
    return result;
  }

  /**
   * Collects all named elements anywhere within this file hierarchy, including local parameters and nested bindings.
   *
   * @return collection of all named declarations and bindings within the file
   */
  public @NotNull Collection<CompactNamedElement> getDeclarations() {
    return PsiTreeUtil.findChildrenOfType(this, CompactNamedElement.class);
  }

  /**
   * Returns a debug representation of this PSI file.
   *
   * @return {@code "Compact File"}
   */
  @Override
  public String toString() {
    return "Compact File";
  }
}