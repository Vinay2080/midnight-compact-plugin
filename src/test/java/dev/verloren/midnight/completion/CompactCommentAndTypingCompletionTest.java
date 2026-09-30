package dev.verloren.midnight.completion;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.PsiElement;
import dev.verloren.midnight.CompactFileType;

import java.util.List;

public class CompactCommentAndTypingCompletionTest extends CompactCompletionTestBase {

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
}
