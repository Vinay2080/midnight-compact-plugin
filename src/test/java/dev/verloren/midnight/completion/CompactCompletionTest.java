package dev.verloren.midnight.completion;

import com.intellij.psi.PsiElement;
import dev.verloren.midnight.CompactFileType;
import dev.verloren.midnight.psi.CompactNamedElement;
import dev.verloren.midnight.resolve.CompactResolveUtil;

import java.util.Collection;
import java.util.List;

public class CompactCompletionTest extends CompactCompletionTestBase {

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
                    const globalConst = 42;
                    circuit mint(amount: Field) {
                      const localVal = 10;
                      const res = <caret>
                    }
                    """
    );
    PsiElement pos = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
    assertNotNull(pos);
    assertEquals(CompactCompletionContext.Kind.VALUE, CompactCompletionContext.classify(pos));

    Collection<CompactNamedElement> valueDecls = CompactResolveUtil.collectValueDeclarations(pos);
    Collection<String> names = valueDecls.stream().map(CompactNamedElement::getName).toList();
    assertTrue("Should collect localVal", names.contains("localVal"));
    assertTrue("Should collect amount parameter", names.contains("amount"));
    assertTrue("Should collect globalConst", names.contains("globalConst"));
  }

  public void testMemberContextClassification() {
    myFixture.configureByText(CompactFileType.INSTANCE,
            """
                    circuit mint() {
                      const res = user.<caret>
                    }
                    """
    );
    PsiElement pos = myFixture.getFile().findElementAt(myFixture.getCaretOffset() - 1);
    assertNotNull(pos);
    assertEquals(CompactCompletionContext.Kind.MEMBER, CompactCompletionContext.classify(pos));
  }

  public void testGenericParameterTypeCompletion() {
    myFixture.configureByText(CompactFileType.INSTANCE,
            """
                    circuit test<T1, T2>(a: <caret>) {}
                    """
    );
    myFixture.completeBasic();
    List<String> lookupStrings = myFixture.getLookupElementStrings();
    assertNotNull("Lookup strings should not be null", lookupStrings);
    assertTrue("Should suggest generic type parameter T1", lookupStrings.contains("T1"));
    assertTrue("Should suggest generic type parameter T2", lookupStrings.contains("T2"));
  }

  public void testStructMemberCompletionOnParameter() {
    myFixture.configureByText(CompactFileType.INSTANCE,
            """
                    struct User {
                      id: Field;
                      active: Boolean;
                    }
                    circuit check(u: User) {
                      const x = u.<caret>
                    }
                    """
    );
    myFixture.completeBasic();
    List<String> lookupStrings = myFixture.getLookupElementStrings();
    assertNotNull("Lookup strings should not be null", lookupStrings);
    assertTrue("Should suggest 'id' field", lookupStrings.contains("id"));
    assertTrue("Should suggest 'active' field", lookupStrings.contains("active"));
  }

  public void testEnumMemberCompletion() {
    myFixture.configureByText(CompactFileType.INSTANCE,
            """
                    enum Color {
                      Red,
                      Green,
                      Blue
                    }
                    circuit check() {
                      const c = Color.<caret>
                    }
                    """
    );
    myFixture.completeBasic();
    List<String> lookupStrings = myFixture.getLookupElementStrings();
    assertNotNull("Lookup strings should not be null", lookupStrings);
    assertTrue("Should suggest 'Red'", lookupStrings.contains("Red"));
    assertTrue("Should suggest 'Green'", lookupStrings.contains("Green"));
    assertTrue("Should suggest 'Blue'", lookupStrings.contains("Blue"));
  }

  public void testStructMemberCompletionOnLocalConst() {
    myFixture.configureByText(CompactFileType.INSTANCE,
            """
                    struct Point {
                      x: Field;
                      y: Field;
                    }
                    circuit check(pt: Point) {
                      const p = pt;
                      const val = p.<caret>
                    }
                    """
    );
    myFixture.completeBasic();
    List<String> lookupStrings = myFixture.getLookupElementStrings();
    assertNotNull("Lookup strings should not be null", lookupStrings);
    assertTrue("Should suggest 'x' field", lookupStrings.contains("x"));
    assertTrue("Should suggest 'y' field", lookupStrings.contains("y"));
  }

  public void testReturnCompletionFieldTypeAwareness() {
    myFixture.configureByText(CompactFileType.INSTANCE,
            """
                    circuit calculate(count: Field, flag: Boolean): Field {
                      return <caret>
                    }
                    """
    );
    myFixture.completeBasic();
    List<String> lookupStrings = myFixture.getLookupElementStrings();
    assertNotNull("Lookup strings should not be null", lookupStrings);
    assertTrue("Should suggest matching Field parameter 'count'", lookupStrings.contains("count"));
    assertTrue("Should suggest Boolean parameter 'flag'", lookupStrings.contains("flag"));
    int countIndex = lookupStrings.indexOf("count");
    int flagIndex = lookupStrings.indexOf("flag");
    assertTrue("Matching Field type 'count' (" + countIndex + ") should precede Boolean type 'flag' (" + flagIndex + ")",
            countIndex < flagIndex);
  }

  public void testReturnCompletionUintTypeAwareness() {
    myFixture.configureByText(CompactFileType.INSTANCE,
            """
                    circuit calculate(amount: Uint<64>, flag: Boolean): Uint<64> {
                      return <caret>
                    }
                    """
    );
    myFixture.completeBasic();
    List<String> lookupStrings = myFixture.getLookupElementStrings();
    assertNotNull("Lookup strings should not be null", lookupStrings);
    assertTrue("Should suggest matching Uint parameter 'amount'", lookupStrings.contains("amount"));
    assertTrue("Should suggest Boolean parameter 'flag'", lookupStrings.contains("flag"));
    int amountIndex = lookupStrings.indexOf("amount");
    int flagIndex = lookupStrings.indexOf("flag");
    assertTrue("Matching Uint type 'amount' (" + amountIndex + ") should precede Boolean type 'flag' (" + flagIndex + ")",
            amountIndex < flagIndex);
  }

  public void testReturnCompletionBooleanTypeAwareness() {
    myFixture.configureByText(CompactFileType.INSTANCE,
            """
                    circuit isValid(amount: Uint<64>, flag: Boolean): Boolean {
                      return <caret>
                    }
                    """
    );
    myFixture.completeBasic();
    List<String> lookupStrings = myFixture.getLookupElementStrings();
    assertNotNull("Lookup strings should not be null", lookupStrings);
    assertTrue("Should suggest matching Boolean parameter 'flag'", lookupStrings.contains("flag"));
    assertTrue("Should suggest Uint parameter 'amount'", lookupStrings.contains("amount"));
    int flagIndex = lookupStrings.indexOf("flag");
    int amountIndex = lookupStrings.indexOf("amount");
    assertTrue("Matching Boolean type 'flag' (" + flagIndex + ") should precede Uint type 'amount' (" + amountIndex + ")",
            flagIndex < amountIndex);
  }

  public void testReturnCompletionBooleanKeywordsPrioritized() {
    myFixture.configureByText(CompactFileType.INSTANCE,
            """
                    circuit isValid(): Boolean {
                      return <caret>
                    }
                    """
    );
    myFixture.completeBasic();
    List<String> lookupStrings = myFixture.getLookupElementStrings();
    assertNotNull("Lookup strings should not be null", lookupStrings);
    assertTrue("Should suggest 'true'", lookupStrings.contains("true"));
    assertTrue("Should suggest 'false'", lookupStrings.contains("false"));
  }

  public void testReturnCompletionStructTypeAwareness() {
    myFixture.configureByText(CompactFileType.INSTANCE,
            """
                    struct Token {
                      id: Field;
                    }
                    circuit getToken(t: Token, n: Field): Token {
                      return <caret>
                    }
                    """
    );
    myFixture.completeBasic();
    List<String> lookupStrings = myFixture.getLookupElementStrings();
    assertNotNull("Lookup strings should not be null", lookupStrings);
    assertTrue("Should suggest matching Token parameter 't'", lookupStrings.contains("t"));
    assertTrue("Should suggest Field parameter 'n'", lookupStrings.contains("n"));
    int tokenIndex = lookupStrings.indexOf("t");
    int numIndex = lookupStrings.indexOf("n");
    assertTrue("Matching Token type 't' (" + tokenIndex + ") should precede Field type 'n' (" + numIndex + ")",
            tokenIndex < numIndex);
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
}