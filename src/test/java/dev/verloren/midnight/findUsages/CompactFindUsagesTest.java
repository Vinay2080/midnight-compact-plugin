package dev.verloren.midnight.findUsages;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.usageView.UsageInfo;
import dev.verloren.midnight.CompactFileType;
import dev.verloren.midnight.CompactLanguage;
import dev.verloren.midnight.parser.CompactParserDefinition;
import dev.verloren.midnight.psi.CompactImportElementImpl;
import dev.verloren.midnight.psi.CompactMemberExprImpl;
import dev.verloren.midnight.psi.CompactNamedElement;

import java.util.Collection;

public class CompactFindUsagesTest extends BasePlatformTestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    com.intellij.lang.LanguageParserDefinitions.INSTANCE.addExplicitExtension(
            CompactLanguage.INSTANCE,
            new CompactParserDefinition()
    );
    com.intellij.lang.findUsages.LanguageFindUsages.INSTANCE.addExplicitExtension(
            CompactLanguage.INSTANCE,
            new CompactFindUsagesProvider()
    );
  }

  private Collection<UsageInfo> findUsagesAtCaret() {
    PsiElement element = myFixture.getElementAtCaret();
    CompactNamedElement namedElement = PsiTreeUtil.getParentOfType(element, CompactNamedElement.class, false);
    assertNotNull("Caret element parent should be CompactNamedElement", namedElement);
    return myFixture.findUsages(namedElement);
  }

  public void testFindUsagesForCircuit() {
    myFixture.configureByText(CompactFileType.INSTANCE,
            """
                    circuit m<caret>int() {}
                    circuit caller() {
                      mint();
                      mint();
                    }
                    """
    );
    PsiElement element = myFixture.getElementAtCaret();
    CompactNamedElement namedElement = PsiTreeUtil.getParentOfType(element, CompactNamedElement.class, false);
    assertNotNull(namedElement);
    Collection<UsageInfo> usages = myFixture.findUsages(namedElement);
    assertEquals(2, usages.size());
  }

  public void testFindUsagesForParameter() {
    myFixture.configureByText(CompactFileType.INSTANCE,
            """
                    circuit mint(a<caret>mount: Uint<32>) {
                      const x = amount;
                      const y = amount;
                    }
                    """
    );
    PsiElement element = myFixture.getElementAtCaret();
    CompactNamedElement namedElement = PsiTreeUtil.getParentOfType(element, CompactNamedElement.class, false);
    assertNotNull(namedElement);
    Collection<UsageInfo> usages = myFixture.findUsages(namedElement);
    assertEquals(2, usages.size());
  }

  public void testFindUsagesForStruct() {
    myFixture.configureByText(CompactFileType.INSTANCE,
            """
                    struct P<caret>oint { x: Field }
                    circuit draw(p: Point) {
                      const p2 = Point { x: 0 };
                    }
                    """
    );
    PsiElement element = myFixture.getElementAtCaret();
    CompactNamedElement namedElement = PsiTreeUtil.getParentOfType(element, CompactNamedElement.class, false);
    assertNotNull(namedElement);
    Collection<UsageInfo> usages = myFixture.findUsages(namedElement);
    assertEquals(2, usages.size());
  }

  public void testFindUsagesIgnoresCommentsAndStrings() {
    myFixture.configureByText(CompactFileType.INSTANCE,
            """
                    // mint is a great circuit
                    const str = "mint";
                    circuit m<caret>int() {}
                    circuit caller() {
                      mint();
                    }
                    """
    );
    PsiElement element = myFixture.getElementAtCaret();
    CompactNamedElement namedElement = PsiTreeUtil.getParentOfType(element, CompactNamedElement.class, false);
    assertNotNull(namedElement);
    Collection<UsageInfo> usages = myFixture.findUsages(namedElement);
    assertEquals("Should find exactly 1 code usage reference, ignoring comment and string literal", 1, usages.size());
  }

  public void testFindUsagesProviderElementTypes() {
    CompactFindUsagesProvider provider = new CompactFindUsagesProvider();

    myFixture.configureByText(CompactFileType.INSTANCE,
            "struct <caret>Point { x: Field }\n"
    );
    PsiElement element = myFixture.getElementAtCaret();
    assertTrue(provider.canFindUsagesFor(element));
    assertEquals("Point", provider.getDescriptiveName(element));
    assertEquals("struct", provider.getType(element));
  }

  public void testFindUsagesForEnumMember() {
    myFixture.configureByText(CompactFileType.INSTANCE,
            """
                    enum Color { R<caret>ed, Green }
                    circuit check() {
                      const c = Color.Red;
                    }
                    """
    );
    PsiElement element = myFixture.getElementAtCaret();
    CompactNamedElement namedElement = PsiTreeUtil.getParentOfType(element, CompactNamedElement.class, false);
    assertNotNull(namedElement);
    Collection<UsageInfo> usages = myFixture.findUsages(namedElement);
    assertEquals(1, usages.size());
  }

  public void testFindUsagesForStructField() {
    myFixture.configureByText(CompactFileType.INSTANCE,
            """
                    struct Point { <caret>x: Field }
                    circuit draw(p: Point) {
                      const val = p.x;
                    }
                    """
    );
    PsiElement element = myFixture.getElementAtCaret();
    dev.verloren.midnight.psi.CompactStructFieldImpl structField = PsiTreeUtil.getParentOfType(element, dev.verloren.midnight.psi.CompactStructFieldImpl.class, false);
    assertNotNull(structField);
    Collection<UsageInfo> usages = myFixture.findUsages(structField);
    assertEquals(1, usages.size());
  }

  public void testUseScopeDifferentiation() {
    myFixture.configureByText(CompactFileType.INSTANCE,
            """
                    circuit <caret>mint(amount: Uint<32>) {
                      const local_var = amount;
                    }
                    """
    );
    PsiElement element = myFixture.getElementAtCaret();
    CompactNamedElement circuit = PsiTreeUtil.getParentOfType(element, CompactNamedElement.class, false);
    assertNotNull(circuit);
    assertTrue("Top-level circuit should have project useScope",
            circuit.getUseScope() instanceof com.intellij.psi.search.GlobalSearchScope);

    CompactNamedElement param = PsiTreeUtil.findChildOfType(circuit, dev.verloren.midnight.psi.CompactPatternImpl.class);
    assertNotNull(param);
    assertEquals("amount", param.getName());
    assertTrue("Parameter should have LocalSearchScope",
            param.getUseScope() instanceof com.intellij.psi.search.LocalSearchScope);
  }

  public void testFindUsagesCrossFile() {
    myFixture.addFileToProject("included.compact",
            """
                    circuit helper(): Void {}
                    """
    );
    myFixture.configureByText(CompactFileType.INSTANCE,
            """
                    include "included.compact";
                    circuit main(): Void {
                      <caret>helper();
                      helper();
                    }
                    """
    );
    PsiReference ref = myFixture.getReferenceAtCaretPosition();
    assertNotNull(ref);
    PsiElement target = ref.resolve();
    assertNotNull(target);
    assertTrue(target instanceof dev.verloren.midnight.psi.CompactCircuitDefinition);
    CompactNamedElement helperDecl = (CompactNamedElement) target;
    assertEquals("helper", helperDecl.getName());

    Collection<UsageInfo> usages = myFixture.findUsages(helperDecl);
    assertEquals(2, usages.size());
  }

  public void testFindUsagesCrossFileEnumAndMember() {
    myFixture.addFileToProject("GameState.compact",
            """
                    export enum GameState {
                      WAITING,
                      PLAYING,
                      FINISHED,
                    }
                    """
    );
    myFixture.configureByText(CompactFileType.INSTANCE,
            """
                    import { GameState } from './GameState';
                    export circuit checkGame(): [] {
                      assert(GameState.PLAYING == GameState.PLAYING, "Game is playing");
                    }
                    """
    );

    // 1. Find usages for imported enum GameState
    PsiFile gameFile = myFixture.getFile();
    CompactImportElementImpl importElement = PsiTreeUtil.findChildOfType(gameFile, CompactImportElementImpl.class);
    assertNotNull(importElement);
    assertNotNull(importElement.getReference());
    PsiElement targetEnum = importElement.getReference().resolve();
    assertNotNull(targetEnum);
    assertTrue(targetEnum instanceof CompactNamedElement);
    Collection<UsageInfo> enumUsages = myFixture.findUsages( targetEnum);
    // Usages: import { GameState } and two GameState qualifiers in PLAYING access
    assertFalse("Should find at least 1 usage of GameState across files", enumUsages.isEmpty());

    // 2. Find usages for imported enum member PLAYING
    CompactMemberExprImpl memberExpr = PsiTreeUtil.findChildOfType(gameFile, CompactMemberExprImpl.class);
    assertNotNull(memberExpr);
    assertNotNull(memberExpr.getReference());
    PsiElement targetMember = memberExpr.getReference().resolve();
    assertNotNull(targetMember);
    assertTrue(targetMember instanceof CompactNamedElement);
    assertEquals("PLAYING", ((CompactNamedElement) targetMember).getName());
    Collection<UsageInfo> memberUsages = myFixture.findUsages(targetMember);
    assertEquals("Should find 2 usages of PLAYING across files", 2, memberUsages.size());
  }
}
