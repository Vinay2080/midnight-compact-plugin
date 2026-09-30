package dev.verloren.midnight.highlighter;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.lang.LanguageAnnotators;
import com.intellij.lang.LanguageParserDefinitions;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import dev.verloren.midnight.CompactFileType;
import dev.verloren.midnight.CompactLanguage;
import dev.verloren.midnight.parser.CompactParserDefinition;

import java.util.List;

/**
 * Tests for complex contract, ledger calls, and advanced highlighting scenarios.
 */
public class CompactAdvancedHighlightingTest extends BasePlatformTestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    LanguageParserDefinitions.INSTANCE.addExplicitExtension(
        CompactLanguage.INSTANCE,
        new CompactParserDefinition()
    );
    LanguageAnnotators.INSTANCE.addExplicitExtension(
        CompactLanguage.INSTANCE,
        new CompactHighlightingAnnotator()
    );
  }

  public void testComplexContractAndCircuitDeclarationsHighlighting() {
    String code = """
        export sealed ledger MY_ROLE: Bytes<32>;

        export circuit revokeRole(
            roleId: Bytes<32>,
            account: Either<Bytes<32>, ContractAddress>
        ): Boolean {
            return true;
        }
        """;
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> highlights = myFixture.doHighlighting();

    assertHasHighlight(highlights, "export", CompactHighlighterColors.MODIFIER);
    assertHasHighlight(highlights, "sealed", CompactHighlighterColors.MODIFIER);
    assertHasHighlight(highlights, "MY_ROLE", CompactHighlighterColors.LEDGER_DECLARATION);
    assertHasHighlight(highlights, "Bytes", CompactHighlighterColors.BUILTIN_TYPE);
    assertHasHighlight(highlights, "revokeRole", CompactHighlighterColors.CIRCUIT_DECLARATION);
    assertHasHighlight(highlights, "roleId", CompactHighlighterColors.PARAMETER_DECLARATION);
    assertHasHighlight(highlights, "account", CompactHighlighterColors.PARAMETER_DECLARATION);
    assertHasHighlight(highlights, "Either", CompactHighlighterColors.BUILTIN_TYPE);
    assertHasHighlight(highlights, "ContractAddress", CompactHighlighterColors.BUILTIN_TYPE);
    assertHasHighlight(highlights, "Boolean", CompactHighlighterColors.BUILTIN_TYPE);
  }

  public void testLedgerMemberAndCallHighlighting() {
    String code = """
        export const DEFAULT_ADMIN_ROLE: Bytes<32> = 0;
        export const MY_ROLE: Bytes<32> = 1;
        export ledger _operatorRoles: Map<Bytes<32>, ContractAddress>;

        witness fetchKey(): Bytes<32>;
        circuit hasRole(account: ContractAddress, roleId: Bytes<32>): Boolean { return true; }
        circuit Utils_canonicalize(account: ContractAddress): ContractAddress { return account; }

        export circuit revokeRole(
            roleId: Bytes<32>,
            account: ContractAddress,
            sk: Field
        ): Boolean {
            const hasRoleRes = hasRole(account, roleId);
            const canonical = Utils_canonicalize(account);
            const roleVal = _operatorRoles.lookup(roleId);
            const disc = disclose(sk);
            const admin = DEFAULT_ADMIN_ROLE;
            const currentRole = MY_ROLE;
            return hasRoleRes;
        }
        """;
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> highlights = myFixture.doHighlighting();

    assertHasHighlight(highlights, "_operatorRoles", CompactHighlighterColors.LEDGER_DECLARATION);
    assertHasHighlight(highlights, "_operatorRoles", CompactHighlighterColors.LEDGER_USAGE);
    assertHasHighlight(highlights, "lookup", CompactHighlighterColors.CIRCUIT_CALL);
    assertHasHighlight(highlights, "hasRole", CompactHighlighterColors.CIRCUIT_CALL);
    assertHasHighlight(highlights, "Utils_canonicalize", CompactHighlighterColors.CIRCUIT_CALL);
    assertHasHighlight(highlights, "disclose", CompactHighlighterColors.BUILTIN_FUNCTION);
    assertHasHighlight(highlights, "DEFAULT_ADMIN_ROLE", CompactHighlighterColors.CONSTANT_DECLARATION);
    assertHasHighlight(highlights, "DEFAULT_ADMIN_ROLE", CompactHighlighterColors.CONSTANT_USAGE);
    assertHasHighlight(highlights, "MY_ROLE", CompactHighlighterColors.CONSTANT_DECLARATION);
    assertHasHighlight(highlights, "MY_ROLE", CompactHighlighterColors.CONSTANT_USAGE);
    assertHasHighlight(highlights, "roleId", CompactHighlighterColors.PARAMETER_DECLARATION);
    assertHasHighlight(highlights, "roleId", CompactHighlighterColors.PARAMETER_USAGE);
    assertHasHighlight(highlights, "account", CompactHighlighterColors.PARAMETER_DECLARATION);
    assertHasHighlight(highlights, "account", CompactHighlighterColors.PARAMETER_USAGE);
    assertHasHighlight(highlights, "sk", CompactHighlighterColors.PARAMETER_DECLARATION);
    assertHasHighlight(highlights, "sk", CompactHighlighterColors.PARAMETER_USAGE);
  }

  private static void assertHasHighlight(List<HighlightInfo> highlights, String text, TextAttributesKey expectedKey) {
    boolean found = highlights.stream().anyMatch(h ->
        text.equals(h.getText()) && expectedKey.equals(h.forcedTextAttributesKey)
    );
    assertTrue("Expected text '" + text + "' to be highlighted with " + expectedKey.getExternalName()
        + ", but was not. All highlights: " + highlights.stream().map(h -> "'" + h.getText() + "':" + (h.forcedTextAttributesKey != null ? h.forcedTextAttributesKey.getExternalName() : "null")).toList(), found);
  }
}
