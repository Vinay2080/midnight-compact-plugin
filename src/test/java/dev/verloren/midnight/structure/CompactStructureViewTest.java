package dev.verloren.midnight.structure;

import com.intellij.icons.AllIcons;
import com.intellij.ide.structureView.StructureViewBuilder;
import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.structureView.TreeBasedStructureViewBuilder;
import com.intellij.ide.util.treeView.smartTree.TreeElement;
import com.intellij.lang.LanguageParserDefinitions;
import com.intellij.lang.LanguageStructureViewBuilder;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import dev.verloren.midnight.CompactFileType;
import dev.verloren.midnight.CompactLanguage;
import dev.verloren.midnight.icons.MidnightIcons;
import dev.verloren.midnight.parser.CompactParserDefinition;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@SuppressWarnings("deprecation")
public class CompactStructureViewTest extends BasePlatformTestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    LanguageParserDefinitions.INSTANCE.addExplicitExtension(
        CompactLanguage.INSTANCE,
        new CompactParserDefinition()
    );
    LanguageStructureViewBuilder.INSTANCE.addExplicitExtension(
        CompactLanguage.INSTANCE,
        new CompactStructureViewFactory()
    );
  }

  public void testFactoryRegistration() {
    PsiFile file = myFixture.configureByText(CompactFileType.INSTANCE, "circuit foo(): Void {}");
    StructureViewBuilder builder = LanguageStructureViewBuilder.INSTANCE.getStructureViewBuilder(file);
    assertNotNull("StructureViewBuilder must be registered for Compact", builder);
    assertTrue("Builder should be TreeBasedStructureViewBuilder", builder instanceof TreeBasedStructureViewBuilder);
  }

  public void testTopLevelDeclarations() {
    String code = """
        pragma language_version >= 0.20.0;
        include "library.compact";
        import { Hash, verify as check } from Crypto;
        export { transfer, State };
        
        type Hash = Bytes<32>;
        const MAX_RETRIES = 5;
        
        circuit transfer(): Void {}
        witness getSecret(): Field {}
        """;
    PsiFile file = myFixture.configureByText(CompactFileType.INSTANCE, code);
    CompactStructureViewModel model = new CompactStructureViewModel(file, null);
    StructureViewTreeElement root = model.getRoot();
    
    assertEquals(file.getName(), root.getPresentation().getPresentableText());
    assertEquals(MidnightIcons.FILE, root.getPresentation().getIcon(false));
    
    TreeElement[] children = root.getChildren();
    List<String> names = Arrays.stream(children)
        .map(c -> c.getPresentation().getPresentableText())
        .toList();

    assertTrue(names.stream().anyMatch(n -> n.contains("pragma")));
    assertTrue(names.stream().anyMatch(n -> n.contains("include")));
    assertTrue(names.stream().anyMatch(n -> n.contains("import")));
    assertTrue(names.stream().anyMatch(n -> n.contains("export")));
    assertTrue(names.contains("type Hash"));
    assertTrue(names.contains("const MAX_RETRIES"));
    assertTrue(names.contains("circuit transfer"));
    assertTrue(names.contains("witness getSecret"));
  }

  public void testExternalContractHierarchy() {
    String code = """
        contract Oracle {
          circuit query(id: Uint): Field;
          pure circuit getVersion(): Uint;
        }
        """;
    PsiFile file = myFixture.configureByText(CompactFileType.INSTANCE, code);
    CompactStructureViewModel model = new CompactStructureViewModel(file, null);
    StructureViewTreeElement root = model.getRoot();
    
    TreeElement[] topLevel = root.getChildren();
    assertEquals(1, topLevel.length);
    assertEquals("contract Oracle", topLevel[0].getPresentation().getPresentableText());
    assertEquals(AllIcons.Nodes.Class, topLevel[0].getPresentation().getIcon(false));
    
    TreeElement[] contractChildren = topLevel[0].getChildren();
    List<String> childNames = Arrays.stream(contractChildren)
        .map(c -> c.getPresentation().getPresentableText())
        .toList();

    assertEquals(2, contractChildren.length);
    assertTrue(childNames.stream().anyMatch(n -> n.contains("circuit query")));
    assertTrue(childNames.stream().anyMatch(n -> n.contains("circuit getVersion")));
  }

  public void testTopLevelContractBodyHierarchy() {
    String code = """
        ledger count: Uint;
        
        constructor(initial: Uint) {
          count = initial;
        }
        
        circuit increment(): Void {
          count = count + 1;
        }
        
        witness readSecret(): Field {}
        """;
    PsiFile file = myFixture.configureByText(CompactFileType.INSTANCE, code);
    CompactStructureViewModel model = new CompactStructureViewModel(file, null);
    StructureViewTreeElement root = model.getRoot();
    
    TreeElement[] topLevel = root.getChildren();
    List<String> names = Arrays.stream(topLevel)
        .map(c -> c.getPresentation().getPresentableText())
        .toList();

    assertTrue(names.stream().anyMatch(n -> n.contains("ledger")));
    assertTrue(names.contains("constructor"));
    assertTrue(names.contains("circuit increment"));
    assertTrue(names.contains("witness readSecret"));
  }

  public void testStructFieldsHierarchy() {
    String code = """
        struct Point {
          x: Field,
          y: Field,
        }
        """;
    PsiFile file = myFixture.configureByText(CompactFileType.INSTANCE, code);
    CompactStructureViewModel model = new CompactStructureViewModel(file, null);
    StructureViewTreeElement root = model.getRoot();
    
    TreeElement[] topLevel = root.getChildren();
    assertEquals(1, topLevel.length);
    assertEquals("struct Point", topLevel[0].getPresentation().getPresentableText());
    assertEquals(AllIcons.Nodes.Record, topLevel[0].getPresentation().getIcon(false));
    
    TreeElement[] fields = topLevel[0].getChildren();
    assertEquals(2, fields.length);
    assertEquals("x", fields[0].getPresentation().getPresentableText());
    assertEquals(AllIcons.Nodes.Field, fields[0].getPresentation().getIcon(false));
    assertEquals("y", fields[1].getPresentation().getPresentableText());
  }

  public void testEnumMembersHierarchy() {
    String code = """
        enum Color {
          Red,
          Green,
          Blue,
        }
        """;
    PsiFile file = myFixture.configureByText(CompactFileType.INSTANCE, code);
    CompactStructureViewModel model = new CompactStructureViewModel(file, null);
    StructureViewTreeElement root = model.getRoot();
    
    TreeElement[] topLevel = root.getChildren();
    assertEquals(1, topLevel.length);
    assertEquals("enum Color", topLevel[0].getPresentation().getPresentableText());
    assertEquals(AllIcons.Nodes.Enum, topLevel[0].getPresentation().getIcon(false));
    
    TreeElement[] members = topLevel[0].getChildren();
    assertEquals(3, members.length);
    assertEquals("Red", members[0].getPresentation().getPresentableText());
    assertEquals(AllIcons.Nodes.Field, members[0].getPresentation().getIcon(false));
    assertEquals("Green", members[1].getPresentation().getPresentableText());
    assertEquals("Blue", members[2].getPresentation().getPresentableText());
  }

  public void testNestedModuleHierarchy() {
    String code = """
        module Outer {
          module Inner {
            circuit helper(): Void {}
          }
          type LocalAlias = Uint;
        }
        """;
    PsiFile file = myFixture.configureByText(CompactFileType.INSTANCE, code);
    CompactStructureViewModel model = new CompactStructureViewModel(file, null);
    StructureViewTreeElement root = model.getRoot();
    
    TreeElement[] topLevel = root.getChildren();
    assertEquals(1, topLevel.length);
    assertEquals("module Outer", topLevel[0].getPresentation().getPresentableText());
    assertEquals(AllIcons.Nodes.Package, topLevel[0].getPresentation().getIcon(false));
    
    TreeElement[] outerChildren = topLevel[0].getChildren();
    assertEquals(2, outerChildren.length);
    assertEquals("module Inner", outerChildren[0].getPresentation().getPresentableText());
    assertEquals("type LocalAlias", outerChildren[1].getPresentation().getPresentableText());
    
    TreeElement[] innerChildren = outerChildren[0].getChildren();
    assertEquals(1, innerChildren.length);
    assertEquals("circuit helper", innerChildren[0].getPresentation().getPresentableText());
    assertEquals(AllIcons.Nodes.Method, innerChildren[0].getPresentation().getIcon(false));
  }

  public void testModelLeafAndPlusBehavior() {
    String code = """
        struct S { a: Field }
        circuit c(): Void {}
        """;
    PsiFile file = myFixture.configureByText(CompactFileType.INSTANCE, code);
    CompactStructureViewModel model = new CompactStructureViewModel(file, null);
    
    assertTrue(model.isAlwaysShowsPlus(model.getRoot()));
    
    StructureViewTreeElement[] children = Arrays.stream(model.getRoot().getChildren())
        .map(c -> (StructureViewTreeElement) c)
        .toArray(StructureViewTreeElement[]::new);
        
    for (StructureViewTreeElement child : children) {
      if (Objects.requireNonNull(child.getPresentation().getPresentableText()).startsWith("circuit")) {
        assertTrue("Circuit should be leaf", model.isAlwaysLeaf(child));
      }
    }
  }

  public void testMalformedCodeResilience() {
    String code = """
        contract {
          circuit (
          struct {
        """;
    PsiFile file = myFixture.configureByText(CompactFileType.INSTANCE, code);
    CompactStructureViewModel model = new CompactStructureViewModel(file, null);
    
    StructureViewTreeElement root = model.getRoot();
    assertNotNull(root);
    TreeElement[] children = root.getChildren();
    assertNotNull(children);
  }
}
