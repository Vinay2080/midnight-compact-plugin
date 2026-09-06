package dev.verloren.midnight.intention;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import dev.verloren.midnight.psi.CompactPragmaForm;
import dev.verloren.midnight.settings.MidnightProjectSettings;

import java.util.List;

public class CompactPragmaIntentionTest extends BasePlatformTestCase {

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
}
