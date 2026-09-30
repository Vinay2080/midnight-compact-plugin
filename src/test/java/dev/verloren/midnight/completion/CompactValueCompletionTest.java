package dev.verloren.midnight.completion;

import com.intellij.codeInsight.lookup.LookupElement;
import dev.verloren.midnight.CompactFileType;

import java.util.List;

public class CompactValueCompletionTest extends CompactCompletionTestBase {

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
