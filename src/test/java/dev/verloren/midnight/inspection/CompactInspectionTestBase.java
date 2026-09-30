package dev.verloren.midnight.inspection;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.lang.LanguageParserDefinitions;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import dev.verloren.midnight.CompactLanguage;
import dev.verloren.midnight.parser.CompactParserDefinition;

import java.util.List;

public abstract class CompactInspectionTestBase extends BasePlatformTestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    LanguageParserDefinitions.INSTANCE.addExplicitExtension(
        CompactLanguage.INSTANCE,
        new CompactParserDefinition()
    );
  }

  protected void enableAllInspections() {
    myFixture.enableInspections(
        CompactUnresolvedReferenceInspection.class,
        CompactDuplicateDeclarationInspection.class,
        CompactUnusedLocalVariableInspection.class,
        CompactTypeMismatchInspection.class,
        CompactPureCircuitInspection.class,
        CompactSealedFieldMutationInspection.class,
        CompactRecursiveCircuitInspection.class,
        CompactConstructorRestrictionInspection.class,
        CompactUndisclosedWitnessInspection.class
    );
  }

  protected List<HighlightInfo> filterInspectionWarnings(List<HighlightInfo> highlights) {
    return highlights.stream()
        .filter(h -> h.getSeverity() == HighlightSeverity.WARNING || h.getSeverity() == HighlightSeverity.WEAK_WARNING)
        .toList();
  }
}
