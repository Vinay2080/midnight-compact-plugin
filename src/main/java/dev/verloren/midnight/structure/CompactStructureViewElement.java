package dev.verloren.midnight.structure;

import com.intellij.icons.AllIcons;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.util.treeView.smartTree.TreeElement;
import com.intellij.navigation.ItemPresentation;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.icons.MidnightIcons;
import dev.verloren.midnight.parser.CompactElementTypes;
import dev.verloren.midnight.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Tree node adapter adapting Compact PSI elements for display in the Structure View tool window.
 *
 * <h3>Subsystem Role &amp; Architecture</h3>
 * <p>Implements {@link StructureViewTreeElement} and {@link Navigatable}. Acts as the presentation
 * and hierarchy adapter wrapping individual {@link PsiElement} AST nodes. For every node, it provides:
 * <ul>
 *   <li><b>Presentable Text:</b> Synthesizes clean labels with contextual prefixes (e.g., {@code circuit foo},
 *       {@code witness bar}, {@code struct Point}, {@code enum Status}, {@code const MAX_LIMIT})
 *       via {@link #getPresentableText()}.</li>
 *   <li><b>Location Metadata:</b> Decorates nested declarations with their enclosing module name via {@link #getLocationString()}.</li>
 *   <li><b>Standard Semantic Icons:</b> Maps Compact language constructs to standard IntelliJ icons
 *       (e.g., circuits to {@link AllIcons.Nodes#Method}, witnesses to {@link AllIcons.Nodes#AbstractMethod},
 *       ledger state to {@link AllIcons.Nodes#DataTables}, and structs to {@link AllIcons.Nodes#Record}) via {@link #getIcon()}.</li>
 *   <li><b>Child Decomposition:</b> Unfolds compound statements (e.g. multi-variable {@code const} statements)
 *       and recurses through code blocks in modules and contract implementations via {@link #getChildren()}.</li>
 *   <li><b>Direct Navigation:</b> Implements {@link Navigatable} to allow seamless double-click or Enter jumps
 *       directly to source offsets in the editor.</li>
 * </ul>
 * </p>
 *
 * <h3>Calling Pipeline</h3>
 * <p>Instantiated initially by {@link CompactStructureViewModel} for the root {@link CompactFile}.
 * When a user expands tree nodes in the Structure tool window or File Structure popup (Ctrl+F12),
 * IntelliJ's background asynchronous tree model calls {@link #getChildren()} and {@link #getPresentation()}
 * to populate child nodes on demand.</p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Execution Thread:</b> Node queries and child evaluations run primarily on pooled background worker threads
 *       in modern IntelliJ platform versions (AsyncTreeModel), or occasionally on the EDT.</li>
 *   <li><b>Read Action:</b> All methods require a platform Read Action ({@code @RequiresReadLock}) since they
 *       traverse PSI elements and inspect AST node tokens.</li>
 *   <li><b>Write Actions:</b> Read-only; never acquires write actions.</li>
 * </ul>
 *
 * <h3>Structural Unfolding Diagram</h3>
 * <pre>
 *   CompactFile (Root Element)
 *         ├── Pragma / Import / Export (Directives)
 *         ├── Contract / Module (Containers)
 *         │     └── Inner Blocks ──► Circuits, Witnesses, Ledgers, Consts
 *         ├── StructDefinition
 *         │     └── StructField (Leaf elements)
 *         └── EnumDefinition
 *               └── EnumMember (Leaf elements)
 * </pre>
 *
 * @see dev.verloren.midnight.structure.CompactStructureViewModel
 * @see dev.verloren.midnight.structure.CompactStructureViewFactory
 * @see com.intellij.ide.structureView.StructureViewTreeElement
 * @see com.intellij.navigation.ItemPresentation
 * @see com.intellij.pom.Navigatable
 */
public class CompactStructureViewElement implements StructureViewTreeElement, Navigatable {

  /**
   * Cached empty array to prevent redundant allocations during high-frequency leaf node queries.
   */
  private static final TreeElement[] EMPTY_TREE_ELEMENTS = new TreeElement[0];

  /**
   * The underlying Compact PSI element represented by this tree node.
   */
  private final PsiElement element;

  /**
   * Constructs a structure view tree element wrapping the specified PSI element.
   *
   * @param element the Compact PSI element to adapt
   */
  public CompactStructureViewElement(@NotNull PsiElement element) {
    this.element = element;
  }

  /**
   * Returns the underlying Compact PSI element represented by this tree node.
   *
   * @return the wrapped {@link PsiElement}
   */
  @Override
  public Object getValue() {
    return element;
  }

  /**
   * Navigates the active editor to the source code definition of the underlying element.
   *
   * @param requestFocus true if the editor should receive focus after navigation
   */
  @Override
  public void navigate(boolean requestFocus) {
    if (element instanceof Navigatable navigatable && navigatable.canNavigate()) {
      navigatable.navigate(requestFocus);
    }
  }

  /**
   * Checks whether the underlying element supports source navigation.
   *
   * @return true if the element is valid and navigatable; false otherwise
   */
  @Override
  public boolean canNavigate() {
    return element instanceof Navigatable navigatable && navigatable.canNavigate();
  }

  /**
   * Checks whether the underlying element can navigate specifically to source code.
   *
   * @return true if source navigation is possible; false otherwise
   */
  @Override
  public boolean canNavigateToSource() {
    return element instanceof Navigatable navigatable && navigatable.canNavigateToSource();
  }

  /**
   * Assembles the presentation metadata (text, location string, and icon) for the tree node.
   *
   * @return an {@link ItemPresentation} configured with presentable text, location, and icon
   */
  @Override
  public @NotNull ItemPresentation getPresentation() {
    return new PresentationData(getPresentableText(), getLocationString(), getIcon(), null);
  }

  /**
   * Computes the presentable display text for this structure element, including semantic kind prefixes.
   *
   * @return formatted label string, or null if the element cannot be described
   */
  public @Nullable String getPresentableText() {
    return switch (element) {
      case CompactFile compactFile -> compactFile.getName();
      case CompactCircuitDefinition circuit -> circuit.getName() != null ? "circuit " + circuit.getName() : "circuit";
      case CompactWitnessDeclaration witness -> witness.getName() != null ? "witness " + witness.getName() : "witness";
      case CompactExternalContractDeclaration contract -> contract.getName() != null ? "contract " + contract.getName() : "contract";
      case CompactContractImplementsDeclaration ignored -> "contract";
      case CompactModuleDefinition module -> module.getName() != null ? "module " + module.getName() : "module";
      case CompactStructDefinition struct -> struct.getName() != null ? "struct " + struct.getName() : "struct";
      case CompactStructFieldImpl field -> field.getName() != null ? field.getName() : "field";
      case CompactEnumDefinition enumDef -> enumDef.getName() != null ? "enum " + enumDef.getName() : "enum";
      case CompactEnumMemberImpl member -> member.getName() != null ? member.getName() : "member";
      case CompactTypeDefinition typeDef -> typeDef.getName() != null ? "type " + typeDef.getName() : "type";
      case CompactConstructorDeclaration ignored -> "constructor";
      case CompactLedgerDeclaration ledger -> ledger.getName() != null ? "ledger " + ledger.getName() : "ledger";
      case CompactPatternImpl pattern -> pattern.getName() != null ? "const " + pattern.getName() : "const";
      case CompactConstBindingImpl binding -> binding.getName() != null ? "const " + binding.getName() : "const";
      case CompactPragmaForm pragma -> "pragma " + pragma.getText().trim();
      case CompactIncludeDeclaration include -> "include " + include.getText().replace("include", "").replace(";", "").trim();
      case CompactImportDeclaration importDecl -> "import " + importDecl.getText().replace("import", "").replace(";", "").trim();
      case CompactExportDeclaration exportDecl -> "export " + exportDecl.getText().replace("export", "").replace(";", "").trim();
      default -> {
        // Fallback A: Special handling for external circuit signatures
        if (element.getNode() != null && element.getNode().getElementType() == CompactElementTypes.EXTERNAL_CIRCUIT) {
          PsiElement id = PsiTreeUtil.findChildOfType(element, CompactReferenceExprImpl.class);
          yield id != null ? "circuit " + id.getText() : "circuit " + element.getText();
        }
        // Fallback B: Any named PSI element
        if (element instanceof PsiNamedElement named) {
          yield named.getName();
        }
        // Fallback C: Raw text representation
        yield element.getText();
      }
    };
  }

  /**
   * Computes contextual location metadata (e.g. enclosing module name) displayed as gray text beside the node.
   *
   * @return the enclosing module name if nested within a module; null otherwise
   */
  public @Nullable String getLocationString() {
    PsiElement parent = element.getParent();
    if (parent instanceof CompactModuleDefinition module) {
      return module.getName();
    }
    return null;
  }

  /**
   * Resolves the semantic icon representing this Compact construct in the structure tree.
   *
   * @return the corresponding {@link Icon}, or null if no specific icon is assigned
   */
  public @Nullable Icon getIcon() {
    return switch (element) {
      case CompactFile ignored -> MidnightIcons.FILE;
      case CompactCircuitDefinition ignored -> AllIcons.Nodes.Method;
      case CompactWitnessDeclaration ignored -> AllIcons.Nodes.AbstractMethod;
      case CompactExternalContractDeclaration ignored -> AllIcons.Nodes.Class;
      case CompactContractImplementsDeclaration ignored -> AllIcons.Nodes.Class;
      case CompactModuleDefinition ignored -> AllIcons.Nodes.Package;
      case CompactStructDefinition ignored -> AllIcons.Nodes.Record;
      case CompactStructFieldImpl ignored -> AllIcons.Nodes.Field;
      case CompactEnumMemberImpl ignored -> AllIcons.Nodes.Field;
      case CompactEnumDefinition ignored -> AllIcons.Nodes.Enum;
      case CompactTypeDefinition ignored -> AllIcons.Nodes.Type;
      case CompactConstructorDeclaration ignored -> AllIcons.Nodes.ClassInitializer;
      case CompactLedgerDeclaration ignored -> AllIcons.Nodes.DataTables;
      case CompactPatternImpl ignored -> AllIcons.Nodes.Constant;
      case CompactConstBindingImpl ignored -> AllIcons.Nodes.Constant;
      case CompactPragmaForm ignored -> AllIcons.Nodes.Tag;
      case CompactImportDeclaration ignored -> AllIcons.Nodes.Tag;
      case CompactIncludeDeclaration ignored -> AllIcons.Nodes.Include;
      case CompactExportDeclaration ignored -> AllIcons.Nodes.Deploy;
      default -> {
        if (element.getNode() != null && element.getNode().getElementType() == CompactElementTypes.EXTERNAL_CIRCUIT) {
          yield AllIcons.Nodes.Method;
        }
        yield null;
      }
    };
  }

  /**
   * Resolves and returns all child structure view elements for expandable containers.
   *
   * <p>Handles container-specific traversal:
   * <ul>
   *   <li><b>Files:</b> Traverses top-level statements and unrolls compound constant declarations.</li>
   *   <li><b>External Contracts:</b> Collects external circuit signatures.</li>
   *   <li><b>Contract Implementations &amp; Modules:</b> Recursively unwraps block statements to collect inner declarations.</li>
   *   <li><b>Structs:</b> Collects struct fields.</li>
   *   <li><b>Enums:</b> Collects enum members.</li>
   * </ul>
   * </p>
   *
   * @return array of child {@link TreeElement} items, or {@link #EMPTY_TREE_ELEMENTS} if this node is a leaf
   */
  @Override
  public TreeElement @NotNull [] getChildren() {
    List<TreeElement> children = new ArrayList<>();
    switch (element) {
      case CompactFile ignored -> {
        // Step 1: Scan direct children of the source file
        for (PsiElement child : element.getChildren()) {
          addStructureItem(child, children);
        }
      }
      case CompactExternalContractDeclaration ignored -> {
        // Step 2: Extract external circuit signatures within contract interface
        for (PsiElement child : element.getChildren()) {
          if (child.getNode().getElementType() == CompactElementTypes.EXTERNAL_CIRCUIT) {
            children.add(new CompactStructureViewElement(child));
          }
        }
      }
      case CompactContractImplementsDeclaration ignored ->
          // Step 3: Recurse into contract implementation blocks
          collectInnerDeclarations(element, children);
      case CompactModuleDefinition ignored ->
          // Step 4: Recurse into module body blocks
          collectInnerDeclarations(element, children);
      case CompactStructDefinition ignored -> {
        // Step 5: Extract individual struct fields
        for (CompactStructFieldImpl field : PsiTreeUtil.findChildrenOfType(element, CompactStructFieldImpl.class)) {
          children.add(new CompactStructureViewElement(field));
        }
      }
      case CompactEnumDefinition ignored -> {
        // Step 6: Extract individual enum members
        for (CompactEnumMemberImpl member : PsiTreeUtil.findChildrenOfType(element, CompactEnumMemberImpl.class)) {
          children.add(new CompactStructureViewElement(member));
        }
      }
      default -> {}
    }
    return children.isEmpty() ? EMPTY_TREE_ELEMENTS : children.toArray(EMPTY_TREE_ELEMENTS);
  }

  /**
   * Evaluates a candidate PSI element, unrolling compound statements or adding structural items.
   *
   * @param elem the candidate PSI element
   * @param out  the destination list of tree elements
   */
  private static void addStructureItem(@NotNull PsiElement elem, @NotNull List<TreeElement> out) {
    // Step 1: Unroll compound constant bindings (e.g. `const (x, y) = ...`) into individual pattern elements
    if (elem.getNode().getElementType() == CompactElementTypes.CONST_STATEMENT) {
      for (CompactPatternImpl pattern : PsiTreeUtil.findChildrenOfType(elem, CompactPatternImpl.class)) {
        out.add(new CompactStructureViewElement(pattern));
      }
      return;
    }
    // Step 2: Handle export form nodes
    if (elem.getNode().getElementType() == CompactElementTypes.EXPORT_FORM) {
      out.add(new CompactStructureViewElement(elem));
      return;
    }
    // Step 3: Handle recognized structure items directly
    if (isStructureItem(elem)) {
      out.add(new CompactStructureViewElement(elem));
    }
  }

  /**
   * Tests whether a PSI element qualifies as an identifiable top-level or structural member.
   *
   * @param elem the PSI element to test
   * @return true if the element should appear in the structure tree; false otherwise
   */
  private static boolean isStructureItem(@NotNull PsiElement elem) {
    return elem instanceof CompactPragmaForm ||
           elem instanceof CompactIncludeDeclaration ||
           elem instanceof CompactImportDeclaration ||
           elem instanceof CompactExportDeclaration ||
           elem instanceof CompactExternalContractDeclaration ||
           elem instanceof CompactContractImplementsDeclaration ||
           elem instanceof CompactModuleDefinition ||
           elem instanceof CompactLedgerDeclaration ||
           elem instanceof CompactConstructorDeclaration ||
           elem instanceof CompactCircuitDefinition ||
           elem instanceof CompactWitnessDeclaration ||
           elem instanceof CompactStructDefinition ||
           elem instanceof CompactEnumDefinition ||
           elem instanceof CompactTypeDefinition ||
           elem instanceof CompactPatternImpl ||
           elem instanceof CompactConstBindingImpl;
  }

  /**
   * Recursively traverses statement containers and blocks to collect inner structural declarations.
   *
   * @param container the parent container (e.g. module or contract implementation)
   * @param out       the destination list of tree elements
   */
  private static void collectInnerDeclarations(@NotNull PsiElement container, @NotNull List<TreeElement> out) {
    for (PsiElement child : container.getChildren()) {
      if (child instanceof CompactBlock) {
        // Step 1: Transparently unpack code blocks
        collectInnerDeclarations(child, out);
      } else {
        // Step 2: Process direct inner declaration
        addStructureItem(child, out);
      }
    }
  }
}
