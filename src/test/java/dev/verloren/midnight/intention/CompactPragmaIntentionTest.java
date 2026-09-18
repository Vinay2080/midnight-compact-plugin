package dev.verloren.midnight.intention;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import dev.verloren.midnight.psi.CompactPragmaForm;
import dev.verloren.midnight.settings.MidnightProjectSettings;
import dev.verloren.midnight.version.CompactVersionManager;

import java.util.Comparator;
import java.util.List;
import java.util.SequencedMap;
import java.util.TreeMap;

public class CompactPragmaIntentionTest extends BasePlatformTestCase {

  @Override
  protected void tearDown() throws Exception {
    CompactVersionManager.setInstalledVersionsForTesting(null);
    super.tearDown();
  }

  public void testPragmaFormAccessors() {
    PsiFile file = myFixture.configureByText("test.compact", "pragma language_version >= 0.26.0;\n");
    CompactPragmaForm pragma = PsiTreeUtil.findChildOfType(file, CompactPragmaForm.class);
    assertNotNull(pragma);
    assertEquals("language_version", pragma.getPragmaName());
    assertEquals(">= 0.26.0", pragma.getConstraintText());
    assertEquals("0.26.0", pragma.getRequiredVersion());
  }

  public void testAvailableIntentionsOnPragma() {
    // When active compiler (0.26.0) does not satisfy pragma >= 0.26.0 (because toolchain 0.26.0 is language 0.18.0)
    MidnightProjectSettings.getInstance(getProject()).selectedCompilerVersion = "0.26.0";

    myFixture.configureByText("test.compact", "pragma <caret>language_version >= 0.26.0;\n");
    List<IntentionAction> intentions = myFixture.getAvailableIntentions();
    boolean foundSwitchOrDownload = intentions.stream()
        .anyMatch(it -> it.getText().contains("Compact 0.26.0"));
    assertTrue("Should offer intention to switch or download Compact 0.26.0", foundSwitchOrDownload);
  }

  public void testIntentionOffersInstalledHigherCompilerDirectly() {
    MidnightProjectSettings settings = MidnightProjectSettings.getInstance(getProject());
    settings.selectedCompilerVersion = "0.26.0"; // Language 0.18.0

    // Mock installed versions: 0.34.0 (Language 0.26.0) is installed
    SequencedMap<String, String> installed = new TreeMap<>(Comparator.reverseOrder());
    installed.put("0.34.0", "/mock/bin/0.34.0");
    installed.put("0.26.0", "/mock/bin/0.26.0");
    CompactVersionManager.setInstalledVersionsForTesting(installed);

    myFixture.configureByText("test.compact", "pragma <caret>language_version >= 0.20;\n");
    List<IntentionAction> intentions = myFixture.getAvailableIntentions();

    IntentionAction switchIntention = intentions.stream()
        .filter(it -> it.getText().contains("Switch project compiler to Compact 0.26.0 (v0.34.0)"))
        .findFirst()
        .orElse(null);
    assertNotNull("Should offer intention to switch to installed 0.34.0 (Language 0.26.0)", switchIntention);

    boolean hasDownload = intentions.stream()
        .anyMatch(it -> it.getText().contains("Download and use Compact 0.20"));
    assertFalse("Should not offer download when installed higher version satisfies constraint", hasDownload);

    // Apply the intention
    myFixture.launchAction(switchIntention);
    assertEquals("0.34.0", settings.selectedCompilerVersion);
  }
}
