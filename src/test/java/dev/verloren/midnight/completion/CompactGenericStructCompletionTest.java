package dev.verloren.midnight.completion;

import com.intellij.codeInsight.lookup.LookupElement;
import dev.verloren.midnight.CompactFileType;

import java.util.List;

public class CompactGenericStructCompletionTest extends CompactCompletionTestBase {

  public void testTypedEitherStructCompletionInExpectedTypeContext() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        export pure circuit zeroAccount(): Either<Bytes<32>, ContractAddress> {
          return Eith<caret>
        }
        """
    );
    LookupElement[] elements = myFixture.completeBasic();
    assertNotNull(elements);
    LookupElement eitherTyped = null;
    for (LookupElement el : elements) {
      if ("Either<Bytes<32>, ContractAddress>".equals(el.getLookupString())
          || "Either".equals(el.getLookupString())) {
        eitherTyped = el;
        break;
      }
    }
    assertNotNull("Should find typed Either lookup element", eitherTyped);
    myFixture.getLookup().setCurrentItem(eitherTyped);
    myFixture.type('\n');
    String text = myFixture.getFile().getText();
    assertTrue("File should contain expanded typed Either struct literal but was:\n" + text,
        text.contains("return Either<Bytes<32>, ContractAddress> { is_left: true, left: , right: default<ContractAddress> }"));
  }

  public void testGenericEitherTypeArgsParser() {
    CompactCompletionContributor.TypeArgs args1 =
        CompactCompletionContributor.parseEitherTypeArgs("Either<Bytes<32>, ContractAddress>");
    assertNotNull(args1);
    assertEquals("Bytes<32>", args1.left());
    assertEquals("ContractAddress", args1.right());

    CompactCompletionContributor.TypeArgs args2 =
        CompactCompletionContributor.parseEitherTypeArgs("Either<T1, T2>");
    assertNotNull(args2);
    assertEquals("T1", args2.left());
    assertEquals("T2", args2.right());

    CompactCompletionContributor.TypeArgs args3 =
        CompactCompletionContributor.parseEitherTypeArgs("Either<Vector<2, Field>, Either<Boolean, Field>>");
    assertNotNull(args3);
    assertEquals("Vector<2, Field>", args3.left());
    assertEquals("Either<Boolean, Field>", args3.right());

    assertNull(CompactCompletionContributor.parseEitherTypeArgs("Field"));
    assertNull(CompactCompletionContributor.parseEitherTypeArgs("Either"));
  }

  public void testGenericStructMemberCompletion() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        struct Either<A, B> {
          is_left: Boolean;
          left: A;
          right: B;
        }

        export pure circuit isTargetZero(target: Either<Bytes<32>, ContractAddress>): Boolean {
          if (target.<caret>) {
            return false;
          }
          return true;
        }
        """
    );
    myFixture.completeBasic();
    List<String> lookupStrings = myFixture.getLookupElementStrings();
    assertNotNull("Lookup strings should not be null", lookupStrings);
    assertTrue("Should suggest 'is_left' for Either member access. Actual: " + lookupStrings, lookupStrings.contains("is_left"));
    assertTrue("Should suggest 'left' for Either member access. Actual: " + lookupStrings, lookupStrings.contains("left"));
    assertTrue("Should suggest 'right' for Either member access. Actual: " + lookupStrings, lookupStrings.contains("right"));
  }

  public void testTypedEitherRightHelperCompletion() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        export circuit selfAsRecipient(): Either<ZswapCoinPublicKey, ContractAddress> {
          return rig<caret>
        }
        """
    );
    LookupElement[] elements = myFixture.completeBasic();
    assertNotNull(elements);
    LookupElement rightEl = null;
    for (LookupElement el : elements) {
      if ("right<ZswapCoinPublicKey, ContractAddress>".equals(el.getLookupString())
          || "right".equals(el.getLookupString())) {
        rightEl = el;
        break;
      }
    }
    assertNotNull("Should find typed 'right' helper completion", rightEl);
    myFixture.getLookup().setCurrentItem(rightEl);
    myFixture.type('\n');
    String text = myFixture.getFile().getText();
    assertTrue("File should contain expanded typed right helper call but was:\n" + text,
        text.contains("return right<ZswapCoinPublicKey, ContractAddress>(<caret>)")
        || text.contains("return right<ZswapCoinPublicKey, ContractAddress>()"));
  }

  public void testTypedEitherLeftHelperCompletion() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        export circuit canonicalZero(): Either<Bytes<32>, ContractAddress> {
          return lef<caret>
        }
        """
    );
    LookupElement[] elements = myFixture.completeBasic();
    assertNotNull(elements);
    LookupElement leftEl = null;
    for (LookupElement el : elements) {
      if ("left<Bytes<32>, ContractAddress>".equals(el.getLookupString())
          || "left".equals(el.getLookupString())) {
        leftEl = el;
        break;
      }
    }
    assertNotNull("Should find typed 'left' helper completion", leftEl);
    myFixture.getLookup().setCurrentItem(leftEl);
    myFixture.type('\n');
    String text = myFixture.getFile().getText();
    assertTrue("File should contain expanded typed left helper call but was:\n" + text,
        text.contains("return left<Bytes<32>, ContractAddress>(<caret>)")
        || text.contains("return left<Bytes<32>, ContractAddress>()"));
  }
}
