package dev.verloren.midnight.resolve;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import dev.verloren.midnight.CompactFileType;
import dev.verloren.midnight.psi.CompactLedgerDeclaration;
import dev.verloren.midnight.psi.CompactNamedElement;

public class CompactLedgerResolveTest extends BasePlatformTestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    com.intellij.lang.LanguageParserDefinitions.INSTANCE.addExplicitExtension(
        dev.verloren.midnight.CompactLanguage.INSTANCE,
        new dev.verloren.midnight.parser.CompactParserDefinition()
    );
  }

  public void testTopLevelLedgerResolutionAfterCircuit() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        export circuit clear(): [] {
          <caret>round.increment(1);
        }

        export ledger round: Counter;
        """
    );
    PsiReference ref = myFixture.getReferenceAtCaretPosition();
    assertNotNull("Reference should exist at caret", ref);
    PsiElement resolved = ref.resolve();
    assertNotNull("Top-level ledger 'round' declared after circuit should resolve", resolved);
    assertInstanceOf(resolved, CompactLedgerDeclaration.class);
    assertEquals("round", ((CompactNamedElement) resolved).getName());
  }

  public void testTopLevelLedgerResolutionAfterConstructor() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        constructor(sk: Bytes<32>, v: Uint<64>) {
          authority = disclose(publicKey(<caret>round, sk));
        }

        export ledger round: Counter;
        """
    );
    PsiReference ref = myFixture.getReferenceAtCaretPosition();
    assertNotNull("Reference should exist at caret", ref);
    PsiElement resolved = ref.resolve();
    assertNotNull("Top-level ledger 'round' declared after constructor should resolve", resolved);
    assertInstanceOf(resolved, CompactLedgerDeclaration.class);
    assertEquals("round", ((CompactNamedElement) resolved).getName());
  }

  public void testParameterPrecedenceOverTopLevelLedger() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        circuit publicKey(round: Field, sk: Bytes<32>): Field {
          return <caret>round;
        }

        export ledger round: Counter;
        """
    );
    PsiReference ref = myFixture.getReferenceAtCaretPosition();
    assertNotNull("Reference should exist at caret", ref);
    PsiElement resolved = ref.resolve();
    assertNotNull("Parameter 'round' should resolve", resolved);
    assertFalse("Should resolve to parameter, not ledger", resolved instanceof CompactLedgerDeclaration);
    assertEquals("round", ((CompactNamedElement) resolved).getName());
  }
}
