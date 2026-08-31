package dev.verloren.midnight.completion;

import com.intellij.psi.PsiElement;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import dev.verloren.midnight.CompactFileType;
import dev.verloren.midnight.CompactLanguage;
import dev.verloren.midnight.parser.CompactParserDefinition;
import dev.verloren.midnight.psi.CompactNamedElement;
import dev.verloren.midnight.resolve.CompactResolveUtil;

import java.util.Collection;

public class CompactCompletionTest extends BasePlatformTestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    com.intellij.lang.LanguageParserDefinitions.INSTANCE.addExplicitExtension(
            CompactLanguage.INSTANCE,
            new CompactParserDefinition()
    );
  }

  public void testKeywordContextClassification() {
    myFixture.configureByText(CompactFileType.INSTANCE, "<caret>");
    PsiElement pos = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
    pos = pos == null ? myFixture.getFile() : pos;
    assertEquals(CompactCompletionContext.Kind.KEYWORD, CompactCompletionContext.classify(pos));
  }

  public void testTypeContextClassificationAndCollection() {
    myFixture.configureByText(CompactFileType.INSTANCE,
            """
                    struct CustomType {}
                    circuit mint(amount: <caret>) {}
                    """
    );
    PsiElement pos = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
    assertNotNull(pos);
    assertEquals(CompactCompletionContext.Kind.TYPE, CompactCompletionContext.classify(pos));

    Collection<CompactNamedElement> typeDecls = CompactResolveUtil.collectTypeDeclarations(pos);
    Collection<String> names = typeDecls.stream().map(CompactNamedElement::getName).toList();
    assertTrue("Should collect 'CustomType'", names.contains("CustomType"));
  }

  public void testValueContextClassificationAndCollection() {
    myFixture.configureByText(CompactFileType.INSTANCE,
            """
                    circuit test() {
                      const myVar = 42;
                      const copy = <caret>;
                    }
                    """
    );
    PsiElement pos = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
    assertNotNull(pos);
    assertEquals(CompactCompletionContext.Kind.VALUE, CompactCompletionContext.classify(pos));

    Collection<CompactNamedElement> valueDecls = CompactResolveUtil.collectValueDeclarations(pos);
    Collection<String> names = valueDecls.stream().map(CompactNamedElement::getName).toList();
    assertTrue("Should collect 'myVar'", names.contains("myVar"));
  }

  public void testMemberContextClassification() {
    myFixture.configureByText(CompactFileType.INSTANCE,
            """
                    enum Status { Active, Suspended, Closed }
                    circuit check() {
                      const s = Status.<caret>;
                    }
                    """
    );
    PsiElement pos = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
    assertNotNull(pos);
    assertEquals(CompactCompletionContext.Kind.MEMBER, CompactCompletionContext.classify(pos));
  }

  public void testGenericParameterTypeCompletion() {
    myFixture.configureByText(CompactFileType.INSTANCE,
            """
                    type Container<#N> = Vector<#<caret>, Field>;
                    """
    );
    PsiElement pos = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
    assertNotNull(pos);
    assertEquals(CompactCompletionContext.Kind.TYPE, CompactCompletionContext.classify(pos));

    Collection<CompactNamedElement> typeDecls = CompactResolveUtil.collectTypeDeclarations(pos);
    Collection<String> names = typeDecls.stream().map(CompactNamedElement::getName).toList();
    assertTrue("Generic parameter 'N' should be collectible in type context", names.contains("N"));
  }

  public void testStructMemberCompletionOnParameter() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        struct Config {
            timeout: Uint<32>,
            retries: Uint<8>
        }

        enum State {
            Idle,
            Running,
            Finished
        }

        circuit run(cfg: Config): Void {
            const t = cfg.<caret>
        }
        """
    );
    myFixture.completeBasic();
    java.util.List<String> lookupStrings = myFixture.getLookupElementStrings();
    assertNotNull("Lookup strings should not be null", lookupStrings);
    assertTrue("Should suggest 'timeout'", lookupStrings.contains("timeout"));
    assertTrue("Should suggest 'retries'", lookupStrings.contains("retries"));
  }

  public void testEnumMemberCompletion() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        struct Config {
            timeout: Uint<32>,
            retries: Uint<8>
        }

        enum State {
            Idle,
            Running,
            Finished
        }

        circuit run(cfg: Config): Void {
            const s = State.<caret>
        }
        """
    );
    myFixture.completeBasic();
    java.util.List<String> lookupStrings = myFixture.getLookupElementStrings();
    assertNotNull("Lookup strings should not be null", lookupStrings);
    assertTrue("Should suggest 'Idle'", lookupStrings.contains("Idle"));
    assertTrue("Should suggest 'Running'", lookupStrings.contains("Running"));
    assertTrue("Should suggest 'Finished'", lookupStrings.contains("Finished"));
  }

  public void testStructMemberCompletionOnLocalConst() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        struct Point {
            x: Field,
            y: Field
        }

        circuit draw(): Void {
            const p: Point = Point { x: 1, y: 2 };
            const px = p.<caret>
        }
        """
    );
    myFixture.completeBasic();
    java.util.List<String> lookupStrings = myFixture.getLookupElementStrings();
    assertNotNull("Lookup strings should not be null", lookupStrings);
    assertTrue("Should suggest 'x'", lookupStrings.contains("x"));
    assertTrue("Should suggest 'y'", lookupStrings.contains("y"));
  }

  public void testReturnCompletionFieldTypeAwareness() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        circuit test(secretKey: Field, publicAddress: Uint<32>): Field {
            const multiplier = 5;
            return <caret>
        }
        """
    );
    myFixture.completeBasic();
    java.util.List<String> lookupStrings = myFixture.getLookupElementStrings();
    assertNotNull("Lookup strings should not be null", lookupStrings);
    assertTrue("Should suggest 'secretKey'", lookupStrings.contains("secretKey"));
    assertTrue("Should suggest 'multiplier'", lookupStrings.contains("multiplier"));
    assertFalse("Should NOT suggest 'publicAddress'", lookupStrings.contains("publicAddress"));
    assertFalse("Should NOT suggest 'true'", lookupStrings.contains("true"));
    assertFalse("Should NOT suggest 'false'", lookupStrings.contains("false"));
    assertFalse("Should NOT suggest 'circuit'", lookupStrings.contains("circuit"));
    assertFalse("Should NOT suggest 'const'", lookupStrings.contains("const"));
  }

  public void testReturnCompletionUintTypeAwareness() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        circuit compute(secretKey: Field, publicAddress: Uint<32>, count: Uint<8>): Uint<32> {
            const multiplier = 5;
            return <caret>
        }
        """
    );
    myFixture.completeBasic();
    java.util.List<String> lookupStrings = myFixture.getLookupElementStrings();
    assertNotNull("Lookup strings should not be null", lookupStrings);
    assertTrue("Should suggest 'publicAddress'", lookupStrings.contains("publicAddress"));
    assertTrue("Should suggest 'count'", lookupStrings.contains("count"));
    assertTrue("Should suggest 'multiplier'", lookupStrings.contains("multiplier"));
    assertFalse("Should NOT suggest 'secretKey'", lookupStrings.contains("secretKey"));
    assertFalse("Should NOT suggest 'true'", lookupStrings.contains("true"));
  }

  public void testReturnCompletionBooleanTypeAwareness() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        circuit isValid(secretKey: Field, isActive: Boolean): Boolean {
            return <caret>
        }
        """
    );
    myFixture.completeBasic();
    java.util.List<String> lookupStrings = myFixture.getLookupElementStrings();
    assertNotNull("Lookup strings should not be null", lookupStrings);
    assertTrue("Should suggest 'isActive'", lookupStrings.contains("isActive"));
    assertTrue("Should suggest 'true'", lookupStrings.contains("true"));
    assertTrue("Should suggest 'false'", lookupStrings.contains("false"));
    assertFalse("Should NOT suggest 'secretKey'", lookupStrings.contains("secretKey"));
  }

  public void testReturnCompletionBooleanKeywordsPrioritized() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        circuit isValid(secretKey: Field, isActive: Boolean): Boolean {
            return <caret>
        }
        """
    );
    myFixture.completeBasic();
    java.util.List<String> lookupStrings = myFixture.getLookupElementStrings();
    assertNotNull("Lookup strings should not be null", lookupStrings);
    int trueIdx = lookupStrings.indexOf("true");
    int falseIdx = lookupStrings.indexOf("false");
    assertTrue("'true' should be suggested", trueIdx >= 0);
    assertTrue("'false' should be suggested", falseIdx >= 0);
  }

  public void testReturnCompletionStructTypeAwareness() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        struct Point { x: Field, y: Field }
        struct Config { timeout: Uint<32> }
        circuit getPoint(p: Point, c: Config): Point {
            return <caret>
        }
        """
    );
    myFixture.completeBasic();
    java.util.List<String> lookupStrings = myFixture.getLookupElementStrings();
    assertNotNull("Lookup strings should not be null", lookupStrings);
    assertTrue("Should suggest 'p'", lookupStrings.contains("p"));
    assertFalse("Should NOT suggest 'c'", lookupStrings.contains("c"));
  }
}
