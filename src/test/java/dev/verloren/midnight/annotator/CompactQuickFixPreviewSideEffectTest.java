package dev.verloren.midnight.annotator;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo;
import com.intellij.lang.LanguageParserDefinitions;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import dev.verloren.midnight.CompactFileType;
import dev.verloren.midnight.CompactLanguage;
import dev.verloren.midnight.inspection.CompactPragmaVersionInspection;
import dev.verloren.midnight.intention.CompactSwitchCompilerVersionIntention;
import dev.verloren.midnight.parser.CompactParserDefinition;
import dev.verloren.midnight.settings.MidnightProjectSettings;

import java.util.List;

/**
 * Tests verifying that compiler quick-fixes and intentions do not trigger
 * SideEffectGuard exceptions (such as Side effect not allowed: INVOKE_LATER)
 * during intention preview generation.
 */
public class CompactQuickFixPreviewSideEffectTest extends BasePlatformTestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    LanguageParserDefinitions.INSTANCE.addExplicitExtension(
        CompactLanguage.INSTANCE,
        new CompactParserDefinition()
    );
  }

  public void testSwitchCompilerQuickFixReturnsEmptyPreview() {
    CompactSwitchCompilerQuickFix fix = new CompactSwitchCompilerQuickFix("0.26.0");
    PsiFile file = myFixture.configureByText(CompactFileType.INSTANCE, "pragma language_version >= 0.26.0;\n");

    IntentionPreviewInfo info = fix.generatePreview(getProject(), myFixture.getEditor(), file);
    assertEquals("Switch compiler quick-fix should return EMPTY preview to avoid side effects",
        IntentionPreviewInfo.EMPTY, info);
  }

  public void testSwitchCompilerVersionIntentionReturnsEmptyPreview() {
    CompactSwitchCompilerVersionIntention intention = new CompactSwitchCompilerVersionIntention();
    PsiFile file = myFixture.configureByText(CompactFileType.INSTANCE, "pragma language_version >= 0.26.0;\n");

    IntentionPreviewInfo info = intention.generatePreview(getProject(), myFixture.getEditor(), file);
    assertEquals("Switch compiler intention should return EMPTY preview to avoid side effects",
        IntentionPreviewInfo.EMPTY, info);
  }

  @SuppressWarnings("unchecked")
  public void testIntentionPreviewsDuringInspectionMismatchDoNotThrow() {
    MidnightProjectSettings settings = MidnightProjectSettings.getInstance(getProject());
    settings.selectedCompilerVersion = "0.26.0";
    settings.customCompilerPath = "";

    // Compiler 0.26.0 is language 0.18.0, so >= 0.26.0 triggers mismatch inspection
    myFixture.enableInspections(CompactPragmaVersionInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, "pragma <caret>language_version >= 0.26.0;\n");

    List<IntentionAction> intentions = myFixture.getAvailableIntentions();
    assertFalse("Expected intentions to be available", intentions.isEmpty());

    for (IntentionAction action : intentions) {
      String text = action.getText();
      if (text.contains("Compact") || text.contains("pragma")) {
        // Generating preview should not throw SideEffectGuard: INVOKE_LATER.
        // If IntentionPreviewInfo.EMPTY is returned, getIntentionPreviewText safely returns null.
        try {
          myFixture.getIntentionPreviewText(action);
        } catch (Exception e) {
          fail("Generating preview for '" + text + "' threw unexpected exception: " + e.getMessage());
        }
      }
    }
  }

  public void testClearProblemsAndRestartNoOpsDuringPreview() {
    PsiFile file = myFixture.configureByText(CompactFileType.INSTANCE, "pragma language_version >= 0.26.0;\n");
    // Should safely return without throwing INVOKE_LATER when called directly
    CompactProblemUtil.clearProblemsAndRestart(getProject(), file.getVirtualFile());
  }
}
