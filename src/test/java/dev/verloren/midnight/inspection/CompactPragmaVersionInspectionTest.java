package dev.verloren.midnight.inspection;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.lang.LanguageParserDefinitions;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import dev.verloren.midnight.CompactFileType;
import dev.verloren.midnight.CompactLanguage;
import dev.verloren.midnight.parser.CompactParserDefinition;
import dev.verloren.midnight.settings.MidnightProjectSettings;

import java.util.List;

public class CompactPragmaVersionInspectionTest extends BasePlatformTestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    LanguageParserDefinitions.INSTANCE.addExplicitExtension(
        CompactLanguage.INSTANCE,
        new CompactParserDefinition()
    );
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
}
