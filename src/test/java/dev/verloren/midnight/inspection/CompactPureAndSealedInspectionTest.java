package dev.verloren.midnight.inspection;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.intention.IntentionAction;
import dev.verloren.midnight.CompactFileType;

import java.util.List;

public class CompactPureAndSealedInspectionTest extends CompactInspectionTestBase {

  // =========================================================================
  // Pure Circuit Inspection Tests
  // =========================================================================

  public void testPureCircuitValidMathAllowed() {
    String code = """
            pure circuit add(x: Field, y: Field): Field {
              return x + y;
            }
            """;
    myFixture.enableInspections(CompactPureCircuitInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> warnings = filterInspectionWarnings(myFixture.doHighlighting());
    assertTrue("Pure circuit with valid math should have zero warnings: " + warnings, warnings.isEmpty());
  }

  public void testPureCircuitCallingWitnessFails() {
    String code = """
            witness secretKey(): Field;
            
            pure circuit deriveKey(): Field {
              return secretKey();
            }
            """;
    myFixture.enableInspections(CompactPureCircuitInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> warnings = filterInspectionWarnings(myFixture.doHighlighting());
    List<HighlightInfo> matched = warnings.stream()
            .filter(h -> h.getDescription() != null && h.getDescription().contains("cannot invoke witness 'secretKey'"))
            .toList();
    assertEquals("Pure circuit calling witness should be flagged", 1, matched.size());
  }

  public void testPureCircuitAccessingLedgerFails() {
    String code = """
            ledger count: Field;
            
            pure circuit getCount(): Field {
              return count;
            }
            """;
    myFixture.enableInspections(CompactPureCircuitInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> warnings = filterInspectionWarnings(myFixture.doHighlighting());
    List<HighlightInfo> matched = warnings.stream()
            .filter(h -> h.getDescription() != null && h.getDescription().contains("cannot access ledger state 'count'"))
            .toList();
    assertEquals("Pure circuit reading ledger state should be flagged", 1, matched.size());
  }

  public void testPureCircuitEmittingEventFails() {
    String code = """
            pure circuit trigger(): [] {
              emit(1);
            }
            """;
    myFixture.enableInspections(CompactPureCircuitInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> warnings = filterInspectionWarnings(myFixture.doHighlighting());
    List<HighlightInfo> matched = warnings.stream()
            .filter(h -> h.getDescription() != null && h.getDescription().contains("cannot emit events"))
            .toList();
    assertEquals("Pure circuit emitting events should be flagged", 1, matched.size());
  }

  public void testPureCircuitCallingImpureCircuitFails() {
    String code = """
            circuit impureHelper(): Field {
              return 1;
            }
            
            pure circuit compute(): Field {
              return impureHelper();
            }
            """;
    myFixture.enableInspections(CompactPureCircuitInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> warnings = filterInspectionWarnings(myFixture.doHighlighting());
    List<HighlightInfo> matched = warnings.stream()
            .filter(h -> h.getDescription() != null && h.getDescription().contains("cannot invoke non-pure circuit 'impureHelper'"))
            .toList();
    assertEquals("Pure circuit calling non-pure circuit should be flagged", 1, matched.size());
  }

  public void testPureCircuitRemoveModifierQuickFix() {
    String code = """
            witness secretKey(): Field;
            
            pure circuit deriveKey(): Field {
              return <caret>secretKey();
            }
            """;
    myFixture.enableInspections(CompactPureCircuitInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    myFixture.doHighlighting();
    List<IntentionAction> fixes = myFixture.getAllQuickFixes();
    IntentionAction fix = fixes.stream()
            .filter(f -> f.getText().contains("Remove 'pure' modifier"))
            .findFirst()
            .orElse(null);
    assertNotNull("Remove 'pure' modifier quick-fix should be available", fix);
    myFixture.launchAction(fix);
    assertFalse("Code should no longer contain 'pure'", myFixture.getFile().getText().contains("pure"));
  }

  // =========================================================================
  // Sealed Field Mutation Inspection Tests
  // =========================================================================

  public void testSealedFieldMutationInConstructorAllowed() {
    String code = """
            sealed ledger owner: Field;
            
            constructor(initialOwner: Field) {
              owner = initialOwner;
            }
            """;
    myFixture.enableInspections(CompactSealedFieldMutationInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> warnings = filterInspectionWarnings(myFixture.doHighlighting());
    assertTrue("Mutating sealed field in constructor should be allowed: " + warnings, warnings.isEmpty());
  }

  public void testSealedFieldMutationOutsideConstructorFails() {
    String code = """
            sealed ledger owner: Field;
            
            circuit transfer(newOwner: Field): [] {
              owner = newOwner;
            }
            """;
    myFixture.enableInspections(CompactSealedFieldMutationInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> warnings = filterInspectionWarnings(myFixture.doHighlighting());
    List<HighlightInfo> matched = warnings.stream()
            .filter(h -> h.getDescription() != null && h.getDescription().contains("Cannot modify sealed ledger field 'owner' outside constructor"))
            .toList();
    assertEquals("Mutating sealed field outside constructor should be flagged", 1, matched.size());
  }

  public void testUnsealedFieldMutationAllowed() {
    String code = """
            ledger round: Field;
            
            circuit step(): [] {
              round = 2;
            }
            """;
    myFixture.enableInspections(CompactSealedFieldMutationInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> warnings = filterInspectionWarnings(myFixture.doHighlighting());
    assertTrue("Mutating unsealed field should produce no sealed field warnings", warnings.isEmpty());
  }

  public void testSealedFieldMutationInsideModuleAllowed() {
    String code = """
            module ShieldedAccessControl {
              export sealed ledger _instanceSalt: Bytes<32>;

              export circuit initialize(instanceSalt: Bytes<32>): [] {
                _instanceSalt = instanceSalt;
              }
            }
            """;
    myFixture.enableInspections(CompactSealedFieldMutationInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> warnings = filterInspectionWarnings(myFixture.doHighlighting());
    assertTrue("Mutating sealed field inside module should produce no sealed field warnings: " + warnings, warnings.isEmpty());
  }
}
