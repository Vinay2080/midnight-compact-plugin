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
 * Line marker provider adding visual gutter indicators and navigation for:
 * <ul>
 *   <li>Interface & implementation hierarchy (navigating between contract interfaces and concrete implementors).</li>
 *   <li>Circuit-level implementation navigation (jumping between abstract interface circuits and concrete circuit definitions).</li>
 *   <li>ZK privacy boundaries (disclose keyword indicators).</li>
 *   <li>Private off-chain witness query declarations.</li>
 *   <li>Exported on-chain ZK circuits.</li>
 *   <li>Sealed and unsealed ledger state variables.</li>
 * </ul>
 */
public class CompactLineMarkerProvider implements LineMarkerProvider {

  @Override
  public @Nullable LineMarkerInfo<?> getLineMarkerInfo(@NotNull PsiElement element) {
    if (element.getNode() == null) {
      return null;
    }

    // 1. Check for a disclosed keyword token (ZK boundary)
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

    // 2. Check for 'implements' keyword inside 'contract implements InterfaceName';
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

      // 3b. Interface circuit name: navigate DOWN to concrete circuit implementations
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

      // 3c. Witness declaration
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

      // 3e. Ledger declaration
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

  public static @NotNull List<PsiElement> findContractImplementations(@NotNull CompactExternalContractDeclaration contractDecl) {
    List<PsiElement> result = new ArrayList<>();
    String name = contractDecl.getName();
    if (name == null || name.isEmpty()) {
      return result;
    }

    PsiFile containingFile = contractDecl.getContainingFile();
    if (containingFile instanceof CompactFile cf) {
      for (CompactContractImplementsDeclaration impl : PsiTreeUtil.findChildrenOfType(cf, CompactContractImplementsDeclaration.class)) {
        if (contractDecl.equals(impl.resolveInterface())) {
          result.add(impl);
        }
      }
    }

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
