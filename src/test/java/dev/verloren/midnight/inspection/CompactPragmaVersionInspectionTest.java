package dev.verloren.midnight.inspection;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.lang.LanguageParserDefinitions;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import dev.verloren.midnight.CompactFileType;
import dev.verloren.midnight.CompactLanguage;
import dev.verloren.midnight.parser.CompactParserDefinition;
import dev.verloren.midnight.settings.MidnightProjectSettings;
import dev.verloren.midnight.version.CompactVersionManager;

import java.util.Comparator;
import java.util.List;
import java.util.SequencedMap;
import java.util.TreeMap;

public class CompactPragmaVersionInspectionTest extends BasePlatformTestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    LanguageParserDefinitions.INSTANCE.addExplicitExtension(
        CompactLanguage.INSTANCE,
        new CompactParserDefinition()
    );
  }

  @Override
  protected void tearDown() throws Exception {
    CompactVersionManager.setInstalledVersionsForTesting(null);
    super.tearDown();
  }

  @SuppressWarnings("unchecked")
  public void testPragmaMismatchWhenNoCompilerConfigured() {
    MidnightProjectSettings settings = MidnightProjectSettings.getInstance(getProject());
    // Toolchain 0.26.0 corresponds to language 0.18.0, so it does not satisfy pragma >= 0.26.0
    settings.selectedCompilerVersion = "0.26.0";
    settings.customCompilerPath = "";

    String code = "pragma language_version >= 0.26.0;\n";
    myFixture.enableInspections(CompactPragmaVersionInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);

    List<HighlightInfo> errors = myFixture.doHighlighting().stream()
        .filter(h -> h.getSeverity() == HighlightSeverity.ERROR)
        .toList();

    assertFalse("Should report error when compiler does not satisfy pragma", errors.isEmpty());
    assertTrue(errors.getFirst().getDescription().toLowerCase().contains("pragma"));
  }

  @SuppressWarnings("unchecked")
  public void testPragmaSatisfiedWithBareVersionAndHigherCompiler() {
    MidnightProjectSettings settings = MidnightProjectSettings.getInstance(getProject());
    // Toolchain 0.34.0 corresponds to language 0.26.0, which satisfies pragma 0.23 (>= 0.23.0)
    settings.selectedCompilerVersion = "0.34.0";
    settings.customCompilerPath = "";

    String code = "pragma language_version 0.23;\n";
    myFixture.enableInspections(CompactPragmaVersionInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);

    List<HighlightInfo> errors = myFixture.doHighlighting().stream()
        .filter(h -> h.getSeverity() == HighlightSeverity.ERROR)
        .toList();

    assertTrue("Higher compiler 0.34.0 (lang 0.26.0) should satisfy pragma 0.23 without errors", errors.isEmpty());
  }

  @SuppressWarnings("unchecked")
  public void testPragmaMismatchWithBareVersionAndLowerCompiler() {
    MidnightProjectSettings settings = MidnightProjectSettings.getInstance(getProject());
    // Toolchain 0.26.0 corresponds to language 0.18.0, which is < 0.23.0
    settings.selectedCompilerVersion = "0.26.0";
    settings.customCompilerPath = "";

    String code = "pragma language_version 0.23;\n";
    myFixture.enableInspections(CompactPragmaVersionInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);

    List<HighlightInfo> errors = myFixture.doHighlighting().stream()
        .filter(h -> h.getSeverity() == HighlightSeverity.ERROR)
        .toList();

    assertFalse("Lower compiler 0.26.0 (lang 0.18.0) should report error for pragma 0.23", errors.isEmpty());
  }

  @SuppressWarnings("unchecked")
  public void testInstalledHigherVersionSuggestedWhenOpenConstraint() {
    MidnightProjectSettings settings = MidnightProjectSettings.getInstance(getProject());
    settings.selectedCompilerVersion = "0.26.0"; // Language 0.18.0
    settings.customCompilerPath = "";

    // Mock installed versions: 0.34.0 (Language 0.26.0) and 0.26.0 (Language 0.18.0)
    SequencedMap<String, String> installed = new TreeMap<>(Comparator.reverseOrder());
    installed.put("0.34.0", "/mock/bin/0.34.0");
    installed.put("0.26.0", "/mock/bin/0.26.0");
    CompactVersionManager.setInstalledVersionsForTesting(installed);

    String code = "pragma <caret>language_version >= 0.20;\n";
    myFixture.enableInspections(CompactPragmaVersionInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    myFixture.doHighlighting();

    List<IntentionAction> fixes = myFixture.getAllQuickFixes();

    IntentionAction switchInstalled = fixes.stream()
        .filter(f -> f.getText().contains("Switch project compiler to Compact v0.34.0 (Language v0.26.0)"))
        .findFirst()
        .orElse(null);
    assertNotNull("Should suggest switching to installed higher version 0.34.0 (Language 0.26.0)", switchInstalled);

    IntentionAction downloadFix = fixes.stream()
        .filter(f -> f.getText().contains("Download and switch project compiler to Compact v0.28.0"))
        .findFirst()
        .orElse(null);
    assertNotNull("Should offer downloading exact required version 0.20", downloadFix);

    int installedIdx = fixes.indexOf(switchInstalled);
    int downloadIdx = fixes.indexOf(downloadFix);
    assertTrue("Installed satisfying version should be offered before download option", installedIdx < downloadIdx);

    // Applying the quick-fix switches project compiler immediately
    myFixture.launchAction(switchInstalled);
    assertEquals("0.34.0", settings.selectedCompilerVersion);
  }

  @SuppressWarnings("unchecked")
  public void testInstalledHigherVersionNotSuggestedWhenLockedConstraint() {
    MidnightProjectSettings settings = MidnightProjectSettings.getInstance(getProject());
    settings.selectedCompilerVersion = "0.26.0"; // Language 0.18.0
    settings.customCompilerPath = "";

    // Mock installed versions: 0.34.0 (Language 0.26.0) installed, but constraint is ^0.20
    SequencedMap<String, String> installed = new TreeMap<>(Comparator.reverseOrder());
    installed.put("0.34.0", "/mock/bin/0.34.0");
    installed.put("0.26.0", "/mock/bin/0.26.0");
    CompactVersionManager.setInstalledVersionsForTesting(installed);

    String code = "pragma <caret>language_version ^0.20;\n";
    myFixture.enableInspections(CompactPragmaVersionInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    myFixture.doHighlighting();

    List<IntentionAction> fixes = myFixture.getAllQuickFixes();

    boolean hasInstalled034 = fixes.stream()
        .anyMatch(f -> f.getText().contains("0.34.0") || f.getText().contains("0.26.0"));
    assertFalse("Higher version 0.26.0 should not be suggested for incompatible ^0.20 constraint", hasInstalled034);

    boolean hasDownload028 = fixes.stream()
        .anyMatch(f -> f.getText().contains("Download and switch project compiler to Compact v0.28.0"));
    assertTrue("Should offer download for exact required version under ^0.20", hasDownload028);
  }

  @SuppressWarnings("unchecked")
  public void testMultipleInstalledSatisfyingVersionsSortedDescending() {
    MidnightProjectSettings settings = MidnightProjectSettings.getInstance(getProject());
    settings.selectedCompilerVersion = "0.26.0"; // Language 0.18.0
    settings.customCompilerPath = "";

    // Multiple higher satisfying versions installed: 0.34.0 (lang 0.26.0) and 0.31.1 (lang 0.23.0)
    SequencedMap<String, String> installed = new TreeMap<>(Comparator.reverseOrder());
    installed.put("0.34.0", "/mock/bin/0.34.0");
    installed.put("0.31.1", "/mock/bin/0.31.1");
    installed.put("0.26.0", "/mock/bin/0.26.0");
    CompactVersionManager.setInstalledVersionsForTesting(installed);

    String code = "pragma <caret>language_version >= 0.20;\n";
    myFixture.enableInspections(CompactPragmaVersionInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    myFixture.doHighlighting();

    List<IntentionAction> fixes = myFixture.getAllQuickFixes();

    IntentionAction fix034 = fixes.stream()
        .filter(f -> f.getText().contains("v0.34.0"))
        .findFirst()
        .orElse(null);
    IntentionAction fix031 = fixes.stream()
        .filter(f -> f.getText().contains("v0.31.1"))
        .findFirst()
        .orElse(null);
    IntentionAction fixDownload = fixes.stream()
        .filter(f -> f.getText().contains("Download and switch project compiler"))
        .findFirst()
        .orElse(null);

    assertNotNull("Should offer 0.34.0 fix", fix034);
    assertNotNull("Should offer 0.31.1 fix", fix031);
    assertNotNull("Should offer download fix", fixDownload);

    int idx034 = fixes.indexOf(fix034);
    int idx031 = fixes.indexOf(fix031);
    int idxDownload = fixes.indexOf(fixDownload);

    assertTrue("0.34.0 should come before 0.31.1", idx034 < idx031);
    assertTrue("0.31.1 should come before download option", idx031 < idxDownload);
  }

  @SuppressWarnings("unchecked")
  public void testExactVersionInstalledNoDuplicateDownloadFix() {
    MidnightProjectSettings settings = MidnightProjectSettings.getInstance(getProject());
    settings.selectedCompilerVersion = "0.26.0"; // Language 0.18.0
    settings.customCompilerPath = "";

    // Mock installed versions: both higher 0.34.0 and exact required 0.28.0 (Language 0.20.0) are installed
    SequencedMap<String, String> installed = new TreeMap<>(Comparator.reverseOrder());
    installed.put("0.34.0", "/mock/bin/0.34.0");
    installed.put("0.28.0", "/mock/bin/0.28.0");
    installed.put("0.26.0", "/mock/bin/0.26.0");
    CompactVersionManager.setInstalledVersionsForTesting(installed);

    String code = "pragma <caret>language_version >= 0.20;\n";
    myFixture.enableInspections(CompactPragmaVersionInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    myFixture.doHighlighting();

    List<IntentionAction> fixes = myFixture.getAllQuickFixes();

    boolean hasInstalled034 = fixes.stream()
        .anyMatch(f -> f.getText().contains("Switch project compiler to Compact v0.34.0"));
    boolean hasInstalled028 = fixes.stream()
        .anyMatch(f -> f.getText().contains("Switch project compiler to Compact v0.28.0"));
    boolean hasDownloadFix = fixes.stream()
        .anyMatch(f -> f.getText().contains("Download and switch"));

    assertTrue("Should offer installed 0.34.0", hasInstalled034);
    assertTrue("Should offer installed 0.28.0", hasInstalled028);
    assertFalse("Should NOT offer download fix since 0.28.0 is already installed", hasDownloadFix);
  }

  @SuppressWarnings("unchecked")
  public void testInstalledCompilerPragmaConstraint() {
    MidnightProjectSettings settings = MidnightProjectSettings.getInstance(getProject());
    settings.selectedCompilerVersion = "0.26.0";
    settings.customCompilerPath = "";

    SequencedMap<String, String> installed = new TreeMap<>(Comparator.reverseOrder());
    installed.put("0.34.0", "/mock/bin/0.34.0");
    installed.put("0.26.0", "/mock/bin/0.26.0");
    CompactVersionManager.setInstalledVersionsForTesting(installed);

    String code = "pragma <caret>compiler_version >= 0.30;\n";
    myFixture.enableInspections(CompactPragmaVersionInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    myFixture.doHighlighting();

    List<IntentionAction> fixes = myFixture.getAllQuickFixes();

    boolean hasSwitch034 = fixes.stream()
        .anyMatch(f -> f.getText().contains("Switch project compiler to Compact 0.34.0"));
    assertTrue("Should offer switching to installed compiler 0.34.0 for compiler_version pragma", hasSwitch034);
  }
}
