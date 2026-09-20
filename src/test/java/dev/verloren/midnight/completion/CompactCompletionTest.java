package dev.verloren.midnight.completion;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.template.Expression;
import com.intellij.codeInsight.template.ExpressionContext;
import com.intellij.codeInsight.template.Result;
import com.intellij.codeInsight.template.TextResult;
import com.intellij.codeInsight.template.impl.ConstantNode;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiElement;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import dev.verloren.midnight.CompactFileType;
import dev.verloren.midnight.CompactLanguage;
import dev.verloren.midnight.ide.templates.CompactTypeExpression;
import dev.verloren.midnight.ide.templates.CompactTypeMacro;
import dev.verloren.midnight.parser.CompactParserDefinition;
import dev.verloren.midnight.psi.CompactNamedElement;
import dev.verloren.midnight.resolve.CompactResolveUtil;

import java.util.Arrays;
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
    assertTrue("Should suggest in-scope parameter 'publicAddress' even if different type", lookupStrings.contains("publicAddress"));
    assertTrue("Type-matching 'secretKey' should be prioritized before 'publicAddress'", lookupStrings.indexOf("secretKey") < lookupStrings.indexOf("publicAddress"));
    assertTrue("Type-matching 'multiplier' should be prioritized before 'publicAddress'", lookupStrings.indexOf("multiplier") < lookupStrings.indexOf("publicAddress"));
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
    assertTrue("Should suggest in-scope parameter 'secretKey' even if different type", lookupStrings.contains("secretKey"));
    assertTrue("Type-matching 'publicAddress' should be prioritized before 'secretKey'", lookupStrings.indexOf("publicAddress") < lookupStrings.indexOf("secretKey"));
    assertTrue("Type-matching 'count' should be prioritized before 'secretKey'", lookupStrings.indexOf("count") < lookupStrings.indexOf("secretKey"));
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
    assertTrue("Should suggest in-scope parameter 'secretKey' even if different type", lookupStrings.contains("secretKey"));
    assertTrue("Type-matching 'isActive' should be prioritized before 'secretKey'", lookupStrings.indexOf("isActive") < lookupStrings.indexOf("secretKey"));
    assertTrue("Type-matching 'true' should be prioritized before 'secretKey'", lookupStrings.indexOf("true") < lookupStrings.indexOf("secretKey"));
    assertTrue("Type-matching 'false' should be prioritized before 'secretKey'", lookupStrings.indexOf("false") < lookupStrings.indexOf("secretKey"));
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
    assertTrue("Should suggest in-scope parameter 'c' even if different type", lookupStrings.contains("c"));
    assertTrue("Type-matching 'p' should be prioritized before 'c'", lookupStrings.indexOf("p") < lookupStrings.indexOf("c"));
  }

  public void testReturnValueCompletionSuggestsParameterInReturnUnaryExpr() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        export pure circuit isContractAddress(keyOrAddress: Either<ZswapCoinPublicKey, ContractAddress>): Boolean {
            return !<caret>
        }
        """
    );
    myFixture.completeBasic();
    List<String> lookupStrings = myFixture.getLookupElementStrings();
    assertNotNull("Lookup strings should not be null", lookupStrings);
    assertTrue("Should suggest parameter 'keyOrAddress' in return expression",
        lookupStrings.contains("keyOrAddress"));
  }

  public void testReturnValueCompletionSuggestsParameterInDirectReturn() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        export pure circuit isContractAddress(keyOrAddress: Either<ZswapCoinPublicKey, ContractAddress>): Boolean {
            return <caret>
        }
        """
    );
    myFixture.completeBasic();
    List<String> lookupStrings = myFixture.getLookupElementStrings();
    assertNotNull("Lookup strings should not be null", lookupStrings);
    assertTrue("Should suggest parameter 'keyOrAddress' in return expression",
        lookupStrings.contains("keyOrAddress"));
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

  public void testNoCompletionInsideLineComment() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        // led<caret>
        """
    );
    myFixture.completeBasic();
    List<String> lookupStrings = myFixture.getLookupElementStrings();
    assertTrue("Should offer no completions inside line comment",
        lookupStrings == null || lookupStrings.isEmpty());
  }

  public void testNoCompletionInsideBlockComment() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        /* led<caret> */
        """
    );
    myFixture.completeBasic();
    List<String> lookupStrings = myFixture.getLookupElementStrings();
    assertTrue("Should offer no completions inside block comment",
        lookupStrings == null || lookupStrings.isEmpty());
  }

  public void testNoCompletionInsideDocComment() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        /**
         * led<caret>
         */
        """
    );
    myFixture.completeBasic();
    List<String> lookupStrings = myFixture.getLookupElementStrings();
    assertTrue("Should offer no completions inside doc comment",
        lookupStrings == null || lookupStrings.isEmpty());
  }

  public void testCommentContextClassificationReturnsNone() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        // line comment <caret>
        """
    );
    PsiElement linePos = myFixture.getFile().findElementAt(myFixture.getCaretOffset() - 1);
    assertNotNull(linePos);
    assertEquals(CompactCompletionContext.Kind.NONE, CompactCompletionContext.classify(linePos));

    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        /* block comment <caret> */
        """
    );
    PsiElement blockPos = myFixture.getFile().findElementAt(myFixture.getCaretOffset() - 1);
    assertNotNull(blockPos);
    assertEquals(CompactCompletionContext.Kind.NONE, CompactCompletionContext.classify(blockPos));

    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        /** doc comment <caret> */
        """
    );
    PsiElement docPos = myFixture.getFile().findElementAt(myFixture.getCaretOffset() - 1);
    assertNotNull(docPos);
    assertEquals(CompactCompletionContext.Kind.NONE, CompactCompletionContext.classify(docPos));
  }

  public void testTopLevelCompletionAfterCommentWorks() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        // Header comment
        cir<caret>
        """
    );
    myFixture.completeBasic();
    List<String> lookupStrings = myFixture.getLookupElementStrings();
    assertNotNull("Lookup strings should not be null", lookupStrings);
    assertTrue("Should suggest 'circuit' after comment", lookupStrings.contains("circuit"));
    assertTrue("Should suggest 'export circuit' after comment", lookupStrings.contains("export circuit"));
  }

  public void testExportLedgerTypeCompletion() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        export ledger ledger1: <caret>;
        """
    );
    myFixture.completeBasic();
    List<String> lookupStrings = myFixture.getLookupElementStrings();
    assertNotNull("Lookup strings should not be null", lookupStrings);
    assertTrue("Should suggest 'State'", lookupStrings.contains("State"));
    assertTrue("Should suggest 'Bytes'", lookupStrings.contains("Bytes"));
    assertTrue("Should suggest 'Bytes<32>'", lookupStrings.contains("Bytes<32>"));
    assertTrue("Should suggest 'Uint'", lookupStrings.contains("Uint"));
    assertTrue("Should suggest 'Uint<64>'", lookupStrings.contains("Uint<64>"));
    assertTrue("Should suggest 'Field'", lookupStrings.contains("Field"));
    assertFalse("Should NOT suggest 'circuit'", lookupStrings.contains("circuit"));
    assertFalse("Should NOT suggest 'ledger'", lookupStrings.contains("ledger"));
  }

  public void testExportLedgerPrefixTypeCompletion() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        export ledger ledger1: St<caret>;
        """
    );
    myFixture.completeBasic();
    List<String> lookupStrings = myFixture.getLookupElementStrings();
    assertNotNull("Lookup strings should not be null", lookupStrings);
    assertTrue("Should suggest 'State'", lookupStrings.contains("State"));
    assertFalse("Should NOT suggest 'circuit'", lookupStrings.contains("circuit"));
  }

  public void testBareLedgerTypeCompletion() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        ledger myLedger: <caret>;
        """
    );
    myFixture.completeBasic();
    List<String> lookupStrings = myFixture.getLookupElementStrings();
    assertNotNull("Lookup strings should not be null", lookupStrings);
    assertTrue("Should suggest 'State'", lookupStrings.contains("State"));
    assertTrue("Should suggest 'Counter'", lookupStrings.contains("Counter"));
  }

  public void testExportCircuitReturnTypeCompletion() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        export circuit myCircuit(): <caret> {
        }
        """
    );
    myFixture.completeBasic();
    List<String> lookupStrings = myFixture.getLookupElementStrings();
    assertNotNull("Lookup strings should not be null", lookupStrings);
    assertTrue("Should suggest 'Void'", lookupStrings.contains("Void"));
    assertTrue("Should suggest 'Field'", lookupStrings.contains("Field"));
    assertTrue("Should suggest 'Boolean'", lookupStrings.contains("Boolean"));
  }

  public void testStructFieldTypeCompletion() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        struct User {
            id: <caret>
        }
        """
    );
    myFixture.completeBasic();
    List<String> lookupStrings = myFixture.getLookupElementStrings();
    assertNotNull("Lookup strings should not be null", lookupStrings);
    assertTrue("Should suggest 'Field'", lookupStrings.contains("Field"));
    assertTrue("Should suggest 'Bytes'", lookupStrings.contains("Bytes"));
  }

  public void testTypeAliasTypeCompletion() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        type MyType = <caret>;
        """
    );
    myFixture.completeBasic();
    List<String> lookupStrings = myFixture.getLookupElementStrings();
    assertNotNull("Lookup strings should not be null", lookupStrings);
    assertTrue("Should suggest 'Field'", lookupStrings.contains("Field"));
    assertTrue("Should suggest 'Bytes'", lookupStrings.contains("Bytes"));
  }

  public void testTypeExpressionLookupItems() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        struct CustomRecord {}
        export ledger ledger1: <caret>State;
        """
    );
    CompactTypeExpression expr = new CompactTypeExpression("State");
    LookupElement[] items = expr.calculateLookupItems(createExpressionContext());
    assertNotNull("Lookup items should not be null", items);
    List<String> itemNames = Arrays.stream(items).map(LookupElement::getLookupString).toList();
    assertTrue("Should include 'State'", itemNames.contains("State"));
    assertTrue("Should include 'Bytes'", itemNames.contains("Bytes"));
    assertTrue("Should include 'Bytes<32>'", itemNames.contains("Bytes<32>"));
    assertTrue("Should include 'Uint'", itemNames.contains("Uint"));
    assertTrue("Should include 'Uint<64>'", itemNames.contains("Uint<64>"));
    assertTrue("Should include 'CustomRecord'", itemNames.contains("CustomRecord"));
  }

  private ExpressionContext createExpressionContext() {
    return new ExpressionContext() {
      @Override
      public Project getProject() {
        return myFixture.getProject();
      }

      @Override
      public Editor getEditor() {
        return myFixture.getEditor();
      }

      @Override
      public int getStartOffset() {
        return myFixture.getCaretOffset();
      }

      @Override
      public int getTemplateStartOffset() {
        return 0;
      }

      @Override
      public int getTemplateEndOffset() {
        return myFixture.getFile().getTextLength();
      }

      @Override
      public <T> T getProperty(Key<T> key) {
        return null;
      }

      @Override
      public PsiElement getPsiElementAtStartOffset() {
        return myFixture.getFile().findElementAt(myFixture.getCaretOffset());
      }

      @Override
      public TextResult getVariableValue(String variableName) {
        return null;
      }
    };
  }

  public void testTypeMacroDelegatesToExpression() {
    CompactTypeMacro macro = new CompactTypeMacro();
    assertEquals("compactType", macro.getName());
    assertEquals("compactType(defaultType)", macro.getPresentableName());

    Expression[] params = new Expression[]{new ConstantNode("State")};
    Result result = macro.calculateResult(params, null);
    assertNotNull(result);
    assertEquals("State", result.toString());
  }

  public void testBytesCompletionInsertsAngleBracketsAndPlacesCaretInside() {
    myFixture.configureByText(CompactFileType.INSTANCE, "export ledger ledger1: By<caret>;");
    myFixture.completeBasic();
    LookupElement[] elements = myFixture.getLookupElements();
    assertNotNull(elements);
    LookupElement bytesEl = null;
    for (LookupElement el : elements) {
      if ("Bytes".equals(el.getLookupString())) {
        bytesEl = el;
        break;
      }
    }
    assertNotNull("Should find 'Bytes' lookup element", bytesEl);
    myFixture.getLookup().setCurrentItem(bytesEl);
    myFixture.type('\n');
    myFixture.checkResult("export ledger ledger1: Bytes<<caret>>;");
  }

  public void testUintCompletionInsertsAngleBracketsAndPlacesCaretInside() {
    myFixture.configureByText(CompactFileType.INSTANCE, "const x: Ui<caret>;");
    myFixture.completeBasic();
    LookupElement[] elements = myFixture.getLookupElements();
    assertNotNull(elements);
    LookupElement uintEl = null;
    for (LookupElement el : elements) {
      if ("Uint".equals(el.getLookupString())) {
        uintEl = el;
        break;
      }
    }
    assertNotNull("Should find 'Uint' lookup element", uintEl);
    myFixture.getLookup().setCurrentItem(uintEl);
    myFixture.type('\n');
    myFixture.checkResult("const x: Uint<<caret>>;");
  }

  public void testBytesInsideBracketsSuggestsSizeOptions() {
    myFixture.configureByText(CompactFileType.INSTANCE, "export ledger l: Bytes<<caret>>;");
    myFixture.completeBasic();
    List<String> lookupStrings = myFixture.getLookupElementStrings();
    assertNotNull("Lookup strings should not be null", lookupStrings);
    assertTrue("Should suggest '32'", lookupStrings.contains("32"));
    assertTrue("Should suggest '64'", lookupStrings.contains("64"));
    assertTrue("Should suggest '16'", lookupStrings.contains("16"));
    assertTrue("Should suggest '8'", lookupStrings.contains("8"));
  }

  public void testUintInsideBracketsSuggestsBitWidthOptions() {
    myFixture.configureByText(CompactFileType.INSTANCE, "const x: Uint<<caret>>;");
    myFixture.completeBasic();
    List<String> lookupStrings = myFixture.getLookupElementStrings();
    assertNotNull("Lookup strings should not be null", lookupStrings);
    assertTrue("Should suggest '8'", lookupStrings.contains("8"));
    assertTrue("Should suggest '16'", lookupStrings.contains("16"));
    assertTrue("Should suggest '32'", lookupStrings.contains("32"));
    assertTrue("Should suggest '64'", lookupStrings.contains("64"));
    assertTrue("Should suggest '128'", lookupStrings.contains("128"));
    assertTrue("Should suggest '256'", lookupStrings.contains("256"));
  }

  public void testBytesSizeOptionCompletion() {
    myFixture.configureByText(CompactFileType.INSTANCE, "export ledger ledger1: Bytes<<caret>>;");
    myFixture.completeBasic();
    LookupElement[] elements = myFixture.getLookupElements();
    assertNotNull(elements);
    LookupElement item32 = null;
    for (LookupElement el : elements) {
      if ("32".equals(el.getLookupString())) {
        item32 = el;
        break;
      }
    }
    assertNotNull(item32);
    myFixture.getLookup().setCurrentItem(item32);
    myFixture.type('\n');
    myFixture.checkResult("export ledger ledger1: Bytes<32<caret>>;");
  }

  public void testUintSizeOptionCompletion() {
    myFixture.configureByText(CompactFileType.INSTANCE, "const x: Uint<<caret>>;");
    myFixture.completeBasic();
    LookupElement[] elements = myFixture.getLookupElements();
    assertNotNull(elements);
    LookupElement item64 = null;
    for (LookupElement el : elements) {
      if ("64".equals(el.getLookupString())) {
        item64 = el;
        break;
      }
    }
    assertNotNull(item64);
    myFixture.getLookup().setCurrentItem(item64);
    myFixture.type('\n');
    myFixture.checkResult("const x: Uint<64<caret>>;");
  }

  public void testPreconfiguredBytes32Completion() {
    myFixture.configureByText(CompactFileType.INSTANCE, "export ledger ledger1: <caret>;");
    myFixture.completeBasic();
    LookupElement[] elements = myFixture.getLookupElements();
    assertNotNull(elements);
    LookupElement bytes32 = null;
    for (LookupElement el : elements) {
      if ("Bytes<32>".equals(el.getLookupString())) {
        bytes32 = el;
        break;
      }
    }
    assertNotNull(bytes32);
    myFixture.getLookup().setCurrentItem(bytes32);
    myFixture.type('\n');
    myFixture.checkResult("export ledger ledger1: Bytes<32><caret>;");
  }

  public void testPreconfiguredUint64Completion() {
    myFixture.configureByText(CompactFileType.INSTANCE, "const x: <caret>;");
    myFixture.completeBasic();
    LookupElement[] elements = myFixture.getLookupElements();
    assertNotNull(elements);
    LookupElement uint64 = null;
    for (LookupElement el : elements) {
      if ("Uint<64>".equals(el.getLookupString())) {
        uint64 = el;
        break;
      }
    }
    assertNotNull(uint64);
    myFixture.getLookup().setCurrentItem(uint64);
    myFixture.type('\n');
    myFixture.checkResult("const x: Uint<64><caret>;");
  }

  public void testAssertCompletionInsertsParenthesesAndPlacesCaretInside() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        circuit test(): Void {
          <caret>
        }
        """
    );
    myFixture.completeBasic();
    LookupElement[] elements = myFixture.getLookupElements();
    assertNotNull("Lookup elements should not be null", elements);
    LookupElement assertEl = null;
    for (LookupElement el : elements) {
      if ("assert".equals(el.getLookupString())) {
        assertEl = el;
        break;
      }
    }
    assertNotNull("Should find 'assert' lookup element", assertEl);
    myFixture.getLookup().setCurrentItem(assertEl);
    myFixture.type('\n');
    myFixture.checkResult(
        """
        circuit test(): Void {
          assert(<caret>)
        }
        """
    );
  }

  public void testAssertCompletionWithExistingParenthesesDoesNotDuplicate() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        circuit test(): Void {
          <caret>(true, "msg");
        }
        """
    );
    myFixture.completeBasic();
    LookupElement[] elements = myFixture.getLookupElements();
    assertNotNull("Lookup elements should not be null", elements);
    LookupElement assertEl = null;
    for (LookupElement el : elements) {
      if ("assert".equals(el.getLookupString())) {
        assertEl = el;
        break;
      }
    }
    assertNotNull("Should find 'assert' lookup element", assertEl);
    myFixture.getLookup().setCurrentItem(assertEl);
    myFixture.type('\n');
    myFixture.checkResult(
        """
        circuit test(): Void {
          assert(<caret>true, "msg");
        }
        """
    );
  }


  public void testAssertCompletionPrefixAutoInsert() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        circuit test(): Void {
          ass<caret>
        }
        """
    );
    LookupElement[] elements = myFixture.completeBasic();
    if (elements == null) {
      myFixture.checkResult(
          """
          circuit test(): Void {
            assert(<caret>)
          }
          """
      );
    } else {
      LookupElement assertEl = null;
      for (LookupElement el : elements) {
        if ("assert".equals(el.getLookupString())) {
          assertEl = el;
          break;
        }
      }
      assertNotNull(assertEl);
      myFixture.getLookup().setCurrentItem(assertEl);
      myFixture.type('\n');
      myFixture.checkResult(
          """
          circuit test(): Void {
            assert(<caret>)
          }
          """
      );
    }
  }

  public void testAfterPragmaContextClassification() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        pragma <caret>
        """
    );
    PsiElement pos = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
    assertNotNull(pos);
    assertEquals(CompactCompletionContext.Kind.AFTER_PRAGMA, CompactCompletionContext.classify(pos));
  }

  public void testPragmaCompletionDirectivesSuggested() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        pragma <caret>
        """
    );
    myFixture.completeBasic();
    List<String> lookupStrings = myFixture.getLookupElementStrings();
    assertNotNull("Lookup strings should not be null", lookupStrings);
    assertTrue("Should suggest 'language_version'", lookupStrings.contains("language_version"));
    assertTrue("Should suggest 'compiler_version'", lookupStrings.contains("compiler_version"));
    assertFalse("Should NOT suggest 'circuit'", lookupStrings.contains("circuit"));
    assertFalse("Should NOT suggest 'import'", lookupStrings.contains("import"));
  }

  public void testPragmaCompletionFilteringAndInsertion() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        pragma lang<caret>
        """
    );
    myFixture.completeBasic();
    String text = myFixture.getFile().getText();
    assertTrue("File should contain 'pragma language_version ' but was:\n" + text,
        text.contains("pragma language_version "));
  }

  public void testPragmaCompilerVersionInsertion() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        pragma comp<caret>
        """
    );
    myFixture.completeBasic();
    String text = myFixture.getFile().getText();
    assertTrue("File should contain 'pragma compiler_version ' but was:\n" + text,
        text.contains("pragma compiler_version "));
  }

  public void testPragmaContextWithPrefixInsideForm() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        pragma langu<caret>
        """
    );
    PsiElement pos = myFixture.getFile().findElementAt(myFixture.getCaretOffset() - 1);
    assertNotNull(pos);
    assertEquals(CompactCompletionContext.Kind.AFTER_PRAGMA, CompactCompletionContext.classify(pos));
  }

  public void testDefaultCompletionInsertsAngleBracketsAndPlacesCaretInside() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        circuit test(): Void {
          const x = def<caret>
        }
        """
    );
    LookupElement[] elements = myFixture.completeBasic();
    if (elements == null) {
      myFixture.checkResult(
          """
          circuit test(): Void {
            const x = default<<caret>>
          }
          """
      );
    } else {
      LookupElement defaultEl = null;
      for (LookupElement el : elements) {
        if ("default".equals(el.getLookupString())) {
          defaultEl = el;
          break;
        }
      }
      assertNotNull("Should find 'default' lookup element", defaultEl);
      myFixture.getLookup().setCurrentItem(defaultEl);
      myFixture.type('\n');
      myFixture.checkResult(
          """
          circuit test(): Void {
            const x = default<<caret>>
          }
          """
      );
    }
  }

  public void testDefaultCompletionWithExpectedType() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        circuit test(): Void {
          const x: ContractAddress = def<caret>
        }
        """
    );
    myFixture.completeBasic();
    List<String> lookupStrings = myFixture.getLookupElementStrings();
    assertNotNull("Lookup strings should not be null", lookupStrings);
    assertTrue("Should suggest 'default<ContractAddress>'", lookupStrings.contains("default<ContractAddress>"));
    assertTrue("Should suggest generic 'default'", lookupStrings.contains("default"));
  }

  public void testEitherCompletionInTypeContext() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        export ledger l: Eith<caret>;
        """
    );
    LookupElement[] elements = myFixture.completeBasic();
    if (elements == null) {
      myFixture.checkResult(
          """
          export ledger l: Either<<caret>>;
          """
      );
    } else {
      LookupElement eitherEl = null;
      for (LookupElement el : elements) {
        if ("Either".equals(el.getLookupString())) {
          eitherEl = el;
          break;
        }
      }
      assertNotNull("Should find 'Either' lookup element in type context", eitherEl);
      myFixture.getLookup().setCurrentItem(eitherEl);
      myFixture.type('\n');
      myFixture.checkResult(
          """
          export ledger l: Either<<caret>>;
          """
      );
    }
  }

  public void testEitherStructCompletionInValueContext() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        circuit test(): Void {
          const res = Eith<caret>
        }
        """
    );
    LookupElement[] elements = myFixture.completeBasic();
    if (elements == null) {
      myFixture.checkResult(
          """
          circuit test(): Void {
            const res = Either { is_left: true, left: , right: default }<caret>
          }
          """
      );
    } else {
      LookupElement eitherEl = null;
      for (LookupElement el : elements) {
        if ("Either".equals(el.getLookupString())) {
          eitherEl = el;
          break;
        }
      }
      assertNotNull("Should find 'Either' lookup element in value context", eitherEl);
      myFixture.getLookup().setCurrentItem(eitherEl);
      myFixture.type('\n');
      myFixture.checkResult(
          """
          circuit test(): Void {
            const res = Either { is_left: true, left: , right: default }<caret>
          }
          """
      );
    }
  }

  public void testLeftAndRightCompletionInValueContext() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        circuit test(): Void {
          const a = le<caret>
        }
        """
    );
    myFixture.completeBasic();
    List<String> lookupStrings = myFixture.getLookupElementStrings();
    assertNotNull("Lookup strings should not be null", lookupStrings);
    assertTrue("Should suggest 'left' in value context", lookupStrings.contains("left"));
  }

  public void testTrueAndFalseCompletionInGeneralContext() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        circuit test(): Void {
          const b = <caret>
        }
        """
    );
    myFixture.completeBasic();
    List<String> lookupStrings = myFixture.getLookupElementStrings();
    assertNotNull("Lookup strings should not be null", lookupStrings);
    assertTrue("Should suggest 'true' in general value context", lookupStrings.contains("true"));
    assertTrue("Should suggest 'false' in general value context", lookupStrings.contains("false"));
  }

  public void testTrueAndFalseCompletionInBooleanExpectedContext() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        circuit test(): Void {
          if (<caret>)
        }
        """
    );
    myFixture.completeBasic();
    List<String> lookupStrings = myFixture.getLookupElementStrings();
    assertNotNull("Lookup strings should not be null", lookupStrings);
    assertTrue("Should suggest 'true' in boolean expected context", lookupStrings.contains("true"));
    assertTrue("Should suggest 'false' in boolean expected context", lookupStrings.contains("false"));
  }

  public void testStructLiteralFieldCompletionForBoolean() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        circuit test(): Void {
          const x = Either { is_left: <caret> };
        }
        """
    );
    myFixture.completeBasic();
    List<String> lookupStrings = myFixture.getLookupElementStrings();
    assertNotNull("Lookup strings should not be null", lookupStrings);
    assertTrue("Should suggest 'true' for is_left struct field. Actual: " + lookupStrings, lookupStrings.contains("true"));
    assertTrue("Should suggest 'false' for is_left struct field. Actual: " + lookupStrings, lookupStrings.contains("false"));
  }

  public void testDefaultAndEitherPrioritiesInUnrestrictedValueContext() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        circuit test(myVar: Field): Void {
          const x = <caret>
        }
        """
    );
    LookupElement[] elements = myFixture.completeBasic();
    assertNotNull("Lookup elements should not be null", elements);
    List<String> lookupStrings = myFixture.getLookupElementStrings();
    assertNotNull("Lookup strings should not be null", lookupStrings);
    int varIndex = lookupStrings.indexOf("myVar");
    int defaultIndex = lookupStrings.indexOf("default");
    int defaultFieldIndex = lookupStrings.indexOf("default<Field>");
    int eitherIndex = lookupStrings.indexOf("Either");

    assertTrue("myVar should be in lookup elements", varIndex >= 0);
    assertTrue("default should be in lookup elements", defaultIndex >= 0);
    assertTrue("default<Field> should be in lookup elements", defaultFieldIndex >= 0);
    assertTrue("Either should be in lookup elements", eitherIndex >= 0);

    // In unrestricted value context, local variables must be prioritized above generic default / Either
    assertTrue("Local variable 'myVar' (" + varIndex + ") should precede 'default' (" + defaultIndex + ")", varIndex < defaultIndex);
    assertTrue("Local variable 'myVar' (" + varIndex + ") should precede 'default<Field>' (" + defaultFieldIndex + ")", varIndex < defaultFieldIndex);
    assertTrue("Local variable 'myVar' (" + varIndex + ") should precede 'Either' (" + eitherIndex + ")", varIndex < eitherIndex);
  }

  public void testEitherPrioritiesWhenExpectedTypeIsEither() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        struct Either<L, R> { is_left: Boolean, left: L, right: R }
        circuit test(): Void {
          const x: Either<Field, Field> = <caret>
        }
        """
    );
    LookupElement[] elements = myFixture.completeBasic();
    assertNotNull("Lookup elements should not be null", elements);
    List<String> lookupStrings = myFixture.getLookupElementStrings();
    assertNotNull("Lookup strings should not be null", lookupStrings);
    int eitherIndex = lookupStrings.indexOf("Either");
    int leftIndex = lookupStrings.indexOf("left");
    int rightIndex = lookupStrings.indexOf("right");

    assertTrue("Either should be suggested when Either expected", eitherIndex >= 0);
    assertTrue("left should be suggested when Either expected", leftIndex >= 0);
    assertTrue("right should be suggested when Either expected", rightIndex >= 0);
  }
}