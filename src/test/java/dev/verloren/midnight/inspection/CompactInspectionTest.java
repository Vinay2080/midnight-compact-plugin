package dev.verloren.midnight.inspection;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.lang.LanguageParserDefinitions;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import dev.verloren.midnight.CompactFileType;
import dev.verloren.midnight.CompactLanguage;
import dev.verloren.midnight.parser.CompactParserDefinition;

import java.util.List;

public class CompactInspectionTest extends BasePlatformTestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    LanguageParserDefinitions.INSTANCE.addExplicitExtension(
        CompactLanguage.INSTANCE,
        new CompactParserDefinition()
    );
  }

  private void enableAllInspections() {
    myFixture.enableInspections(
        CompactUnresolvedReferenceInspection.class,
        CompactDuplicateDeclarationInspection.class,
        CompactUnusedLocalVariableInspection.class,
        CompactTypeMismatchInspection.class
    );
  }

  private List<HighlightInfo> filterInspectionWarnings(List<HighlightInfo> highlights) {
    return highlights.stream()
        .filter(h -> h.getSeverity() == HighlightSeverity.WARNING || h.getSeverity() == HighlightSeverity.WEAK_WARNING)
        .toList();
  }

  // =========================================================================
  // 1. Unresolved Reference Inspection Tests
  // =========================================================================

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

  // =========================================================================
  // 2. Duplicate Declaration Inspection Tests
  // =========================================================================

  public void testNoDuplicateValidCode() {
    String code = """
        struct A {}
        struct B {}
        circuit test(): [] {
          const x = 1;
          const y = 2;
        }
        """;
    myFixture.enableInspections(CompactDuplicateDeclarationInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> warnings = filterInspectionWarnings(myFixture.doHighlighting());
    assertTrue("Valid code with unique names should produce no duplicate warnings", warnings.isEmpty());
  }

  public void testDuplicateConstInBlock() {
    String code = """
        circuit test(): [] {
          const x = 1;
          const x = 2;
        }
        """;
    myFixture.enableInspections(CompactDuplicateDeclarationInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> duplicates = myFixture.doHighlighting().stream()
        .filter(h -> h.getDescription() != null && h.getDescription().contains("Duplicate declaration 'x'"))
        .toList();
    assertEquals("Should report 1 duplicate declaration for 'x'", 1, duplicates.size());
  }

  public void testDuplicateCircuitTopLevel() {
    String code = """
        circuit foo(): [] {}
        circuit foo(): [] {}
        """;
    myFixture.enableInspections(CompactDuplicateDeclarationInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> duplicates = myFixture.doHighlighting().stream()
        .filter(h -> h.getDescription() != null && h.getDescription().contains("Duplicate declaration 'foo'"))
        .toList();
    assertEquals("Should report 1 duplicate declaration for top-level circuit 'foo'", 1, duplicates.size());
  }

  public void testDuplicateStruct() {
    String code = """
        struct S {}
        struct S {}
        """;
    myFixture.enableInspections(CompactDuplicateDeclarationInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> duplicates = myFixture.doHighlighting().stream()
        .filter(h -> h.getDescription() != null && h.getDescription().contains("Duplicate declaration 'S'"))
        .toList();
    assertEquals("Should report 1 duplicate declaration for struct 'S'", 1, duplicates.size());
  }

  public void testDuplicateEnum() {
    String code = """
        enum E { A }
        enum E { B }
        """;
    myFixture.enableInspections(CompactDuplicateDeclarationInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> duplicates = myFixture.doHighlighting().stream()
        .filter(h -> h.getDescription() != null && h.getDescription().contains("Duplicate declaration 'E'"))
        .toList();
    assertEquals("Should report 1 duplicate declaration for enum 'E'", 1, duplicates.size());
  }

  public void testDuplicateTypeAlias() {
    String code = """
        type A = Field;
        type A = Boolean;
        """;
    myFixture.enableInspections(CompactDuplicateDeclarationInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> duplicates = myFixture.doHighlighting().stream()
        .filter(h -> h.getDescription() != null && h.getDescription().contains("Duplicate declaration 'A'"))
        .toList();
    assertEquals("Should report 1 duplicate declaration for type alias 'A'", 1, duplicates.size());
  }

  public void testShadowingIsNotDuplicate() {
    String code = """
        circuit test(): [] {
          const x = 1;
          {
            const x = 2;
          }
        }
        """;
    myFixture.enableInspections(CompactDuplicateDeclarationInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> warnings = filterInspectionWarnings(myFixture.doHighlighting());
    assertTrue("Shadowing in nested block is not a duplicate declaration", warnings.isEmpty());
  }

  public void testSameNameDifferentNamespace() {
    String code = """
        struct Item { id: Field; }
        const Item = 42;
        """;
    myFixture.enableInspections(CompactDuplicateDeclarationInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> warnings = filterInspectionWarnings(myFixture.doHighlighting());
    assertTrue("Declarations with same name in different namespaces (TYPE vs VALUE) are not duplicates", warnings.isEmpty());
  }

  public void testDuplicateParameter() {
    String code = """
        circuit test(a: Field, a: Boolean): [] {}
        """;
    myFixture.enableInspections(CompactDuplicateDeclarationInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> duplicates = myFixture.doHighlighting().stream()
        .filter(h -> h.getDescription() != null && h.getDescription().contains("Duplicate declaration 'a'"))
        .toList();
    assertEquals("Should report 1 duplicate parameter warning for 'a'", 1, duplicates.size());
  }

  public void testDuplicateStructField() {
    String code = """
        struct Point { x: Field; x: Boolean; }
        """;
    myFixture.enableInspections(CompactDuplicateDeclarationInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> duplicates = myFixture.doHighlighting().stream()
        .filter(h -> h.getDescription() != null && h.getDescription().contains("Duplicate declaration 'x'"))
        .toList();
    assertEquals("Should report 1 duplicate struct field warning for 'x'", 1, duplicates.size());
  }

  public void testDuplicateEnumMember() {
    String code = """
        enum Color { Red, Green, Red }
        """;
    myFixture.enableInspections(CompactDuplicateDeclarationInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> duplicates = myFixture.doHighlighting().stream()
        .filter(h -> h.getDescription() != null && h.getDescription().contains("Duplicate declaration 'Red'"))
        .toList();
    assertEquals("Should report 1 duplicate enum member warning for 'Red'", 1, duplicates.size());
  }

  public void testIncompleteDeclarationNoCrash() {
    String code = "circuit test(): [] { const = ; const x = 1; }";
    myFixture.enableInspections(CompactDuplicateDeclarationInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    assertNotNull(myFixture.doHighlighting());
  }

  // =========================================================================
  // 3. Unused Local Variable Inspection Tests
  // =========================================================================

  public void testNoUnusedValidCode() {
    String code = """
        circuit test(): [] {
          const x = 1;
          const y = x;
        }
        """;
    myFixture.enableInspections(CompactUnusedLocalVariableInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    // Here y is unused, but x is used
    List<HighlightInfo> unused = myFixture.doHighlighting().stream()
        .filter(h -> h.getDescription() != null && h.getDescription().contains("Unused local variable 'x'"))
        .toList();
    assertTrue("Variable 'x' is used, should not be reported", unused.isEmpty());
  }

  public void testUnusedConstBinding() {
    String code = """
        circuit test(): [] {
          const unusedVar = 42;
        }
        """;
    myFixture.enableInspections(CompactUnusedLocalVariableInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> unused = myFixture.doHighlighting().stream()
        .filter(h -> h.getDescription() != null && h.getDescription().contains("Unused local variable 'unusedVar'"))
        .toList();
    assertEquals("Should report unused variable 'unusedVar'", 1, unused.size());
  }

  public void testTopLevelConstNotFlagged() {
    String code = """
        const GLOBAL_CONST = 100;
        """;
    myFixture.enableInspections(CompactUnusedLocalVariableInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> unused = myFixture.doHighlighting().stream()
        .filter(h -> h.getDescription() != null && h.getDescription().contains("Unused local variable"))
        .toList();
    assertTrue("Top-level consts should not be reported as unused local variables", unused.isEmpty());
  }

  public void testParameterNotFlagged() {
    String code = """
        circuit test(param: Field): [] {}
        """;
    myFixture.enableInspections(CompactUnusedLocalVariableInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> unused = myFixture.doHighlighting().stream()
        .filter(h -> h.getDescription() != null && h.getDescription().contains("Unused local variable"))
        .toList();
    assertTrue("Parameters should not be reported as unused local variables", unused.isEmpty());
  }

  public void testStructFieldNotFlagged() {
    String code = """
        struct Point { x: Field; y: Field; }
        """;
    myFixture.enableInspections(CompactUnusedLocalVariableInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> unused = myFixture.doHighlighting().stream()
        .filter(h -> h.getDescription() != null && h.getDescription().contains("Unused local variable"))
        .toList();
    assertTrue("Struct fields should not be reported as unused local variables", unused.isEmpty());
  }

  public void testUnderscorePrefixedVariableNotFlagged() {
    String code = """
        circuit test(): [] {
          const _ignored = 42;
        }
        """;
    myFixture.enableInspections(CompactUnusedLocalVariableInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> unused = myFixture.doHighlighting().stream()
        .filter(h -> h.getDescription() != null && h.getDescription().contains("Unused local variable"))
        .toList();
    assertTrue("Underscore-prefixed variables should be ignored", unused.isEmpty());
  }

  public void testIncompleteCodeNoCrashUnused() {
    String code = "circuit test(): [] { const = ; }";
    myFixture.enableInspections(CompactUnusedLocalVariableInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    assertNotNull(myFixture.doHighlighting());
  }

  public void testQuickFixRemovesUnusedVariable() {
    String code = """
        circuit test(): [] {
          const <caret>unusedVar = 42;
          const y = 10;
        }
        """;
    myFixture.enableInspections(CompactUnusedLocalVariableInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    myFixture.doHighlighting();

    IntentionAction fix = myFixture.findSingleIntention("Remove unused variable 'unusedVar'");
    assertNotNull("QuickFix should be available", fix);
    myFixture.launchAction(fix);

    String result = myFixture.getFile().getText();
    assertFalse("Variable 'unusedVar' should have been removed", result.contains("unusedVar"));
    assertTrue("Other statements should remain", result.contains("const y = 10;"));
  }

  // =========================================================================
  // 4. Type Mismatch Inspection Tests
  // =========================================================================

  public void testNoMismatchValidLogical() {
    String code = """
        circuit test(): [] {
          const x = true && false;
        }
        """;
    myFixture.enableInspections(CompactTypeMismatchInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> warnings = filterInspectionWarnings(myFixture.doHighlighting());
    assertTrue("Valid logical expression should have no type mismatch warnings", warnings.isEmpty());
  }

  public void testLogicalAndWithNonBoolean() {
    String code = """
        circuit test(): [] {
          const x = 1 && true;
        }
        """;
    myFixture.enableInspections(CompactTypeMismatchInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> mismatches = myFixture.doHighlighting().stream()
        .filter(h -> h.getDescription() != null && h.getDescription().contains("Boolean expected, got 'Field'"))
        .toList();
    assertEquals("Should report 1 Boolean expected warning for '1'", 1, mismatches.size());
  }

  public void testLogicalOrWithNonBoolean() {
    String code = """
        circuit test(): [] {
          const x = true || 42;
        }
        """;
    myFixture.enableInspections(CompactTypeMismatchInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> mismatches = myFixture.doHighlighting().stream()
        .filter(h -> h.getDescription() != null && h.getDescription().contains("Boolean expected, got 'Field'"))
        .toList();
    assertEquals("Should report 1 Boolean expected warning for '42'", 1, mismatches.size());
  }

  public void testNegationOfNonBoolean() {
    String code = """
        circuit test(): [] {
          const x = !42;
        }
        """;
    myFixture.enableInspections(CompactTypeMismatchInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> mismatches = myFixture.doHighlighting().stream()
        .filter(h -> h.getDescription() != null && h.getDescription().contains("Boolean expected, got 'Field'"))
        .toList();
    assertEquals("Should report 1 Boolean expected warning for '!42'", 1, mismatches.size());
  }

  public void testEqualityTypeMismatch() {
    String code = """
        circuit test(): [] {
          const x = true == 1;
        }
        """;
    myFixture.enableInspections(CompactTypeMismatchInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> mismatches = myFixture.doHighlighting().stream()
        .filter(h -> h.getDescription() != null && h.getDescription().contains("Cannot compare 'Boolean' with 'Field'"))
        .toList();
    assertEquals("Should report 1 cannot compare warning for 'true == 1'", 1, mismatches.size());
  }

  public void testEqualityTypesMatch() {
    String code = """
        circuit test(): [] {
          const x = 1 == 2;
        }
        """;
    myFixture.enableInspections(CompactTypeMismatchInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> warnings = filterInspectionWarnings(myFixture.doHighlighting());
    assertTrue("Matching types in equality comparison should produce no warnings", warnings.isEmpty());
  }

  public void testUnknownTypeNotFlagged() {
    String code = """
        circuit test(): [] {
          const x = unknownVar && true;
        }
        """;
    myFixture.enableInspections(CompactTypeMismatchInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> warnings = filterInspectionWarnings(myFixture.doHighlighting());
    assertTrue("Unknown types should not produce type mismatch false positives", warnings.isEmpty());
  }

  public void testArithmeticNotFlagged() {
    String code = """
        circuit test(): [] {
          const x = 1 + 2;
        }
        """;
    myFixture.enableInspections(CompactTypeMismatchInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> warnings = filterInspectionWarnings(myFixture.doHighlighting());
    assertTrue("Valid arithmetic operations should not produce type warnings", warnings.isEmpty());
  }

  public void testIncompleteExprNoCrash() {
    String code = "circuit test(): [] { const x = && ; }";
    myFixture.enableInspections(CompactTypeMismatchInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    assertNotNull(myFixture.doHighlighting());
  }

  // =========================================================================
  // 5. Cross-Cutting & Malformed Code Tests
  // =========================================================================

  public void testCompleteValidContractNoWarnings() {
    String code = """
        struct Point {
          x: Field;
          y: Field;
        }

        enum Status {
          Active,
          Inactive
        }

        circuit calculate(p: Point, s: Status): [] {
          const xVal = p.x;
          const isMatch = s == Status.Active;
          const combined = isMatch && (xVal == 0);
        }
        """;
    enableAllInspections();
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> highlights = myFixture.doHighlighting();
    List<HighlightInfo> criticalWarnings = highlights.stream()
        .filter(h -> h.getDescription() != null && (
            h.getDescription().contains("Unresolved")
            || h.getDescription().contains("Duplicate")
            || h.getDescription().contains("Cannot compare")
            || h.getDescription().contains("Boolean expected")
        ))
        .toList();
    assertTrue("Complete valid contract should have zero critical semantic errors: " + criticalWarnings, criticalWarnings.isEmpty());
  }

  public void testSevereMalformedCodeNoCrash() {
    String code = "circuit { struct } const enum && = ;;; !!!";
    enableAllInspections();
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    assertNotNull(myFixture.doHighlighting());
  }
}
