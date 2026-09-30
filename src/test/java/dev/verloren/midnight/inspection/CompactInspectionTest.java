package dev.verloren.midnight.inspection;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import dev.verloren.midnight.CompactFileType;

import java.util.List;

public class CompactInspectionTest extends CompactInspectionTestBase {

  public void testNoFalsePositiveValidCode() {
    String code = """
            struct Point { x: Field; y: Field; }
            circuit test(p: Point): [] {
              const a = p.x;
              const b = a;
            }
            """;
    myFixture.enableInspections(CompactUnresolvedReferenceInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> highlights = myFixture.doHighlighting();
    List<HighlightInfo> warnings = filterInspectionWarnings(highlights);
    assertTrue("Valid code should produce no unresolved reference warnings: " + warnings, warnings.isEmpty());
  }

  public void testUnresolvedLocalVariable() {
    String code = """
            circuit test(): [] {
              const x = nonExistentVar;
            }
            """;
    myFixture.enableInspections(CompactUnresolvedReferenceInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> highlights = myFixture.doHighlighting();
    List<HighlightInfo> unresolved = highlights.stream()
            .filter(h -> h.getDescription() != null && h.getDescription().contains("Unresolved reference 'nonExistentVar'"))
            .toList();
    assertEquals("Should report 1 unresolved reference for nonExistentVar", 1, unresolved.size());
  }

  public void testUnresolvedTypeReferenceNotFlagged() {
    String code = """
            circuit test(a: SomeExternalType): [] {}
            """;
    myFixture.enableInspections(CompactUnresolvedReferenceInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> highlights = myFixture.doHighlighting();
    List<HighlightInfo> warnings = filterInspectionWarnings(highlights);
    assertTrue("External/unresolved type references should be soft-unresolved and not produce inspection warnings", warnings.isEmpty());
  }

  public void testResolvedConstReference() {
    String code = """
            circuit test(): [] {
              const a = 1;
              const b = a;
            }
            """;
    myFixture.enableInspections(CompactUnresolvedReferenceInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> warnings = filterInspectionWarnings(myFixture.doHighlighting());
    assertTrue("Resolved const reference should produce no warnings", warnings.isEmpty());
  }

  public void testResolvedTypeReference() {
    String code = """
            struct Data { val: Field; }
            circuit test(d: Data): [] {}
            """;
    myFixture.enableInspections(CompactUnresolvedReferenceInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> warnings = filterInspectionWarnings(myFixture.doHighlighting());
    assertTrue("Resolved struct type reference should produce no warnings", warnings.isEmpty());
  }

  public void testResolvedEnumMemberReference() {
    String code = """
            enum Color { Red, Green, Blue }
            circuit test(): [] {
              const c = Color.Green;
            }
            """;
    myFixture.enableInspections(CompactUnresolvedReferenceInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> warnings = filterInspectionWarnings(myFixture.doHighlighting());
    assertTrue("Resolved enum member access should produce no warnings", warnings.isEmpty());
  }

  public void testUnresolvedEnumMemberReference() {
    String code = """
            enum Color { Red, Green, Blue }
            circuit test(): [] {
              const c = Color.Yellow;
            }
            """;
    myFixture.enableInspections(CompactUnresolvedReferenceInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> unresolved = myFixture.doHighlighting().stream()
            .filter(h -> h.getDescription() != null && h.getDescription().contains("Unresolved enum member 'Yellow'"))
            .toList();
    assertEquals("Should report 1 unresolved enum member warning", 1, unresolved.size());
  }

  public void testBuiltinTypeNotFlagged() {
    String code = """
            circuit test(a: Boolean, b: Uint<32>, c: Field, d: Bytes<32>): [] {}
            """;
    myFixture.enableInspections(CompactUnresolvedReferenceInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> warnings = filterInspectionWarnings(myFixture.doHighlighting());
    assertTrue("Builtin types should not produce unresolved warnings", warnings.isEmpty());
  }

  public void testResolvedStructFieldAccess() {
    String code = """
            struct Point { x: Field; y: Field; }
            circuit test(p: Point): [] {
              const v = p.x;
            }
            """;
    myFixture.enableInspections(CompactUnresolvedReferenceInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> warnings = filterInspectionWarnings(myFixture.doHighlighting());
    assertTrue("Resolved struct field access should produce no warnings", warnings.isEmpty());
  }

  public void testUnresolvedStructFieldAccess() {
    String code = """
            struct Point { x: Field; y: Field; }
            circuit test(p: Point): [] {
              const v = p.z;
            }
            """;
    myFixture.enableInspections(CompactUnresolvedReferenceInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> unresolved = myFixture.doHighlighting().stream()
            .filter(h -> h.getDescription() != null && h.getDescription().contains("Unresolved struct field 'z'"))
            .toList();
    assertEquals("Should report 1 unresolved struct field warning", 1, unresolved.size());
  }

  public void testIncompleteCodeNoCrash() {
    String code = "circuit test(): [] { const x = ; }";
    myFixture.enableInspections(CompactUnresolvedReferenceInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    assertNotNull(myFixture.doHighlighting());
  }

  public void testMissingBracesNoCrash() {
    String code = "circuit test( { const x = 1";
    myFixture.enableInspections(CompactUnresolvedReferenceInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    assertNotNull(myFixture.doHighlighting());
  }

  public void testNestedScopeResolution() {
    String code = """
            circuit test(): [] {
              const a = 1;
              {
                const b = a;
              }
            }
            """;
    myFixture.enableInspections(CompactUnresolvedReferenceInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> warnings = filterInspectionWarnings(myFixture.doHighlighting());
    assertTrue("Nested block referencing outer const should produce no warnings", warnings.isEmpty());
  }

  public void testTopLevelLedgerForwardReferenceNoUnresolvedWarning() {
    String code = """
            export circuit clear(): [] {
              round.increment(1);
            }
            
            circuit publicKey(round: Field, sk: Bytes<32>): Field {
              return round;
            }
            
            constructor(sk: Bytes<32>, v: Uint<64>) {
              authority = disclose(publicKey(round, sk));
            }
            
            export ledger round: Counter;
            """;
    myFixture.enableInspections(CompactUnresolvedReferenceInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> warnings = filterInspectionWarnings(myFixture.doHighlighting());
    List<HighlightInfo> roundUnresolved = warnings.stream()
            .filter(h -> h.getDescription() != null && h.getDescription().contains("round"))
            .toList();
    if (!roundUnresolved.isEmpty()) {
      StringBuilder sb = new StringBuilder("Found unresolved references:");
      for (HighlightInfo h : roundUnresolved) {
        sb.append("\n  - ").append(h.getDescription()).append(" at [").append(h.getStartOffset()).append(", ").append(h.getEndOffset()).append("]");
      }
      fail(sb.toString());
    }
    assertTrue("Forward-referenced top-level ledger 'round' should not produce unresolved reference warnings", true);
  }
}
