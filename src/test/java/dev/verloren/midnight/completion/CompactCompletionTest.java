package dev.verloren.midnight.completion;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.PsiElement;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import dev.verloren.midnight.CompactFileType;
import dev.verloren.midnight.CompactLanguage;
import dev.verloren.midnight.parser.CompactParserDefinition;
import dev.verloren.midnight.psi.CompactNamedElement;
import dev.verloren.midnight.resolve.CompactResolveUtil;

import java.util.Collection;
import java.util.List;

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
    List<String> lookupStrings = myFixture.getLookupElementStrings();
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
    List<String> lookupStrings = myFixture.getLookupElementStrings();
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
    List<String> lookupStrings = myFixture.getLookupElementStrings();
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
    List<String> lookupStrings = myFixture.getLookupElementStrings();
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
    List<String> lookupStrings = myFixture.getLookupElementStrings();
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
    List<String> lookupStrings = myFixture.getLookupElementStrings();
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
    List<String> lookupStrings = myFixture.getLookupElementStrings();
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
    List<String> lookupStrings = myFixture.getLookupElementStrings();
    assertNotNull("Lookup strings should not be null", lookupStrings);
    assertTrue("Should suggest 'p'", lookupStrings.contains("p"));
    assertFalse("Should NOT suggest 'c'", lookupStrings.contains("c"));
  }

  public void testStatementContextClassification() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        circuit test() {
          <caret>
        }
        """
    );
    PsiElement pos = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
    assertNotNull(pos);
    assertEquals(CompactCompletionContext.Kind.STATEMENT, CompactCompletionContext.classify(pos));
  }

  public void testStatementCompletionSeparation() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        circuit test(amount: Uint<64>) {
          <caret>
        }
        """
    );
    myFixture.completeBasic();
    List<String> lookupStrings = myFixture.getLookupElementStrings();
    assertNotNull("Lookup strings should not be null", lookupStrings);
    assertTrue("Should suggest statement keyword 'const'", lookupStrings.contains("const"));
    assertTrue("Should suggest statement keyword 'return'", lookupStrings.contains("return"));
    assertTrue("Should suggest statement keyword 'if'", lookupStrings.contains("if"));
    assertTrue("Should suggest in-scope parameter 'amount'", lookupStrings.contains("amount"));
    assertFalse("Should NOT suggest top-level keyword 'import'", lookupStrings.contains("import"));
    assertFalse("Should NOT suggest top-level keyword 'pragma'", lookupStrings.contains("pragma"));
    assertFalse("Should NOT suggest top-level keyword 'circuit'", lookupStrings.contains("circuit"));
    assertFalse("Should NOT suggest top-level keyword 'ledger'", lookupStrings.contains("ledger"));
  }

  public void testTopLevelCompletionSeparation() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        pragma language_version >= 0.26.0;
        <caret>
        """
    );
    myFixture.completeBasic();
    List<String> lookupStrings = myFixture.getLookupElementStrings();
    assertNotNull("Lookup strings should not be null", lookupStrings);
    assertTrue("Should suggest declaration keyword 'import'", lookupStrings.contains("import"));
    assertTrue("Should suggest declaration keyword 'circuit'", lookupStrings.contains("circuit"));
    assertTrue("Should suggest declaration keyword 'struct'", lookupStrings.contains("struct"));
    assertTrue("Should suggest 'export ledger'", lookupStrings.contains("export ledger"));
    assertTrue("Should suggest 'ledger'", lookupStrings.contains("ledger"));
    assertFalse("Should NOT suggest statement keyword 'return'", lookupStrings.contains("return"));
    assertFalse("Should NOT suggest statement keyword 'for'", lookupStrings.contains("for"));
  }

  public void testAfterExportContextClassification() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        export <caret>
        """
    );
    PsiElement pos = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
    assertNotNull(pos);
    assertEquals(CompactCompletionContext.Kind.AFTER_EXPORT, CompactCompletionContext.classify(pos));
  }

  public void testAfterSealedContextClassification() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        export sealed <caret>
        """
    );
    PsiElement pos = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
    assertNotNull(pos);
    assertEquals(CompactCompletionContext.Kind.AFTER_SEALED, CompactCompletionContext.classify(pos));
  }

  public void testAfterPureContextClassification() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        export pure <caret>
        """
    );
    PsiElement pos = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
    assertNotNull(pos);
    assertEquals(CompactCompletionContext.Kind.AFTER_PURE, CompactCompletionContext.classify(pos));
  }

  public void testAfterNewContextClassification() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        export new <caret>
        """
    );
    PsiElement pos = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
    assertNotNull(pos);
    assertEquals(CompactCompletionContext.Kind.AFTER_NEW, CompactCompletionContext.classify(pos));
  }

  public void testAfterExportSuggestsLedgerAndNotImport() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        export <caret>
        """
    );
    myFixture.completeBasic();
    List<String> lookupStrings = myFixture.getLookupElementStrings();
    assertNotNull("Lookup strings should not be null", lookupStrings);
    assertTrue("Should suggest 'ledger' after export", lookupStrings.contains("ledger"));
    assertTrue("Should suggest 'circuit' after export", lookupStrings.contains("circuit"));
    assertTrue("Should suggest 'module' after export", lookupStrings.contains("module"));
    assertFalse("Should NOT suggest 'import' after export", lookupStrings.contains("import"));
    assertFalse("Should NOT suggest 'pragma' after export", lookupStrings.contains("pragma"));
  }

  public void testAfterExportSuggestsAllExportableDeclarationsAndModifiers() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        export <caret>
        """
    );
    myFixture.completeBasic();
    List<String> lookupStrings = myFixture.getLookupElementStrings();
    assertNotNull("Lookup strings should not be null", lookupStrings);
    assertTrue("Should suggest 'ledger'", lookupStrings.contains("ledger"));
    assertTrue("Should suggest 'circuit'", lookupStrings.contains("circuit"));
    assertFalse("Should NOT suggest 'const' after export", lookupStrings.contains("const"));
    assertTrue("Should suggest 'struct'", lookupStrings.contains("struct"));
    assertTrue("Should suggest 'enum'", lookupStrings.contains("enum"));
    assertTrue("Should suggest 'type'", lookupStrings.contains("type"));
    assertTrue("Should suggest 'module'", lookupStrings.contains("module"));
    assertTrue("Should suggest 'contract'", lookupStrings.contains("contract"));
    assertTrue("Should suggest 'witness'", lookupStrings.contains("witness"));
    assertTrue("Should suggest 'pure'", lookupStrings.contains("pure"));
    assertTrue("Should suggest 'sealed'", lookupStrings.contains("sealed"));
    assertTrue("Should suggest 'new'", lookupStrings.contains("new"));
    assertTrue("Should suggest '{'", lookupStrings.contains("{"));
    assertFalse("Should NOT suggest 'import'", lookupStrings.contains("import"));
    assertFalse("Should NOT suggest 'pragma'", lookupStrings.contains("pragma"));
  }

  public void testAfterExportDoesNotSuggestExportPrefixedVariants() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        export <caret>
        """
    );
    myFixture.completeBasic();
    List<String> lookupStrings = myFixture.getLookupElementStrings();
    assertNotNull("Lookup strings should not be null", lookupStrings);
    assertTrue("Should suggest bare 'witness'", lookupStrings.contains("witness"));
    assertTrue("Should suggest bare 'circuit'", lookupStrings.contains("circuit"));
    assertTrue("Should suggest bare 'ledger'", lookupStrings.contains("ledger"));
    assertFalse("Should NOT suggest 'export witness' after export", lookupStrings.contains("export witness"));
    assertFalse("Should NOT suggest 'export circuit' after export", lookupStrings.contains("export circuit"));
    assertFalse("Should NOT suggest 'export ledger' after export", lookupStrings.contains("export ledger"));
    assertFalse("Should NOT suggest 'export struct' after export", lookupStrings.contains("export struct"));
  }

  public void testAfterSealedSuggestsLedger() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        export sealed <caret>
        """
    );
    myFixture.completeBasic();
    List<String> lookupStrings = myFixture.getLookupElementStrings();
    assertNotNull("Lookup strings should not be null", lookupStrings);
    assertTrue("Should suggest 'ledger' after sealed", lookupStrings.contains("ledger"));
    assertFalse("Should NOT suggest 'circuit' after sealed", lookupStrings.contains("circuit"));
  }

  public void testAfterPureSuggestsCircuit() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        export pure <caret>
        """
    );
    myFixture.completeBasic();
    List<String> lookupStrings = myFixture.getLookupElementStrings();
    assertNotNull("Lookup strings should not be null", lookupStrings);
    assertTrue("Should suggest 'circuit' after pure", lookupStrings.contains("circuit"));
    assertFalse("Should NOT suggest 'ledger' after pure", lookupStrings.contains("ledger"));
  }

  public void testAfterNewSuggestsType() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        export new <caret>
        """
    );
    myFixture.completeBasic();
    List<String> lookupStrings = myFixture.getLookupElementStrings();
    assertNotNull("Lookup strings should not be null", lookupStrings);
    assertTrue("Should suggest 'type' after new", lookupStrings.contains("type"));
    assertFalse("Should NOT suggest 'circuit' after new", lookupStrings.contains("circuit"));
  }

  public void testTopLevelDeclarationSuggestsExportVariants() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        <caret>
        """
    );
    myFixture.completeBasic();
    List<String> lookupStrings = myFixture.getLookupElementStrings();
    assertNotNull("Lookup strings should not be null", lookupStrings);
    assertTrue("Should suggest 'export ledger'", lookupStrings.contains("export ledger"));
    assertTrue("Should suggest 'export circuit'", lookupStrings.contains("export circuit"));
    assertFalse("Should NOT suggest 'export const'", lookupStrings.contains("export const"));
    assertTrue("Should suggest 'export struct'", lookupStrings.contains("export struct"));
    assertTrue("Should suggest 'export enum'", lookupStrings.contains("export enum"));
    assertTrue("Should suggest 'export type'", lookupStrings.contains("export type"));
    assertTrue("Should suggest 'export module'", lookupStrings.contains("export module"));
    assertTrue("Should suggest 'export contract'", lookupStrings.contains("export contract"));
    assertTrue("Should suggest 'export witness'", lookupStrings.contains("export witness"));
  }

  public void testExportCircuitCompletionInsertion() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        export cir<caret>
        """
    );
    myFixture.completeBasic();
    myFixture.type('\n');
    String text = myFixture.getFile().getText();
    assertTrue("File should contain 'export circuit circuit1(): Void {' but was:\n" + text,
        text.contains("export circuit circuit1(): Void {"));
  }

  public void testExportConstNotSuggestedAtTopLevel() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        export con<caret>
        """
    );
    myFixture.completeBasic();
    List<String> lookupStrings = myFixture.getLookupElementStrings();
    if (lookupStrings != null) {
      assertFalse("Should NOT suggest 'const' after export keyword", lookupStrings.contains("const"));
    }
  }

  public void testExportStructCompletionInsertion() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        export str<caret>
        """
    );
    myFixture.completeBasic();
    myFixture.type('\n');
    String text = myFixture.getFile().getText();
    assertTrue("File should contain 'export struct struct1 {' but was:\n" + text,
        text.contains("export struct struct1 {"));
  }

  public void testExportEnumCompletionInsertion() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        export en<caret>
        """
    );
    myFixture.completeBasic();
    myFixture.type('\n');
    String text = myFixture.getFile().getText();
    assertTrue("File should contain 'export enum enum1 {' but was:\n" + text,
        text.contains("export enum enum1 {"));
  }

  public void testExportTypeCompletionInsertion() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        export ty<caret>
        """
    );
    myFixture.completeBasic();
    myFixture.type('\n');
    String text = myFixture.getFile().getText();
    assertTrue("File should contain 'export type type1 = Field;' but was:\n" + text,
        text.contains("export type type1 = Field;"));
  }

  public void testExportWitnessCompletionInsertion() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        export wit<caret>
        """
    );
    myFixture.completeBasic();
    myFixture.type('\n');
    String text = myFixture.getFile().getText();
    assertTrue("File should contain 'export witness witness1(): Field;' but was:\n" + text,
        text.contains("export witness witness1(): Field;"));
  }

  public void testExportLedgerCompletionInsertion() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        export led<caret>
        """
    );
    myFixture.completeBasic();
    myFixture.type('\n');
    String text = myFixture.getFile().getText();
    assertTrue("File should contain 'export ledger ledger1: State;' but was:\n" + text,
        text.contains("export ledger ledger1: State;"));
  }

  public void testLedgerCompletionAtTopLevelInsertion() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        led<caret>
        """
    );
    myFixture.completeBasic();
    // Select the "export ledger" lookup item
    LookupElement[] elements = myFixture.getLookupElements();
    assertNotNull(elements);
    LookupElement target = null;
    for (LookupElement el : elements) {
      if ("export ledger".equals(el.getLookupString())) {
        target = el;
        break;
      }
    }
    assertNotNull("Should find 'export ledger' lookup item", target);
    myFixture.getLookup().setCurrentItem(target);
    myFixture.type('\n');
    String text = myFixture.getFile().getText();
    assertTrue("File should contain 'export ledger ledger1: State;' but was:\n" + text,
        text.contains("export ledger ledger1: State;"));
  }

  public void testSequentialLedgerCompletionInsertion() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        export ledger ledger1: State;
        export led<caret>
        """
    );
    myFixture.completeBasic();
    myFixture.type('\n');
    String text = myFixture.getFile().getText();
    assertTrue("File should contain 'export ledger ledger2: State;' but was:\n" + text,
        text.contains("export ledger ledger2: State;"));
  }

  public void testStructBodyCompletionDoesNotSuggestKeywords() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        struct Config {
          <caret>
        }
        """
    );
    myFixture.completeBasic();
    List<String> lookupStrings = myFixture.getLookupElementStrings();
    if (lookupStrings != null) {
      assertFalse("Should NOT suggest 'import' inside struct body", lookupStrings.contains("import"));
      assertFalse("Should NOT suggest 'return' inside struct body", lookupStrings.contains("return"));
      assertFalse("Should NOT suggest 'circuit' inside struct body", lookupStrings.contains("circuit"));
    }
  }

  public void testTopLevelWitnessTypingSuggestsBothWithAndWithoutExport() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        wit<caret>
        """
    );
    myFixture.completeBasic();
    List<String> lookupStrings = myFixture.getLookupElementStrings();
    assertNotNull("Lookup strings should not be null", lookupStrings);
    assertTrue("Should suggest 'witness' when typing 'wit'", lookupStrings.contains("witness"));
    assertTrue("Should suggest 'export witness' when typing 'wit'", lookupStrings.contains("export witness"));
  }

  public void testTopLevelCircuitTypingSuggestsBothWithAndWithoutExport() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        cir<caret>
        """
    );
    myFixture.completeBasic();
    List<String> lookupStrings = myFixture.getLookupElementStrings();
    assertNotNull("Lookup strings should not be null", lookupStrings);
    assertTrue("Should suggest 'circuit' when typing 'cir'", lookupStrings.contains("circuit"));
    assertTrue("Should suggest 'export circuit' when typing 'cir'", lookupStrings.contains("export circuit"));
  }

  public void testTopLevelStructTypingSuggestsBothWithAndWithoutExport() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        str<caret>
        """
    );
    myFixture.completeBasic();
    List<String> lookupStrings = myFixture.getLookupElementStrings();
    assertNotNull("Lookup strings should not be null", lookupStrings);
    assertTrue("Should suggest 'struct' when typing 'str'", lookupStrings.contains("struct"));
    assertTrue("Should suggest 'export struct' when typing 'str'", lookupStrings.contains("export struct"));
  }

  public void testTopLevelEnumTypingSuggestsBothWithAndWithoutExport() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        en<caret>
        """
    );
    myFixture.completeBasic();
    List<String> lookupStrings = myFixture.getLookupElementStrings();
    assertNotNull("Lookup strings should not be null", lookupStrings);
    assertTrue("Should suggest 'enum' when typing 'en'", lookupStrings.contains("enum"));
    assertTrue("Should suggest 'export enum' when typing 'en'", lookupStrings.contains("export enum"));
  }

  public void testTopLevelLedgerTypingSuggestsBothWithAndWithoutExport() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        led<caret>
        """
    );
    myFixture.completeBasic();
    List<String> lookupStrings = myFixture.getLookupElementStrings();
    assertNotNull("Lookup strings should not be null", lookupStrings);
    assertTrue("Should suggest 'ledger' when typing 'led'", lookupStrings.contains("ledger"));
    assertTrue("Should suggest 'export ledger' when typing 'led'", lookupStrings.contains("export ledger"));
  }

  public void testTopLevelContractTypingSuggestsBothWithAndWithoutExport() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        cct<caret>
        """
    );
    myFixture.completeBasic();
    List<String> lookupStrings = myFixture.getLookupElementStrings();
    assertNotNull("Lookup strings should not be null", lookupStrings);
    assertTrue("Should suggest 'contract' when typing 'cct'", lookupStrings.contains("contract"));
    assertTrue("Should suggest 'export contract' when typing 'cct'", lookupStrings.contains("export contract"));
  }

  public void testTopLevelModuleTypingSuggestsBothWithAndWithoutExport() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        mod<caret>
        """
    );
    myFixture.completeBasic();
    List<String> lookupStrings = myFixture.getLookupElementStrings();
    assertNotNull("Lookup strings should not be null", lookupStrings);
    assertTrue("Should suggest 'module' when typing 'mod'", lookupStrings.contains("module"));
    assertTrue("Should suggest 'export module' when typing 'mod'", lookupStrings.contains("export module"));
  }

  public void testTopLevelTypeSuggestsBothWithAndWithoutExport() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        type<caret>
        """
    );
    myFixture.completeBasic();
    List<String> lookupStrings = myFixture.getLookupElementStrings();
    assertNotNull("Lookup strings should not be null", lookupStrings);
    assertTrue("Should suggest 'type' when typing 'type'", lookupStrings.contains("type"));
    assertTrue("Should suggest 'export type' when typing 'type'", lookupStrings.contains("export type"));
  }

  public void testAfterExportTypingWitSuggestsOnlyBareWitnessAndNoDuplicateExport() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        export wit<caret>
        """
    );
    myFixture.completeBasic();
    // Since bare 'witness' is the only matching item after 'export wit', IntelliJ auto-completes it immediately
    String text = myFixture.getFile().getText();
    assertFalse("Must not contain duplicate 'export export'", text.contains("export export"));
    assertTrue("File should contain 'export witness witness1(): Field;' but was:\n" + text,
        text.contains("export witness witness1(): Field;"));
  }

  public void testAfterExportTypingCirSuggestsOnlyBareCircuitAndNoDuplicateExport() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        export cir<caret>
        """
    );
    myFixture.completeBasic();
    // Since bare 'circuit' is the only matching item after 'export cir', IntelliJ auto-completes it immediately
    String text = myFixture.getFile().getText();
    assertFalse("Must not contain duplicate 'export export'", text.contains("export export"));
    assertTrue("File should contain 'export circuit circuit1(): Void {' but was:\n" + text,
        text.contains("export circuit circuit1(): Void {"));
  }

  public void testBareLedgerInsertionAtTopLevelDoesNotForceExport() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        led<caret>
        """
    );
    myFixture.completeBasic();
    LookupElement[] elements = myFixture.getLookupElements();
    assertNotNull(elements);
    LookupElement bareLedger = null;
    for (LookupElement el : elements) {
      if ("ledger".equals(el.getLookupString())) {
        bareLedger = el;
        break;
      }
    }
    assertNotNull("Should find bare 'ledger' lookup item", bareLedger);
    myFixture.getLookup().setCurrentItem(bareLedger);
    myFixture.type('\n');
    String text = myFixture.getFile().getText().trim();
    assertEquals("ledger ledger1: State;", text);
  }

  public void testHasPrecedingExportOnLineDetection() {
    String text = "export ";
    assertTrue(CompactCompletionContext.hasPrecedingExportOnLine(text, 0, text.length()));
    String text2 = "export witness";
    assertTrue(CompactCompletionContext.hasPrecedingExportOnLine(text2, 0, text2.length()));
    String text3 = "witness";
    assertFalse(CompactCompletionContext.hasPrecedingExportOnLine(text3, 0, text3.length()));
    String text4 = "export; witness";
    assertFalse(CompactCompletionContext.hasPrecedingExportOnLine(text4, 0, text4.length()));
    String text5 = "const x = 1; export ";
    assertTrue(CompactCompletionContext.hasPrecedingExportOnLine(text5, 0, text5.length()));
    String text6 = "{ export ";
    assertTrue(CompactCompletionContext.hasPrecedingExportOnLine(text6, 0, text6.length()));
  }
}
