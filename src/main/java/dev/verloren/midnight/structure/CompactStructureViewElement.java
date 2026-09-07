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
      case CompactCircuitDefinition circuit -> {
        String name = circuit.getName();
        return name != null ? "circuit " + name : "circuit";
      }
      case CompactWitnessDeclaration witness -> {
        String name = witness.getName();
        return name != null ? "witness " + name : "witness";
      }
      default -> {
      }
    }
    if (element instanceof CompactExternalContractDeclaration || element instanceof CompactContractImplementsDeclaration) {
      if (element instanceof PsiNamedElement named) {
        String name = named.getName();
        if (name != null) {
          return "contract " + name;
        }
      }
      return "contract";
    }
    switch (element) {
      case CompactModuleDefinition module -> {
        String name = module.getName();
        return name != null ? "module " + name : "module";
      }
      case CompactStructDefinition struct -> {
        String name = struct.getName();
        return name != null ? "struct " + name : "struct";
      }
      case CompactStructFieldImpl field -> {
        String name = field.getName();
        return name != null ? name : "field";
      }
      case CompactEnumDefinition enumDef -> {
        String name = enumDef.getName();
        return name != null ? "enum " + name : "enum";
      }
      case CompactEnumMemberImpl member -> {
        String name = member.getName();
        return name != null ? name : "member";
      }
      case CompactTypeDefinition typeDef -> {
        String name = typeDef.getName();
        return name != null ? "type " + name : "type";
      }
      case CompactConstructorDeclaration ignored -> {
        return "constructor";
      }
      case CompactLedgerDeclaration ledger -> {
        String name = ledger.getName();
        if (name != null) {
          return "ledger " + name;
        }
        return "ledger";
      }
      default -> {
      }
    }
    if (element instanceof CompactPatternImpl || element instanceof CompactConstBindingImpl) {
      PsiNamedElement named = (PsiNamedElement) element;
      String name = named.getName();
      return name != null ? "const " + name : "const";
    }
    switch (element) {
      case CompactPragmaForm pragma -> {
        return "pragma " + pragma.getText().trim();
      }
      case CompactIncludeDeclaration include -> {
        return "include " + include.getText().replace("include", "").replace(";", "").trim();
      }
      case CompactImportDeclaration importDecl -> {
        return "import " + importDecl.getText().replace("import", "").replace(";", "").trim();
      }
      case CompactExportDeclaration exportDecl -> {
        return "export " + exportDecl.getText().replace("export", "").replace(";", "").trim();
      }
      default -> {
      }
    }
    if (element.getNode().getElementType() == CompactElementTypes.EXTERNAL_CIRCUIT) {
      PsiElement id = PsiTreeUtil.findChildOfType(element, CompactReferenceExprImpl.class);
      return id != null ? "circuit " + id.getText() : "circuit " + element.getText();
    }
    if (element instanceof PsiNamedElement named) {
      return named.getName();
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
      case CompactModuleDefinition ignored -> {
        return AllIcons.Nodes.Package;
      }
      case CompactStructDefinition ignored -> {
        return AllIcons.Nodes.Record;
      }
      case CompactStructFieldImpl ignored -> {
        return AllIcons.Nodes.Field;
      }
      case CompactEnumDefinition ignored -> {
        return AllIcons.Nodes.Enum;
      }
      case CompactEnumMemberImpl ignored -> {
        return AllIcons.Nodes.Field;
      }
      case CompactTypeDefinition ignored -> {
        return AllIcons.Nodes.Type;
      }
      case CompactConstructorDeclaration ignored -> {
        return AllIcons.Nodes.ClassInitializer;
      }
      case CompactLedgerDeclaration ignored -> {
        return AllIcons.Nodes.DataTables;
      }
      default -> {
      }
    }
    if (element instanceof CompactPatternImpl || element instanceof CompactConstBindingImpl) {
      return AllIcons.Nodes.Constant;
    }
    return switch (element) {
      case CompactPragmaForm ignored -> AllIcons.Nodes.Tag;
      case CompactIncludeDeclaration ignored -> AllIcons.Nodes.Include;
      case CompactImportDeclaration ignored -> AllIcons.Nodes.Tag;
      case CompactExportDeclaration ignored -> AllIcons.Nodes.Deploy;
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
