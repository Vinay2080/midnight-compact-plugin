package dev.verloren.midnight.completion;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.PsiElement;
import dev.verloren.midnight.CompactFileType;

import java.util.List;

public class CompactTopLevelCompletionTest extends CompactCompletionTestBase {

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
}
