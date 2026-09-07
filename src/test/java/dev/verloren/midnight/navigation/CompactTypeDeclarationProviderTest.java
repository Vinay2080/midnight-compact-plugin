package dev.verloren.midnight.navigation;

import com.intellij.lang.LanguageParserDefinitions;
import com.intellij.psi.PsiElement;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import dev.verloren.midnight.CompactFileType;
import dev.verloren.midnight.CompactLanguage;
import dev.verloren.midnight.completion.CompactCompletionContext;
import dev.verloren.midnight.parser.CompactParserDefinition;
import dev.verloren.midnight.psi.*;

public class CompactTypeDeclarationProviderTest extends BasePlatformTestCase {

  private CompactTypeDeclarationProvider provider;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    LanguageParserDefinitions.INSTANCE.addExplicitExtension(
        CompactLanguage.INSTANCE,
        new CompactParserDefinition()
    );
    provider = new CompactTypeDeclarationProvider();
  }

  public void testGotoTypeFromVariableBinding() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        struct User { id: Field; }
        circuit test() {
            const u = User { id: 1 };
            const userRef = <caret>u;
        }
        """
    );
    PsiElement element = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
    assertNotNull("Leaf element at caret should not be null", element);

    PsiElement[] targets = provider.getSymbolTypeDeclarations(element);
    assertNotNull("Should resolve type declarations for variable 'u'", targets);
    assertEquals("Should find exactly 1 type declaration", 1, targets.length);
    assertInstanceOf(targets[0], CompactStructDefinition.class);
    assertEquals("User", ((CompactNamedElement) targets[0]).getName());
  }

  public void testGotoTypeFromParameter() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        struct Account { balance: Field; }
        circuit verify(<caret>acc: Account) {}
        """
    );
    PsiElement element = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
    assertNotNull(element);

    PsiElement[] targets = provider.getSymbolTypeDeclarations(element);
    assertNotNull("Should resolve type declaration for parameter 'acc'", targets);
    assertEquals(1, targets.length);
    assertInstanceOf(targets[0], CompactStructDefinition.class);
    assertEquals("Account", ((CompactNamedElement) targets[0]).getName());
  }

  public void testGotoTypeFromParameterUsageInsideBody() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        struct Account { balance: Field; }
        circuit verify(acc: Account) {
            const copy = <caret>acc;
        }
        """
    );
    PsiElement element = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
    assertNotNull(element);

    PsiElement[] targets = provider.getSymbolTypeDeclarations(element);
    assertNotNull("Should resolve type declaration for parameter usage 'acc'", targets);
    assertEquals(1, targets.length);
    assertInstanceOf(targets[0], CompactStructDefinition.class);
    assertEquals("Account", ((CompactNamedElement) targets[0]).getName());
  }

  public void testGotoTypeFromEnumVariable() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        enum Status { Pending, Active, Done }
        circuit test() {
            const s = Status.Active;
            const current = <caret>s;
        }
        """
    );
    PsiElement element = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
    assertNotNull(element);

    PsiElement[] targets = provider.getSymbolTypeDeclarations(element);
    assertNotNull("Should resolve type declaration for enum variable 's'", targets);
    assertEquals(1, targets.length);
    assertInstanceOf(targets[0], CompactEnumDefinition.class);
    assertEquals("Status", ((CompactNamedElement) targets[0]).getName());
  }

  public void testGotoTypeFromEnumMemberAccess() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        enum Status { Pending, Active, Done }
        circuit test() {
            const s = Status.<caret>Active;
        }
        """
    );
    PsiElement element = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
    assertNotNull(element);

    PsiElement[] targets = provider.getSymbolTypeDeclarations(element);
    assertNotNull("Should resolve type declaration for enum member 'Active'", targets);
    assertEquals(1, targets.length);
    assertInstanceOf(targets[0], CompactEnumDefinition.class);
    assertEquals("Status", ((CompactNamedElement) targets[0]).getName());
  }

  public void testGotoTypeFromStructFieldAccess() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        struct Profile { name: Bytes<32>; }
        struct User { profile: Profile; }
        circuit test(u: User) {
            const p = u.<caret>profile;
        }
        """
    );
    PsiElement element = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
    assertNotNull(element);

    PsiElement[] targets = provider.getSymbolTypeDeclarations(element);
    assertNotNull("Should resolve type declaration for struct field access", targets);
    assertEquals(1, targets.length);
    assertInstanceOf(targets[0], CompactStructDefinition.class);
    assertEquals("Profile", ((CompactNamedElement) targets[0]).getName());
  }

  public void testGotoTypeFromTypeAlias() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        type Salt = Bytes<32>;
        circuit test(s: Salt) {
            const mySalt = <caret>s;
        }
        """
    );
    PsiElement element = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
    assertNotNull(element);

    PsiElement[] targets = provider.getSymbolTypeDeclarations(element);
    assertNotNull("Should resolve type alias declaration 'Salt'", targets);
    assertEquals(1, targets.length);
    assertInstanceOf(targets[0], CompactTypeDefinition.class);
    assertEquals("Salt", ((CompactNamedElement) targets[0]).getName());
  }

  public void testGotoTypeFromTypeAliasToTargetStruct() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        struct TargetStruct { val: Field; }
        type Alias = <caret>TargetStruct;
        """
    );
    PsiElement element = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
    assertNotNull(element);

    PsiElement[] targets = provider.getSymbolTypeDeclarations(element);
    assertNotNull("Should resolve underlying target struct from type alias", targets);
    assertEquals(1, targets.length);
    assertInstanceOf(targets[0], CompactStructDefinition.class);
    assertEquals("TargetStruct", ((CompactNamedElement) targets[0]).getName());
  }

  public void testGotoTypeFromLedgerState() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        struct LedgerConfig { maxSupply: Uint<64>; }
        ledger config: LedgerConfig;
        circuit test() {
            const c = <caret>config;
        }
        """
    );
    PsiElement element = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
    assertNotNull(element);

    PsiElement[] targets = provider.getSymbolTypeDeclarations(element);
    assertNotNull("Should resolve type declaration for ledger 'config'", targets);
    assertEquals(1, targets.length);
    assertInstanceOf(targets[0], CompactStructDefinition.class);
    assertEquals("LedgerConfig", ((CompactNamedElement) targets[0]).getName());
  }

  public void testGotoTypeFromContractInterface() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        contract IToken {
            witness transfer(): Void;
        }
        contract implements <caret>IToken;
        """
    );
    PsiElement element = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
    assertNotNull(element);

    PsiElement[] targets = provider.getSymbolTypeDeclarations(element);
    assertNotNull("Should resolve external contract interface", targets);
    assertEquals(1, targets.length);
    assertInstanceOf(targets[0], CompactExternalContractDeclaration.class);
    assertEquals("IToken", ((CompactNamedElement) targets[0]).getName());
  }

  public void testGotoTypeCrossFileInclude() {
    myFixture.addFileToProject("types.compact",
        """
        struct SharedAccount {
            id: Field;
        }
        """
    );
    myFixture.configureByText("main.compact",
        """
        include "types.compact";
        circuit test(a: SharedAccount) {
            const ref = <caret>a;
        }
        """
    );
    PsiElement element = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
    assertNotNull(element);

    PsiElement[] targets = provider.getSymbolTypeDeclarations(element);
    assertNotNull("Should resolve struct defined in included file", targets);
    assertEquals(1, targets.length);
    assertInstanceOf(targets[0], CompactStructDefinition.class);
    assertEquals("SharedAccount", ((CompactNamedElement) targets[0]).getName());
    assertEquals("types.compact", targets[0].getContainingFile().getName());
  }

  public void testGotoTypeGracefulOnPrimitive() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        circuit test(x: Uint<32>) {
            const v = <caret>x;
        }
        """
    );
    PsiElement element = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
    assertNotNull(element);

    PsiElement[] targets = provider.getSymbolTypeDeclarations(element);
    assertNull("Built-in primitive type should gracefully yield null", targets);
  }

  public void testGotoTypeGracefulOnUnresolved() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        circuit test() {
            const v = <caret>unknownVariable;
        }
        """
    );
    PsiElement element = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
    assertNotNull(element);

    PsiElement[] targets = provider.getSymbolTypeDeclarations(element);
    assertNull("Unresolved expression should gracefully yield null", targets);
  }

  public void testNonInterferenceWithCompletionContext() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        struct CustomType {}
        circuit mint(amount: <caret>) {}
        """
    );
    PsiElement typePos = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
    assertNotNull(typePos);
    assertEquals("CompactCompletionContext must continue classifying TYPE context identically",
        CompactCompletionContext.Kind.TYPE, CompactCompletionContext.classify(typePos));

    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        circuit test() {
            const myVar = 42;
            const copy = <caret>;
        }
        """
    );
    PsiElement valPos = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
    assertNotNull(valPos);
    assertEquals("CompactCompletionContext must continue classifying VALUE context identically",
        CompactCompletionContext.Kind.VALUE, CompactCompletionContext.classify(valPos));
  }
}
