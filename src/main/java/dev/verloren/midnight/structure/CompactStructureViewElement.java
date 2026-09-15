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
 * Tree node adapter for Compact PSI elements displayed in the Structure View tool window.
 *
 * <p>Implements {@link StructureViewTreeElement} and {@link Navigatable} to provide:
 * <ul>
 *   <li><b>Presentable Text:</b> Formatted labels for circuits, witnesses, structs, fields, and modules.</li>
 *   <li><b>Icons:</b> Specific IntelliJ standard icons reflecting element semantics.</li>
 *   <li><b>Child Elements:</b> Hierarchical children for modules, contracts, structs, and enums.</li>
 * </ul>
 * </p>
 */
public class CompactStructureViewElement implements StructureViewTreeElement, Navigatable {
  private static final TreeElement[] EMPTY_TREE_ELEMENTS = new TreeElement[0];
  private final PsiElement element;

  public CompactStructureViewElement(@NotNull PsiElement element) {
    this.element = element;
  }

  @Override
  public Object getValue() {
    return element;
  }

  @Override
  public void navigate(boolean requestFocus) {
    if (element instanceof Navigatable navigatable && navigatable.canNavigate()) {
      navigatable.navigate(requestFocus);
    }
  }

  @Override
  public boolean canNavigate() {
    return element instanceof Navigatable navigatable && navigatable.canNavigate();
  }

  @Override
  public boolean canNavigateToSource() {
    return element instanceof Navigatable navigatable && navigatable.canNavigateToSource();
  }

  @Override
  public @NotNull ItemPresentation getPresentation() {
    return new PresentationData(getPresentableText(), getLocationString(), getIcon(), null);
  }

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
        if (element.getNode() != null && element.getNode().getElementType() == CompactElementTypes.EXTERNAL_CIRCUIT) {
          PsiElement id = PsiTreeUtil.findChildOfType(element, CompactReferenceExprImpl.class);
          yield id != null ? "circuit " + id.getText() : "circuit " + element.getText();
        }
        if (element instanceof PsiNamedElement named) {
          yield named.getName();
        }
        yield element.getText();
      }
    };
  }

  public @Nullable String getLocationString() {
    PsiElement parent = element.getParent();
    if (parent instanceof CompactModuleDefinition module) {
      return module.getName();
    }
    return null;
  }

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

  @Override
  public TreeElement @NotNull [] getChildren() {
    List<TreeElement> children = new ArrayList<>();
    switch (element) {
      case CompactFile ignored -> {
        for (PsiElement child : element.getChildren()) {
          addStructureItem(child, children);
        }
      }
      case CompactExternalContractDeclaration ignored -> {
        for (PsiElement child : element.getChildren()) {
          if (child.getNode().getElementType() == CompactElementTypes.EXTERNAL_CIRCUIT) {
            children.add(new CompactStructureViewElement(child));
          }
        }
      }
      case CompactContractImplementsDeclaration ignored ->
          collectInnerDeclarations(element, children);
      case CompactModuleDefinition ignored ->
          collectInnerDeclarations(element, children);
      case CompactStructDefinition ignored -> {
        for (CompactStructFieldImpl field : PsiTreeUtil.findChildrenOfType(element, CompactStructFieldImpl.class)) {
          children.add(new CompactStructureViewElement(field));
        }
      }
      case CompactEnumDefinition ignored -> {
        for (CompactEnumMemberImpl member : PsiTreeUtil.findChildrenOfType(element, CompactEnumMemberImpl.class)) {
          children.add(new CompactStructureViewElement(member));
        }
      }
      default -> {}
    }
    return children.isEmpty() ? EMPTY_TREE_ELEMENTS : children.toArray(EMPTY_TREE_ELEMENTS);
  }

  private static void addStructureItem(@NotNull PsiElement elem, @NotNull List<TreeElement> out) {
    if (elem.getNode().getElementType() == CompactElementTypes.CONST_STATEMENT) {
      for (CompactPatternImpl pattern : PsiTreeUtil.findChildrenOfType(elem, CompactPatternImpl.class)) {
        out.add(new CompactStructureViewElement(pattern));
      }
      return;
    }
    if (elem.getNode().getElementType() == CompactElementTypes.EXPORT_FORM) {
      out.add(new CompactStructureViewElement(elem));
      return;
    }
    if (isStructureItem(elem)) {
      out.add(new CompactStructureViewElement(elem));
    }
  }

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

  private static void collectInnerDeclarations(@NotNull PsiElement container, @NotNull List<TreeElement> out) {
    for (PsiElement child : container.getChildren()) {
      if (child instanceof CompactBlock) {
        collectInnerDeclarations(child, out);
      } else {
        addStructureItem(child, out);
      }
    }
  }
}
