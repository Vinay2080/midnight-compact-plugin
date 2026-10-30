package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import dev.verloren.midnight.parser.CompactElementTypes;
import dev.verloren.midnight.reference.CompactValueReference;
import dev.verloren.midnight.type.CompactPrimitiveType;
import dev.verloren.midnight.type.CompactType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Central utility repository for Compact PSI tree navigation, parameter extraction,
 * callable signature queries, relative file resolution, and AST document manipulations.
 *
 * <h3>IntelliJ Platform Lifecycle &amp; Subsystem Role</h3>
 * <p>Serves as a shared, stateless helper library supporting reference resolvers, inspection tools,
 * navigation handlers, and quick-fix intentions across the Compact plugin:
 * <ul>
 *   <li><b>Parameter &amp; Binding Extraction:</b> Discovers parameters and destructuring pattern bindings
 *       within circuits, witnesses, and lambda forms via {@link #getParameters(PsiElement)}.</li>
 *   <li><b>Callable Return Type Extraction:</b> Determines declared or default return types for circuits,
 *       witnesses, and constructors via {@link #getCallableReturnType(PsiElement)}.</li>
 *   <li><b>Multi-Tiered File Resolution:</b> Implements intelligent relative module resolution
 *       ({@link #resolveRelativeCompactFile(PsiElement, String)}) searching local directories, parent folders,
 *       and IntelliJ project content roots, seamlessly handling relative prefixes ({@code ./}) and optional
 *       {@code .compact} extensions.</li>
 *   <li><b>Safe AST / Document Editing:</b> Provides whitespace-aware node deletion
 *       ({@link #deleteNodeWithTrailingWhitespace(Document, ASTNode)}) for quick-fixes like removing unused imports.</li>
 * </ul>
 * </p>
 *
 * <h3>Calling Pipeline</h3>
 * <p>Invoked across core IDE features:
 * <ul>
 *   <li>{@link dev.verloren.midnight.resolve.CompactResolveUtil} for parameter scoping, file importing, and include resolution.</li>
 *   <li>{@link dev.verloren.midnight.reference.CompactValueReference} and {@link dev.verloren.midnight.reference.CompactIncludeReference}.</li>
 *   <li>{@link dev.verloren.midnight.intention.CompactToggleExportIntention} when toggling keyword modifiers.</li>
 * </ul>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Read Action:</b> All query, traversal, and file resolution methods strictly require an active
 *       platform Read Action ({@code @RequiresReadLock}).</li>
 *   <li><b>Write Action:</b> Document-modifying methods (such as {@link #deleteNodeWithTrailingWhitespace(Document, ASTNode)})
 *       mutate the editor document and must execute inside a {@link com.intellij.openapi.command.WriteCommandAction} on the EDT.</li>
 * </ul>
 *
 * <h3>Design Decisions &amp; Memory Safety</h3>
 * <ul>
 *   <li><b>Stateless Pure Functions:</b> Uses a private constructor and static pure functions. Never caches
 *       {@link PsiElement}, {@link Document}, or {@link Project} references in static fields.</li>
 *   <li><b>Resolution Resilience:</b> File resolution normalizes forward/backward slashes and generates candidate permutations
 *       to accommodate varying import statement syntaxes across operating systems.</li>
 * </ul>
 *
 * @see dev.verloren.midnight.resolve.CompactResolveUtil
 * @see CompactFile
 * @see CompactElementFactory
 */
public final class CompactPsiUtil {

  /**
   * Private constructor preventing instantiation of this stateless utility class.
   */
  private CompactPsiUtil() {
  }

  /**
   * Collects all declared parameters (including standard parameters and pattern-bound variables)
   * within a circuit, witness, or callable declaration.
   *
   * @param owner the callable PSI element containing parameters
   * @return list of parameter and pattern-bound named elements; never {@code null}
   */
  public static @NotNull List<CompactNamedElement> getParameters(@NotNull PsiElement owner) {
    List<CompactNamedElement> params = new ArrayList<>();
    // Step 1: Scan all named descendants of the callable owner
    for (CompactNamedElement named : PsiTreeUtil.findChildrenOfType(owner, CompactNamedElement.class)) {
      // Step 2: Include direct parameter implementations or pattern parameters
      if (named instanceof CompactParameterImpl || isPatternParameter(named)) {
        params.add(named);
      }
    }
    return params;
  }

  /**
   * Checks whether a named pattern binding resides within a parameter list.
   *
   * @param declaration the named element to inspect
   * @return {@code true} if the pattern element is nested inside a parameter list; {@code false} otherwise
   */
  public static boolean isPatternParameter(@NotNull CompactNamedElement declaration) {
    if (!(declaration instanceof CompactPatternImpl)) {
      return false;
    }
    // Check if any ancestor is a pattern parameter list or arrow parameter list
    return hasAncestorOfType(declaration, CompactElementTypes.PATTERN_PARAMETER_LIST)
        || hasAncestorOfType(declaration, CompactElementTypes.ARROW_PARAMETER_LIST);
  }

  /**
   * Checks whether an element has an ancestor with the specified AST element type.
   *
   * @param element the starting PSI element
   * @param type the target ancestor element type
   * @return {@code true} if a matching ancestor exists in the parent chain; {@code false} otherwise
   */
  public static boolean hasAncestorOfType(@NotNull PsiElement element, @NotNull IElementType type) {
    for (PsiElement parent = element.getParent(); parent != null; parent = parent.getParent()) {
      if (parent.getNode() != null && parent.getNode().getElementType() == type) {
        return true;
      }
    }
    return false;
  }

  /**
   * Checks whether an element has an ancestor containing a child with the given token type within a scope limit.
   *
   * @param element the starting PSI element
   * @param tokenType the token type to look for
   * @param limit the boundary PSI element where the upward traversal halts
   * @return {@code true} if a matching token is found along the ancestor path before reaching limit; {@code false} otherwise
   */
  public static boolean hasAncestorWithToken(
      @NotNull PsiElement element,
      @NotNull IElementType tokenType,
      @NotNull PsiElement limit
  ) {
    for (PsiElement p = element; p != null && p != limit; p = p.getParent()) {
      if (p.getNode() != null && p.getNode().findChildByType(tokenType) != null) {
        return true;
      }
    }
    return false;
  }

  /**
   * Creates a {@link CompactValueReference} targeting the identifier child of the given element.
   *
   * <p>Calculates the relative text offset of the identifier leaf within its parent node,
   * ensuring that reference ranges, hover tooltips, and rename operations align exactly with the identifier.</p>
   *
   * @param element the PSI parent containing the identifier node
   * @return a new {@link CompactValueReference}, or {@code null} if no identifier child exists
   */
  public static @Nullable PsiReference createIdentifierValueReference(@NotNull PsiElement element) {
    // Step 1: Locate the IDENTIFIER leaf node in the AST
    ASTNode identifier = element.getNode().findChildByType(CompactTokenTypes.IDENTIFIER);
    if (identifier == null) {
      return null;
    }
    // Step 2: Compute relative text range within parent
    PsiElement psi = identifier.getPsi();
    int start = psi.getTextRange().getStartOffset() - element.getTextRange().getStartOffset();
    // Step 3: Instantiate reference bound to the identifier range
    return new CompactValueReference(element, TextRange.from(start, psi.getTextLength()));
  }

  /**
   * Extracts the evaluated return type of a circuit, witness, or constructor declaration.
   *
   * <p>If the callable omits an explicit return type, circuits and witnesses default to {@code Void}.
   * Constructors implicitly return {@code Void}.</p>
   *
   * @param element the callable element to inspect
   * @return the declared or defaulted {@link CompactType}, or {@link CompactPrimitiveType#UNKNOWN} if unrecognized
   */
  public static @NotNull CompactType getCallableReturnType(@Nullable PsiElement element) {
    if (element instanceof CompactCircuitDefinition circuit) {
      CompactTypeElement rt = circuit.getReturnTypeElement();
      return rt != null ? rt.getType() : new CompactPrimitiveType("Void");
    }
    if (element instanceof CompactWitnessDeclaration witness) {
      CompactTypeElement rt = witness.getReturnTypeElement();
      return rt != null ? rt.getType() : new CompactPrimitiveType("Void");
    }
    if (element instanceof CompactConstructorDeclaration) {
      return new CompactPrimitiveType("Void");
    }
    return CompactPrimitiveType.UNKNOWN;
  }

  /**
   * Extracts the unquoted string content from a string literal child of the given element.
   *
   * <p>Strips matching outer single quotes ({@code '}) or double quotes ({@code "}), returning the inner text.</p>
   *
   * @param element the PSI element containing a string literal node
   * @return unquoted string content, or {@code null} if no string literal child exists
   */
  public static @Nullable String extractStringLiteralValue(@NotNull PsiElement element) {
    ASTNode stringNode = element.getNode().findChildByType(CompactTokenTypes.STRING_LITERAL);
    if (stringNode == null) {
      return null;
    }
    String text = stringNode.getText();
    if ((text.startsWith("\"") && text.endsWith("\"")) || (text.startsWith("'") && text.endsWith("'"))) {
      if (text.length() >= 2) {
        return text.substring(1, text.length() - 1);
      }
    }
    return text;
  }

  /**
   * Generates candidate relative file path permutations with and without the {@code .compact} extension
   * and with or without the leading {@code ./} directory prefix.
   *
   * @param path the raw import or include path string
   * @return list of normalized path candidates; never {@code null}
   */
  public static @NotNull List<String> getCandidateFilePaths(@NotNull String path) {
    List<String> candidates = new ArrayList<>();
    String normalized = path.replace('\\', '/');
    candidates.add(normalized);
    if (!normalized.endsWith(".compact")) {
      candidates.add(normalized + ".compact");
    }
    if (normalized.startsWith("./")) {
      String stripped = normalized.substring(2);
      candidates.add(stripped);
      if (!stripped.endsWith(".compact")) {
        candidates.add(stripped + ".compact");
      }
    }
    return candidates;
  }

  /**
   * Searches for candidate files within a directory and returns the resolved {@link CompactFile}.
   *
   * @param project the project context
   * @param dir the parent directory virtual file to search within
   * @param candidates list of relative path permutations to probe
   * @return the resolved {@link CompactFile}, or {@code null} if not found
   */
  public static @Nullable CompactFile findCompactFileInDir(
      @NotNull Project project,
      @Nullable VirtualFile dir,
      @NotNull List<String> candidates
  ) {
    if (dir == null) {
      return null;
    }
    for (String candidate : candidates) {
      VirtualFile targetVirtualFile = dir.findFileByRelativePath(candidate);
      if (targetVirtualFile != null && targetVirtualFile.isValid()) {
        PsiFile psiFile = PsiManager.getInstance(project).findFile(targetVirtualFile);
        if (psiFile instanceof CompactFile compactFile) {
          return compactFile;
        }
      }
    }
    return null;
  }

  /**
   * Resolves a relative Compact file path across a multi-tiered lookup sequence:
   * <ol>
   *   <li>Immediate directory containing the context file.</li>
   *   <li>VirtualFile parent directory of the context file.</li>
   *   <li>IntelliJ project content roots registered in {@link ProjectRootManager}.</li>
   * </ol>
   *
   * @param context the PSI element originating the resolution (e.g. an import or include statement)
   * @param path the raw target path from the source code
   * @return the resolved {@link CompactFile}, or {@code null} if no matching file exists
   */
  public static @Nullable CompactFile resolveRelativeCompactFile(
      @NotNull PsiElement context,
      @NotNull String path
  ) {
    PsiFile containingFile = context.getContainingFile();
    if (containingFile == null) {
      return null;
    }

    Project project = context.getProject();
    List<String> candidates = getCandidateFilePaths(path);

    // Tier 1: Immediate directory containing the context file
    PsiDirectory dir = containingFile.getContainingDirectory();
    if (dir != null) {
      for (String candidate : candidates) {
        PsiFile direct = dir.findFile(candidate);
        if (direct instanceof CompactFile compactFile) {
          return compactFile;
        }
      }
      CompactFile psiFile = findCompactFileInDir(project, dir.getVirtualFile(), candidates);
      if (psiFile != null) return psiFile;
    }

    // Tier 2: VirtualFile parent relative path
    VirtualFile virtualFile = containingFile.getOriginalFile().getVirtualFile();
    if (virtualFile == null) {
      virtualFile = containingFile.getViewProvider().getVirtualFile();
    }
    {
      CompactFile psiFile = findCompactFileInDir(project, virtualFile.getParent(), candidates);
      if (psiFile != null) return psiFile;
    }

    // Tier 3: Project content root relative paths
    VirtualFile[] contentRoots = ProjectRootManager.getInstance(project).getContentRoots();
    for (VirtualFile root : contentRoots) {
      for (String candidate : candidates) {
        VirtualFile targetVirtualFile = root.findFileByRelativePath(candidate);
        if (targetVirtualFile != null && targetVirtualFile.isValid()) {
          PsiFile psiFile = PsiManager.getInstance(project).findFile(targetVirtualFile);
          if (psiFile instanceof CompactFile compactFile && psiFile != containingFile) {
            return compactFile;
          }
        }
      }
    }

    return null;
  }

  /**
   * Deletes an AST node and any trailing horizontal whitespace up to the newline character from an editor document.
   *
   * <p>Used by refactoring and quick-fix actions (such as removing unused imports or includes)
   * to ensure no trailing blank whitespace is left behind on the line.</p>
   *
   * @param document the editor document to mutate
   * @param node the AST node to delete
   */
  public static void deleteNodeWithTrailingWhitespace(@NotNull Document document, @NotNull ASTNode node) {
    int start = node.getTextRange().getStartOffset();
    int end = node.getTextRange().getEndOffset();
    CharSequence chars = document.getCharsSequence();
    // Advance end offset across any trailing horizontal whitespace characters
    while (end < chars.length() && Character.isWhitespace(chars.charAt(end)) && chars.charAt(end) != '\n') {
      end++;
    }
    // Delete the computed range in the document
    document.deleteString(start, end);
  }
}