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
    if (element instanceof Navigatable && ((Navigatable) element).canNavigate()) {
      ((Navigatable) element).navigate(requestFocus);
    }
  }

  @Override
  public boolean canNavigate() {
    return element instanceof Navigatable && ((Navigatable) element).canNavigate();
  }

  @Override
  public boolean canNavigateToSource() {
    return element instanceof Navigatable && ((Navigatable) element).canNavigateToSource();
  }

  @Override
  public @NotNull ItemPresentation getPresentation() {
    return new PresentationData(getPresentableText(), getLocationString(), getIcon(), null);
  }

  public @Nullable String getPresentableText() {
    switch (element) {
      case CompactFile compactFile -> {
        return compactFile.getName();
      }
      case CompactCircuitDefinition _ -> {
        String name = ((PsiNamedElement) element).getName();
        return name != null ? "circuit " + name : "circuit";
      }
      case CompactWitnessDeclaration _ -> {
        String name = ((PsiNamedElement) element).getName();
        return name != null ? "witness " + name : "witness";
      }
      default -> {
      }
    }
    if (element instanceof CompactExternalContractDeclaration || element instanceof CompactContractImplementsDeclaration) {
      if (element instanceof PsiNamedElement) {
        String name = ((PsiNamedElement) element).getName();
        if (name != null) {
          return "contract " + name;
        }
      }
      return "contract";
    }
    switch (element) {
      case CompactModuleDefinition _ -> {
        String name = ((PsiNamedElement) element).getName();
        return name != null ? "module " + name : "module";
      }
      case CompactStructDefinition _ -> {
        String name = ((PsiNamedElement) element).getName();
        return name != null ? "struct " + name : "struct";
      }
      case CompactStructFieldImpl compactStructField -> {
        String name = compactStructField.getName();
        return name != null ? name : "field";
      }
      case CompactEnumDefinition _ -> {
        String name = ((PsiNamedElement) element).getName();
        return name != null ? "enum " + name : "enum";
      }
      case CompactEnumMemberImpl compactEnumMember -> {
        String name = compactEnumMember.getName();
        return name != null ? name : "member";
      }
      case CompactTypeDefinition _ -> {
        String name = ((PsiNamedElement) element).getName();
        return name != null ? "type " + name : "type";
      }
      case CompactConstructorDeclaration _ -> {
        return "constructor";
      }
      case CompactLedgerDeclaration _ -> {
        if (((PsiNamedElement) element).getName() != null) {
          return "ledger " + ((PsiNamedElement) element).getName();
        }
        return "ledger";
      }
      default -> {
      }
    }
    if (element instanceof CompactPatternImpl || element instanceof CompactConstBindingImpl) {
      String name = ((PsiNamedElement) element).getName();
      return name != null ? "const " + name : "const";
    }
    switch (element) {
      case CompactPragmaForm _ -> {
        return "pragma " + element.getText().trim();
      }
      case CompactIncludeDeclaration _ -> {
        return "include " + element.getText().replace("include", "").replace(";", "").trim();
      }
      case CompactImportDeclaration _ -> {
        return "import " + element.getText().replace("import", "").replace(";", "").trim();
      }
      case CompactExportDeclaration _ -> {
        return "export " + element.getText().replace("export", "").replace(";", "").trim();
      }
      default -> {
      }
    }
    if (element.getNode().getElementType() == CompactElementTypes.EXTERNAL_CIRCUIT) {
      PsiElement id = PsiTreeUtil.findChildOfType(element, CompactReferenceExprImpl.class);
      return id != null ? "circuit " + id.getText() : "circuit " + element.getText();
    }
    if (element instanceof PsiNamedElement) {
      return ((PsiNamedElement) element).getName();
    }
    return element.getText();
  }

  public @Nullable String getLocationString() {
    PsiElement parent = element.getParent();
    if (parent instanceof CompactModuleDefinition module) {
      return module.getName();
    }
    return null;
  }

  public @Nullable Icon getIcon() {
    if (element instanceof CompactFile) {
      return MidnightIcons.FILE;
    }
    if (element instanceof CompactCircuitDefinition || element.getNode().getElementType() == CompactElementTypes.EXTERNAL_CIRCUIT) {
      return AllIcons.Nodes.Method;
    }
    if (element instanceof CompactWitnessDeclaration) {
      return AllIcons.Nodes.AbstractMethod;
    }
    if (element instanceof CompactExternalContractDeclaration || element instanceof CompactContractImplementsDeclaration) {
      return AllIcons.Nodes.Class;
    }
    switch (element) {
      case CompactModuleDefinition _ -> {
        return AllIcons.Nodes.Package;
      }
      case CompactStructDefinition _ -> {
        return AllIcons.Nodes.Record;
      }
      case CompactStructFieldImpl _ -> {
        return AllIcons.Nodes.Field;
      }
      case CompactEnumDefinition _ -> {
        return AllIcons.Nodes.Enum;
      }
      case CompactEnumMemberImpl _ -> {
        return AllIcons.Nodes.Field;
      }
      case CompactTypeDefinition _ -> {
        return AllIcons.Nodes.Type;
      }
      case CompactConstructorDeclaration _ -> {
        return AllIcons.Nodes.ClassInitializer;
      }
      case CompactLedgerDeclaration _ -> {
        return AllIcons.Nodes.DataTables;
      }
      default -> {
      }
    }
    if (element instanceof CompactPatternImpl || element instanceof CompactConstBindingImpl) {
      return AllIcons.Nodes.Constant;
    }
    return switch (element) {
      case CompactPragmaForm _ -> AllIcons.Nodes.Tag;
      case CompactIncludeDeclaration _ -> AllIcons.Nodes.Include;
      case CompactImportDeclaration _ -> AllIcons.Nodes.Tag;
      case CompactExportDeclaration _ -> AllIcons.Nodes.Deploy;
      default -> null;
    };
  }

  @Override
  public TreeElement @NotNull [] getChildren() {
    List<TreeElement> children = new ArrayList<>();
    if (element instanceof CompactFile) {
      for (PsiElement child : element.getChildren()) {
        addStructureItem(child, children);
      }
      return children.toArray(new TreeElement[0]);
    }
    if (element instanceof CompactExternalContractDeclaration) {
      for (PsiElement child : element.getChildren()) {
        if (child.getNode().getElementType() == CompactElementTypes.EXTERNAL_CIRCUIT) {
          children.add(new CompactStructureViewElement(child));
        }
      }
      return children.toArray(new TreeElement[0]);
    }
    if (element instanceof CompactContractImplementsDeclaration || element instanceof CompactModuleDefinition) {
      collectInnerDeclarations(element, children);
      return children.toArray(new TreeElement[0]);
    }
    if (element instanceof CompactStructDefinition) {
      for (CompactStructFieldImpl field : PsiTreeUtil.findChildrenOfType(element, CompactStructFieldImpl.class)) {
        children.add(new CompactStructureViewElement(field));
      }
      return children.toArray(new TreeElement[0]);
    }
    if (element instanceof CompactEnumDefinition) {
      for (CompactEnumMemberImpl member : PsiTreeUtil.findChildrenOfType(element, CompactEnumMemberImpl.class)) {
        children.add(new CompactStructureViewElement(member));
      }
      return children.toArray(new TreeElement[0]);
    }
    return EMPTY_ARRAY;
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
