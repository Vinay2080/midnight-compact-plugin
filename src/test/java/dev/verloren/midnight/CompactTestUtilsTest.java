package dev.verloren.midnight;

import com.intellij.lang.LanguageParserDefinitions;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import dev.verloren.midnight.parser.CompactParserDefinition;

public class CompactTestUtilsTest extends BasePlatformTestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    LanguageParserDefinitions.INSTANCE.addExplicitExtension(
        CompactLanguage.INSTANCE,
        new CompactParserDefinition()
    );
  }

  public void testDoCheckResolveLocalConst() {
    CompactTestUtils.doCheckResolve(myFixture, """
        circuit test() {
          const /*def*/targetVal = 42;
          const result = /*caret*/targetVal;
        }
        """);
  }

  public void testDoCheckResolveParameter() {
    CompactTestUtils.doCheckResolve(myFixture, """
        circuit compute(/*def*/param1: Uint<32>): Uint<32> {
          return /*caret*/param1;
        }
        """);
  }

  public void testDoCheckNoResolveUnboundIdentifier() {
    CompactTestUtils.doCheckNoResolve(myFixture, """
        circuit compute(): Uint<32> {
          return /*caret*/unknownSymbol;
        }
        """);
  }
}
