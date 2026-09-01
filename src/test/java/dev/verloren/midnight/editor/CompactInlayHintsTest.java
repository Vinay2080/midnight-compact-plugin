package dev.verloren.midnight.editor;

import com.intellij.lang.LanguageParserDefinitions;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import dev.verloren.midnight.CompactFileType;
import dev.verloren.midnight.CompactLanguage;
import dev.verloren.midnight.parser.CompactParserDefinition;
import dev.verloren.midnight.psi.CompactCallExprImpl;

import java.util.List;

public class CompactInlayHintsTest extends BasePlatformTestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    LanguageParserDefinitions.INSTANCE.addExplicitExtension(
        CompactLanguage.INSTANCE,
        new CompactParserDefinition()
    );
  }

  public void testCircuitCallParameterHints() {
    String code = """
        circuit compute(round: Field, sk: Bytes<32>): Field {
            return round;
        }

        circuit test(): [] {
            const res = compute(1, 2);
        }
        """;
    myFixture.configureByText(CompactFileType.INSTANCE, code);

    CompactCallExprImpl call = PsiTreeUtil.findChildOfType(myFixture.getFile(), CompactCallExprImpl.class);
    assertNotNull("Call expression should exist", call);

    List<CompactInlayHintsProvider.HintInfo> hints = CompactInlayHintsProvider.computeHints(call);

    assertEquals("Should generate 2 parameter hints", 2, hints.size());
    assertEquals("round", hints.get(0).label());
    assertEquals("sk", hints.get(1).label());
  }

  public void testWitnessCallParameterHints() {
    String code = """
        witness query(user: Field, nonce: Field): Field;

        circuit test(): [] {
            const w = query(100, 200);
        }
        """;
    myFixture.configureByText(CompactFileType.INSTANCE, code);

    CompactCallExprImpl call = PsiTreeUtil.findChildOfType(myFixture.getFile(), CompactCallExprImpl.class);
    assertNotNull("Call expression should exist", call);

    List<CompactInlayHintsProvider.HintInfo> hints = CompactInlayHintsProvider.computeHints(call);

    assertEquals("Should generate 2 parameter hints for witness call", 2, hints.size());
    assertEquals("user", hints.get(0).label());
    assertEquals("nonce", hints.get(1).label());
  }

  public void testMatchingArgumentNameSuppressesHint() {
    String code = """
        circuit compute(round: Field, sk: Bytes<32>): Field {
            return round;
        }

        circuit test(round: Field): [] {
            const res = compute(round, 42);
        }
        """;
    myFixture.configureByText(CompactFileType.INSTANCE, code);

    CompactCallExprImpl call = PsiTreeUtil.findChildOfType(myFixture.getFile(), CompactCallExprImpl.class);
    assertNotNull("Call expression should exist", call);

    List<CompactInlayHintsProvider.HintInfo> hints = CompactInlayHintsProvider.computeHints(call);

    assertEquals("Should only generate 1 hint since first argument name matches param name", 1, hints.size());
    assertEquals("sk", hints.getFirst().label());
  }
}
