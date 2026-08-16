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
    return new PresentationData(getPresentableText(), getLocationString(), getIcon(false), null);
  }

  public @Nullable String getPresentableText() {
    if (element instanceof CompactFile) {
      return ((CompactFile) element).getName();
    }
    if (element instanceof CompactCircuitDefinition) {
      String name = ((PsiNamedElement) element).getName();
      return name != null ? "circuit " + name : "circuit";
    }
    if (element instanceof CompactWitnessDeclaration) {
      String name = ((PsiNamedElement) element).getName();
      return name != null ? "witness " + name : "witness";
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
    if (element instanceof CompactModuleDefinition) {
      String name = ((PsiNamedElement) element).getName();
      return name != null ? "module " + name : "module";
    }
    if (element instanceof CompactStructDefinition) {
      String name = ((PsiNamedElement) element).getName();
      return name != null ? "struct " + name : "struct";
    }
    if (element instanceof CompactStructFieldImpl) {
      String name = ((CompactStructFieldImpl) element).getName();
      return name != null ? name : "field";
    }
    if (element instanceof CompactEnumDefinition) {
      String name = ((PsiNamedElement) element).getName();
      return name != null ? "enum " + name : "enum";
    }
    if (element instanceof CompactEnumMemberImpl) {
      String name = ((CompactEnumMemberImpl) element).getName();
      return name != null ? name : "member";
    }
    if (element instanceof CompactTypeDefinition) {
      String name = ((PsiNamedElement) element).getName();
      return name != null ? "type " + name : "type";
    }
    if (element instanceof CompactConstructorDeclaration) {
      return "constructor";
    }
    if (element instanceof CompactLedgerDeclaration) {
      if (element instanceof PsiNamedElement && ((PsiNamedElement) element).getName() != null) {
        return "ledger " + ((PsiNamedElement) element).getName();
      }
      return "ledger";
    }
    if (element instanceof CompactPatternImpl || element instanceof CompactConstBindingImpl) {
      String name = ((PsiNamedElement) element).getName();
      return name != null ? "const " + name : "const";
    }
    if (element instanceof CompactPragmaForm) {
      return "pragma " + element.getText().trim();
    }
    if (element instanceof CompactIncludeDeclaration) {
      return "include " + element.getText().replace("include", "").replace(";", "").trim();
    }
    if (element instanceof CompactImportDeclaration) {
      return "import " + element.getText().replace("import", "").replace(";", "").trim();
    }
    if (element instanceof CompactExportDeclaration) {
      return "export " + element.getText().replace("export", "").replace(";", "").trim();
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
    return null;
  }

  public @Nullable Icon getIcon(boolean unused) {
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
    if (element instanceof CompactModuleDefinition) {
      return AllIcons.Nodes.Package;
    }
    if (element instanceof CompactStructDefinition) {
      return AllIcons.Nodes.Record;
    }
    if (element instanceof CompactStructFieldImpl) {
      return AllIcons.Nodes.Field;
    }
    if (element instanceof CompactEnumDefinition) {
      return AllIcons.Nodes.Enum;
    }
    if (element instanceof CompactEnumMemberImpl) {
      return AllIcons.Nodes.Field;
    }
    if (element instanceof CompactTypeDefinition) {
      return AllIcons.Nodes.Type;
    }
    if (element instanceof CompactConstructorDeclaration) {
      return AllIcons.Nodes.ClassInitializer;
    }
    if (element instanceof CompactLedgerDeclaration) {
      return AllIcons.Nodes.DataTables;
    }
    if (element instanceof CompactPatternImpl || element instanceof CompactConstBindingImpl) {
      return AllIcons.Nodes.Constant;
    }
    if (element instanceof CompactPragmaForm) {
      return AllIcons.Nodes.Tag;
    }
    if (element instanceof CompactIncludeDeclaration) {
      return AllIcons.Nodes.Include;
    }
    if (element instanceof CompactImportDeclaration) {
      return AllIcons.Nodes.Tag;
    }
    if (element instanceof CompactExportDeclaration) {
      return AllIcons.Nodes.Deploy;
    }
    return null;
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
