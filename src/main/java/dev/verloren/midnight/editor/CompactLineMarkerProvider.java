package dev.verloren.midnight.editor;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.CompactFileType;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import dev.verloren.midnight.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Line marker provider that renders contextual gutter icons and navigability targets for
 * Compact declarations, interface implementations, and Zero-Knowledge privacy constructs.
 *
 * <h3>Architectural Role &amp; Extension Point</h3>
 * <p>Registered as a {@code codeInsight.lineMarkerProvider} extension for language {@code Compact}
 * in {@code plugin.xml}. Implements {@link LineMarkerProvider} to decorate editor gutters with visual
 * indicators and cross-navigation popups, enriching the developer experience when working with
 * Midnight's hybrid public/private smart contract paradigm.</p>
 *
 * <h3>Gutter Indicator Taxonomy</h3>
 * <table border="1">
 *   <caption>Compact Gutter Marker Taxonomy</caption>
 *   <tr><th>Syntax Target</th><th>Visual Icon</th><th>Navigation / Semantic Behavior</th></tr>
 *   <tr><td>{@code disclose} keyword</td><td>{@link AllIcons.Nodes#KeymapEditor}</td><td>ZK Disclosure boundary indicator (private witness to circuit).</td></tr>
 *   <tr><td>{@code implements} in contract</td><td>{@link AllIcons.Gutter#ImplementingMethod}</td><td>Navigate UP to the implemented interface contract declaration.</td></tr>
 *   <tr><td>Interface Contract name</td><td>{@link AllIcons.Gutter#ImplementedMethod}</td><td>Navigate DOWN to all project contracts implementing this interface.</td></tr>
 *   <tr><td>Interface Circuit name</td><td>{@link AllIcons.Gutter#ImplementedMethod}</td><td>Navigate DOWN to concrete circuit definitions implementing this signature.</td></tr>
 *   <tr><td>{@code witness} declaration name</td><td>{@link AllIcons.Nodes#AbstractMethod}</td><td>Visual indicator: Private off-chain witness query.</td></tr>
 *   <tr><td>Concrete Circuit (overriding)</td><td>{@link AllIcons.Gutter#ImplementingMethod}</td><td>Navigate UP to matching interface circuit signature.</td></tr>
 *   <tr><td>Concrete Circuit (exported)</td><td>{@link AllIcons.Actions#Lightning}</td><td>Visual indicator: Exported on-chain ZK circuit entry point.</td></tr>
 *   <tr><td>{@code ledger} declaration name</td><td>{@link AllIcons.Nodes#DataTables}</td><td>Visual indicator: On-chain ledger state (sealed / unsealed).</td></tr>
 * </table>
 *
 * <h3>Navigation Resolution Architecture</h3>
 * <pre>
 *   [Gutter Marker on AST Element]
 *                 │
 *                 ├─► DISCLOSE Token ────────► Tooltip: ZK Privacy Boundary
 *                 ├─► IMPLEMENTS Token ──────► Upward navigation to Interface
 *                 │
 *                 └─► IDENTIFIER Token
 *                       ├─► ExternalContractDecl ──► Search project for 'contract implements'
 *                       ├─► ExternalCircuit ───────► Search implementors for matching circuit
 *                       ├─► WitnessDeclaration ────► Witness indicator
 *                       ├─► CircuitDefinition ─────► Check super circuit (UP) or Exported (Lightning)
 *                       └─► LedgerDeclaration ─────► Ledger state indicator
 * </pre>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Read Action:</b> Invoked on background daemon analysis threads under a platform read lock.</li>
 *   <li><b>Project Search:</b> Multi-file lookups utilize {@link FileTypeIndex} scoped to {@link GlobalSearchScope#projectScope(Project)}.</li>
 * </ul>
 *
 * @see NavigationGutterIconBuilder
 * @see CompactExternalContractDeclaration
 * @see CompactContractImplementsDeclaration
 */
public class CompactLineMarkerProvider implements LineMarkerProvider {

  /**
   * Computes gutter icon presentation and navigation targets for a single PSI element.
   *
   * @param element the leaf PSI element being inspected
   * @return {@link LineMarkerInfo} describing the icon, tooltip, and navigation target, or {@code null} if none
   */
  @Override
  public @Nullable LineMarkerInfo<?> getLineMarkerInfo(@NotNull PsiElement element) {
    if (element.getNode() == null) {
      return null;
    }

    // 1. Check for a disclosed keyword token (Zero-Knowledge privacy boundary)
    if (element.getNode().getElementType() == CompactTokenTypes.DISCLOSE) {
      return new LineMarkerInfo<>(
          element,
          element.getTextRange(),
          AllIcons.Nodes.KeymapEditor,
          ignored -> "Zero-Knowledge boundary: disclosing private witness data into circuit",
          null,
          GutterIconRenderer.Alignment.RIGHT,
          () -> "ZK Disclosure Boundary"
      );
    }

    // 2. Check for 'implements' keyword inside 'contract implements InterfaceName'
    if (element.getNode().getElementType() == CompactTokenTypes.IMPLEMENTS) {
      PsiElement parent = element.getParent();
      if (parent instanceof CompactContractImplementsDeclaration impl) {
        CompactExternalContractDeclaration iface = impl.resolveInterface();
        if (iface != null) {
          String ifaceName = iface.getName() != null ? iface.getName() : "interface";
          return NavigationGutterIconBuilder.create(AllIcons.Gutter.ImplementingMethod)
              .setTarget(iface)
              .setTooltipText("Implements contract interface '" + ifaceName + "'")
              .setAlignment(GutterIconRenderer.Alignment.RIGHT)
              .createLineMarkerInfo(element);
        }
      }
    }

    // 3. Check identifier tokens of declarations
    if (element.getNode().getElementType() == CompactTokenTypes.IDENTIFIER) {
      PsiElement parent = element.getParent();

      // 3a. Interface declaration name: navigate DOWN to all implementations across the project
      if (parent instanceof CompactExternalContractDeclaration contractDecl && element.equals(contractDecl.getNameIdentifier())) {
        List<PsiElement> implementations = findContractImplementations(contractDecl);
        if (!implementations.isEmpty()) {
          String name = contractDecl.getName() != null ? contractDecl.getName() : "contract";
          return NavigationGutterIconBuilder.create(AllIcons.Gutter.ImplementedMethod)
              .setTargets(implementations)
              .setTooltipText("Contract interface is implemented by " + implementations.size() + (implementations.size() == 1 ? " contract" : " contracts"))
              .setPopupTitle("Choose Implementation of " + name)
              .setAlignment(GutterIconRenderer.Alignment.RIGHT)
              .createLineMarkerInfo(element);
        }
      }

      // 3b. Interface circuit name: navigate DOWN to concrete circuit implementations across the project
      if (parent instanceof CompactExternalCircuit externalCircuit && element.equals(externalCircuit.getNameIdentifier())) {
        CompactExternalContractDeclaration contractDecl = PsiTreeUtil.getParentOfType(externalCircuit, CompactExternalContractDeclaration.class);
        if (contractDecl != null) {
          List<PsiElement> implementations = findCircuitImplementations(contractDecl, externalCircuit.getName());
          if (!implementations.isEmpty()) {
            String cName = externalCircuit.getName() != null ? externalCircuit.getName() : "circuit";
            return NavigationGutterIconBuilder.create(AllIcons.Gutter.ImplementedMethod)
                .setTargets(implementations)
                .setTooltipText("Circuit '" + cName + "' is implemented in " + implementations.size() + (implementations.size() == 1 ? " contract" : " contracts"))
                .setPopupTitle("Choose Implementation of " + cName)
                .setAlignment(GutterIconRenderer.Alignment.RIGHT)
                .createLineMarkerInfo(element);
          }
        }
      }

      // 3c. Witness declaration: private off-chain witness query
      if (parent instanceof CompactWitnessDeclaration witness && element.equals(witness.getNameIdentifier())) {
        return new LineMarkerInfo<>(
            element,
            element.getTextRange(),
            AllIcons.Nodes.AbstractMethod,
            ignored -> "Private off-chain witness query '" + element.getText() + "'",
            null,
            GutterIconRenderer.Alignment.RIGHT,
            () -> "Private Witness Query"
        );
      }

      // 3d. Circuit definition: check if it implements an interface circuit (navigate UP), otherwise show exported circuit marker
      if (parent instanceof CompactCircuitDefinition circuit && element.equals(circuit.getNameIdentifier())) {
        CompactExternalCircuit superCircuit = findSuperCircuit(circuit);
        if (superCircuit != null) {
          CompactExternalContractDeclaration iface = PsiTreeUtil.getParentOfType(superCircuit, CompactExternalContractDeclaration.class);
          String ifaceName = iface != null && iface.getName() != null ? iface.getName() : "interface";
          return NavigationGutterIconBuilder.create(AllIcons.Gutter.ImplementingMethod)
              .setTarget(superCircuit)
              .setTooltipText("Implements circuit '" + circuit.getName() + "' from '" + ifaceName + "'")
              .setAlignment(GutterIconRenderer.Alignment.RIGHT)
              .createLineMarkerInfo(element);
        }

        if (circuit.isExported()) {
          return new LineMarkerInfo<>(
              element,
              element.getTextRange(),
              AllIcons.Actions.Lightning,
              ignored -> "Exported on-chain ZK circuit '" + element.getText() + "'",
              null,
              GutterIconRenderer.Alignment.RIGHT,
              () -> "Exported ZK Circuit"
          );
        }
      }

      // 3e. Ledger declaration: on-chain ledger state
      if (parent instanceof CompactLedgerDeclaration ledger && element.equals(ledger.getNameIdentifier())) {
        String typeDesc = ledger.isSealed() ? "Sealed on-chain ledger state" : "On-chain ledger state";
        return new LineMarkerInfo<>(
            element,
            element.getTextRange(),
            AllIcons.Nodes.DataTables,
            ignored -> typeDesc + " '" + element.getText() + "'",
            null,
            GutterIconRenderer.Alignment.RIGHT,
            () -> "Ledger State"
        );
      }
    }

    return null;
  }

  /**
   * Searches the current file and all other Compact files in the project for contract definitions
   * declaring that they implement the specified interface contract declaration.
   *
   * @param contractDecl the external contract interface declaration
   * @return list of matching {@link CompactContractImplementsDeclaration} PSI elements
   */
  public static @NotNull List<PsiElement> findContractImplementations(@NotNull CompactExternalContractDeclaration contractDecl) {
    List<PsiElement> result = new ArrayList<>();
    String name = contractDecl.getName();
    if (name == null || name.isEmpty()) {
      return result;
    }

    // Check implementations within the same file first
    PsiFile containingFile = contractDecl.getContainingFile();
    if (containingFile instanceof CompactFile cf) {
      for (CompactContractImplementsDeclaration impl : PsiTreeUtil.findChildrenOfType(cf, CompactContractImplementsDeclaration.class)) {
        if (contractDecl.equals(impl.resolveInterface())) {
          result.add(impl);
        }
      }
    }

    // Search across all other project files via FileTypeIndex
    Project project = contractDecl.getProject();
    Collection<VirtualFile> files = FileTypeIndex.getFiles(CompactFileType.INSTANCE, GlobalSearchScope.projectScope(project));
    for (VirtualFile file : files) {
      if (containingFile != null && file.equals(containingFile.getVirtualFile())) {
        continue;
      }
      PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
      if (psiFile instanceof CompactFile cf) {
        for (CompactContractImplementsDeclaration impl : PsiTreeUtil.findChildrenOfType(cf, CompactContractImplementsDeclaration.class)) {
          if (contractDecl.equals(impl.resolveInterface())) {
            result.add(impl);
          }
        }
      }
    }
    return result;
  }

  /**
   * Searches for concrete circuit definitions matching {@code circuitName} within any contract
   * implementing {@code contractDecl}.
   *
   * @param contractDecl the external contract interface declaration
   * @param circuitName the circuit identifier to locate
   * @return list of concrete {@link CompactCircuitDefinition} elements
   */
  public static @NotNull List<PsiElement> findCircuitImplementations(
      @NotNull CompactExternalContractDeclaration contractDecl,
      @Nullable String circuitName
  ) {
    List<PsiElement> result = new ArrayList<>();
    if (circuitName == null || circuitName.isEmpty()) {
      return result;
    }

    for (PsiElement impl : findContractImplementations(contractDecl)) {
      PsiFile implFile = impl.getContainingFile();
      if (implFile instanceof CompactFile cf) {
        for (CompactCircuitDefinition circuit : PsiTreeUtil.findChildrenOfType(cf, CompactCircuitDefinition.class)) {
          if (circuitName.equals(circuit.getName())) {
            result.add(circuit);
          }
        }
      }
    }
    return result;
  }

  /**
   * Resolves the corresponding interface circuit declaration in the implemented contract interface
   * if this circuit definition overrides one.
   *
   * @param circuit the concrete circuit definition
   * @return the matching {@link CompactExternalCircuit} in the implemented interface, or {@code null}
   */
  public static @Nullable CompactExternalCircuit findSuperCircuit(@NotNull CompactCircuitDefinition circuit) {
    String circuitName = circuit.getName();
    if (circuitName == null || circuitName.isEmpty()) {
      return null;
    }
    PsiFile file = circuit.getContainingFile();
    if (!(file instanceof CompactFile cf)) {
      return null;
    }
    for (CompactContractImplementsDeclaration impl : PsiTreeUtil.findChildrenOfType(cf, CompactContractImplementsDeclaration.class)) {
      CompactExternalContractDeclaration iface = impl.resolveInterface();
      if (iface != null) {
        for (CompactExternalCircuit ifaceCircuit : iface.getCircuits()) {
          if (Objects.equals(circuitName, ifaceCircuit.getName())) {
            return ifaceCircuit;
          }
        }
      }
    }
    return null;
  }
}
