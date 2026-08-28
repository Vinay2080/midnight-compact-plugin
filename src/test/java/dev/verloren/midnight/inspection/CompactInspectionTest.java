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

@SuppressWarnings("unchecked")
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
        CompactTypeMismatchInspection.class,
        CompactPureCircuitInspection.class,
        CompactSealedFieldMutationInspection.class,
        CompactRecursiveCircuitInspection.class,
        CompactConstructorRestrictionInspection.class,
        CompactUndisclosedWitnessInspection.class
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

  public void testSameParamNameAcrossDifferentCircuits() {
    String code = """
        circuit foo(x: Field, amount: Uint<64>): Void {
        }
        circuit bar(x: Field, amount: Uint<64>): Void {
        }
        """;
    myFixture.enableInspections(CompactDuplicateDeclarationInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> warnings = filterInspectionWarnings(myFixture.doHighlighting());
    assertTrue("Parameters with same name across different circuits should not produce duplicate warnings", warnings.isEmpty());
  }

  public void testSameParamNameAcrossDifferentWitnesses() {
    String code = """
        witness getSecretA(id: Field): Boolean;
        witness getSecretB(id: Field): Boolean;
        """;
    myFixture.enableInspections(CompactDuplicateDeclarationInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> warnings = filterInspectionWarnings(myFixture.doHighlighting());
    assertTrue("Parameters with same name across different witnesses should not produce duplicate warnings", warnings.isEmpty());
  }

  public void testSameVarNameInSiblingBlocks() {
    String code = """
        circuit test(c: Boolean): Void {
          if (c) {
            const x = 1;
          } else {
            const x = 2;
          }
        }
        """;
    myFixture.enableInspections(CompactDuplicateDeclarationInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> warnings = filterInspectionWarnings(myFixture.doHighlighting());
    assertTrue("Variables with same name in sibling if/else blocks should not produce duplicate warnings", warnings.isEmpty());
  }

  public void testTopLevelConstAndParamSameName() {
    String code = """
        const x: Field = 42;
        circuit test(x: Field): Void {
        }
        """;
    myFixture.enableInspections(CompactDuplicateDeclarationInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> warnings = filterInspectionWarnings(myFixture.doHighlighting());
    assertTrue("Top-level const and circuit parameter with same name should not produce duplicate warnings", warnings.isEmpty());
  }

  public void testParamAndLocalShadowing() {
    String code = """
        circuit test(x: Field): Void {
          const x = 1;
        }
        """;
    myFixture.enableInspections(CompactDuplicateDeclarationInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> warnings = filterInspectionWarnings(myFixture.doHighlighting());
    assertTrue("Local variable in block shadowing circuit parameter should not produce duplicate warnings", warnings.isEmpty());
  }

  public void testSameFieldNameAcrossDifferentStructs() {
    String code = """
        struct Point {
          x: Field,
          y: Field
        }
        struct Vector {
          x: Field,
          y: Field
        }
        """;
    myFixture.enableInspections(CompactDuplicateDeclarationInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> warnings = filterInspectionWarnings(myFixture.doHighlighting());
    assertTrue("Fields with same name across different structs should not produce duplicate warnings", warnings.isEmpty());
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

  public void testIfConditionNonBoolean() {
    String code = """
        circuit test(): [] {
          if (42) {
            const x = 1;
          }
        }
        """;
    myFixture.enableInspections(CompactTypeMismatchInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> mismatches = myFixture.doHighlighting().stream()
        .filter(h -> h.getDescription() != null && h.getDescription().contains("Boolean expected in 'if' condition, got 'Field'"))
        .toList();
    assertEquals("Should report 1 warning for non-boolean condition", 1, mismatches.size());
  }

  public void testIfConditionBoolean() {
    String code = """
        circuit test(flag: Boolean): [] {
          if (flag) {
            const x = 1;
          }
        }
        """;
    myFixture.enableInspections(CompactTypeMismatchInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> warnings = filterInspectionWarnings(myFixture.doHighlighting());
    assertTrue("Boolean condition in 'if' should have no warnings", warnings.isEmpty());
  }

  public void testConstDeclarationTypeMismatch() {
    String code = """
        circuit test(): [] {
          const x: Boolean = 42;
        }
        """;
    myFixture.enableInspections(CompactTypeMismatchInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> mismatches = myFixture.doHighlighting().stream()
        .filter(h -> h.getDescription() != null && h.getDescription().contains("Type mismatch: expected 'Boolean', got 'Field'"))
        .toList();
    assertEquals("Should report 1 type mismatch warning for const initializer", 1, mismatches.size());
  }

  public void testConstDeclarationTypeMatch() {
    String code = """
        circuit test(): [] {
          const x: Field = 42;
        }
        """;
    myFixture.enableInspections(CompactTypeMismatchInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> warnings = filterInspectionWarnings(myFixture.doHighlighting());
    assertTrue("Matching const declaration type should have no warnings", warnings.isEmpty());
  }

  public void testRelationalComparisonWithBoolean() {
    String code = """
        circuit test(): [] {
          const x = true < false;
        }
        """;
    myFixture.enableInspections(CompactTypeMismatchInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> mismatches = myFixture.doHighlighting().stream()
        .filter(h -> h.getDescription() != null && h.getDescription().contains("Relational operator not applicable to 'Boolean'"))
        .toList();
    assertEquals("Should report warning for relational comparison with boolean", 2, mismatches.size());
  }

  public void testArithmeticWithBoolean() {
    String code = """
        circuit test(): [] {
          const x = true + 1;
        }
        """;
    myFixture.enableInspections(CompactTypeMismatchInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> mismatches = myFixture.doHighlighting().stream()
        .filter(h -> h.getDescription() != null && h.getDescription().contains("Arithmetic operator not applicable to 'Boolean'"))
        .toList();
    assertEquals("Should report warning for arithmetic on boolean", 1, mismatches.size());
  }

  public void testUnaryMinusOnBoolean() {
    String code = """
        circuit test(): [] {
          const x = -true;
        }
        """;
    myFixture.enableInspections(CompactTypeMismatchInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> mismatches = myFixture.doHighlighting().stream()
        .filter(h -> h.getDescription() != null && h.getDescription().contains("Unary minus not applicable to 'Boolean'"))
        .toList();
    assertEquals("Should report warning for unary minus on boolean", 1, mismatches.size());
  }

  public void testUint8ComparisonWithIntegerLiterals() {
    String code = """
        export circuit player2Shoot(x: Uint<8>): [] {
          assert(x > 0 && x <= 20, "Shot out of bounds");
        }
        """;
    myFixture.enableInspections(CompactTypeMismatchInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> warnings = filterInspectionWarnings(myFixture.doHighlighting());
    assertTrue("Comparing Uint<8> with literals 0 and 20 should produce no warnings: " + warnings, warnings.isEmpty());
  }

  public void testUint8BoundaryValues() {
    String code = """
        circuit testBounds(x: Uint<8>): [] {
          const minBound = x >= 0;
          const maxBound = x <= 255;
          const belowMax = x < 256;
          const leftZero = 0 <= x;
          const leftMax = 255 >= x;
        }
        """;
    myFixture.enableInspections(CompactTypeMismatchInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> warnings = filterInspectionWarnings(myFixture.doHighlighting());
    assertTrue("Uint<8> boundary comparisons should produce no warnings", warnings.isEmpty());
  }

  public void testUintConstInitializationBounds() {
    String code = """
        circuit testConstInit(): [] {
          const valid1: Uint<8> = 0;
          const valid2: Uint<8> = 20;
          const valid3: Uint<8> = 255;
          const valid4: Uint<16> = 65535;
          const valid5: Uint<32> = 4294967295;
        }
        """;
    myFixture.enableInspections(CompactTypeMismatchInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> warnings = filterInspectionWarnings(myFixture.doHighlighting());
    assertTrue("In-bounds const initialization for Uint types should produce no warnings", warnings.isEmpty());
  }

  public void testUintConstInitializationOutOfBounds() {
    String code = """
        circuit testConstInitOOB(): [] {
          const oob1: Uint<8> = 256;
          const oob2: Uint<8> = 300;
          const oob3: Uint<16> = 65536;
        }
        """;
    myFixture.enableInspections(CompactTypeMismatchInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> mismatches = myFixture.doHighlighting().stream()
        .filter(h -> h.getDescription() != null && h.getDescription().contains("Type mismatch"))
        .toList();
    assertEquals("Should report 3 type mismatch warnings for out-of-bounds Uint initializers", 3, mismatches.size());
  }

  public void testUintOtherBitWidthsComparisons() {
    String code = """
        circuit testOtherWidths(a: Uint<16>, b: Uint<32>, c: Uint<64>, d: Uint): [] {
          const c1 = a > 0 && a <= 1000;
          const c2 = b >= 0 && b < 1000000;
          const c3 = c > 42;
          const c4 = d >= 10;
          const c5 = 0 < a && 100 >= b;
          const c6 = a < b;
        }
        """;
    myFixture.enableInspections(CompactTypeMismatchInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> warnings = filterInspectionWarnings(myFixture.doHighlighting());
    assertTrue("Comparisons across other Uint widths and literals should produce no warnings", warnings.isEmpty());
  }

  public void testUintArithmeticWithNumericLiterals() {
    String code = """
        circuit testArithmetic(x: Uint<8>, y: Uint<32>): [] {
          const a = x + 1;
          const b = 1 + x;
          const c = y - 10;
          const d = x * 2;
          const e = y / 4;
          const f = x % 5;
        }
        """;
    myFixture.enableInspections(CompactTypeMismatchInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> warnings = filterInspectionWarnings(myFixture.doHighlighting());
    assertTrue("Arithmetic between Uint and literals should produce no warnings", warnings.isEmpty());
  }

  public void testUintEqualityWithLiteralsAndOtherUints() {
    String code = """
        circuit testEquality(x: Uint<8>, y: Uint<16>, z: Uint<8>): [] {
          const e1 = x == 0;
          const e2 = x != 20;
          const e3 = 0 == x;
          const e4 = 20 != x;
          const e5 = x == z;
          const e6 = x == y;
        }
        """;
    myFixture.enableInspections(CompactTypeMismatchInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> warnings = filterInspectionWarnings(myFixture.doHighlighting());
    assertTrue("Equality comparisons for Uint should produce no warnings", warnings.isEmpty());
  }

  public void testUintIncompatibleComparisonsNegative() {
    String code = """
        circuit testIncompatible(x: Uint<8>, flag: Boolean, b: Bytes<32>, f: Field): [] {
          const bad1 = x == flag;
          const bad2 = x == b;
          const bad3 = x == f;
          const bad4 = x < b;
          const bad5 = x < f;
        }
        """;
    myFixture.enableInspections(CompactTypeMismatchInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> mismatches = myFixture.doHighlighting().stream()
        .filter(h -> h.getDescription() != null && h.getDescription().contains("Cannot compare"))
        .toList();
    assertEquals("Should report 5 cannot compare warnings for incompatible Uint comparisons", 5, mismatches.size());
  }

  public void testFieldIncompatibleRelationalNegative() {
    String code = """
        circuit testFieldRelational(f1: Field, f2: Field): [] {
          const bad1 = f1 < f2;
          const bad2 = f1 > 10;
        }
        """;
    myFixture.enableInspections(CompactTypeMismatchInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> mismatches = myFixture.doHighlighting().stream()
        .filter(h -> h.getDescription() != null && h.getDescription().contains("Cannot compare"))
        .toList();
    assertEquals("Should report 2 cannot compare warnings for Field relational comparisons", 2, mismatches.size());
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

  public void testCrossFileImportedEnumMemberValidNoWarnings() {
    myFixture.addFileToProject(
        "GameState.compact",
        """
        export enum GameState {
            WAITING,
            PLAYING,
            FINISHED,
        }
        """
    );
    String code = """
        import { GameState } from './GameState';

        export circuit checkGame(): [] {
            assert(
                GameState.PLAYING == GameState.PLAYING,
                "Game is not currently playing"
            );
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
        ))
        .toList();
    assertTrue("Cross-file imported enum comparison should produce zero warnings: " + criticalWarnings, criticalWarnings.isEmpty());
  }

  public void testCrossFileUnresolvedImportedSymbol() {
    myFixture.addFileToProject(
        "GameState.compact",
        """
        export enum GameState {
            WAITING,
            PLAYING,
            FINISHED,
        }
        """
    );
    String code = """
        import { NonExistentState } from './GameState';
        """;
    myFixture.enableInspections(CompactUnresolvedReferenceInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> unresolved = myFixture.doHighlighting().stream()
        .filter(h -> h.getDescription() != null && h.getDescription().contains("Unresolved imported symbol 'NonExistentState'"))
        .toList();
    assertEquals("Should report 1 unresolved imported symbol error", 1, unresolved.size());
  }

  public void testCrossFileUnresolvedEnumMember() {
    myFixture.addFileToProject(
        "GameState.compact",
        """
        export enum GameState {
            WAITING,
            PLAYING,
            FINISHED,
        }
        """
    );
    String code = """
        import { GameState } from './GameState';

        export circuit checkGame(): [] {
            const state = GameState.DOES_NOT_EXIST;
        }
        """;
    myFixture.enableInspections(CompactUnresolvedReferenceInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> unresolved = myFixture.doHighlighting().stream()
        .filter(h -> h.getDescription() != null && h.getDescription().contains("Unresolved enum member 'DOES_NOT_EXIST'"))
        .toList();
    assertEquals("Should report 1 unresolved enum member error for cross-file enum", 1, unresolved.size());
  }

  // =========================================================================
  // 5. Pure Circuit Inspection Tests
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
  // 6. Sealed Field Mutation Inspection Tests
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

  // =========================================================================
  // 7. Recursive Circuit Inspection Tests
  // =========================================================================

  public void testNonRecursiveCircuitAllowed() {
    String code = """
        circuit helper(): Field { return 1; }
        circuit compute(): Field { return helper(); }
        """;
    myFixture.enableInspections(CompactRecursiveCircuitInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> warnings = filterInspectionWarnings(myFixture.doHighlighting());
    assertTrue("Non-recursive calls should produce zero recursion warnings", warnings.isEmpty());
  }

  public void testDirectRecursiveCircuitFails() {
    String code = """
        circuit fib(n: Field): Field {
          return fib(n);
        }
        """;
    myFixture.enableInspections(CompactRecursiveCircuitInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> warnings = filterInspectionWarnings(myFixture.doHighlighting());
    List<HighlightInfo> matched = warnings.stream()
        .filter(h -> h.getDescription() != null && h.getDescription().contains("cannot be recursive"))
        .toList();
    assertEquals("Directly recursive circuit should be flagged", 1, matched.size());
  }

  public void testMutualRecursiveCircuitFails() {
    String code = """
        circuit ping(): Field {
          return pong();
        }

        circuit pong(): Field {
          return ping();
        }
        """;
    myFixture.enableInspections(CompactRecursiveCircuitInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> warnings = filterInspectionWarnings(myFixture.doHighlighting());
    List<HighlightInfo> matched = warnings.stream()
        .filter(h -> h.getDescription() != null && h.getDescription().contains("recursion"))
        .toList();
    assertFalse("Mutual recursion should be flagged", matched.isEmpty());
  }

  // =========================================================================
  // 8. Constructor Restriction Inspection Tests
  // =========================================================================

  public void testConstructorValidCodeAllowed() {
    String code = """
        ledger count: Field;

        constructor(c: Field) {
          count = c;
        }
        """;
    myFixture.enableInspections(CompactConstructorRestrictionInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> warnings = filterInspectionWarnings(myFixture.doHighlighting());
    assertTrue("Valid constructor should produce zero warnings", warnings.isEmpty());
  }

  public void testConstructorEmitFails() {
    String code = """
        constructor() {
          emit(1);
        }
        """;
    myFixture.enableInspections(CompactConstructorRestrictionInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> warnings = filterInspectionWarnings(myFixture.doHighlighting());
    List<HighlightInfo> matched = warnings.stream()
        .filter(h -> h.getDescription() != null && h.getDescription().contains("Constructor cannot emit events"))
        .toList();
    assertEquals("Constructor emitting events should be flagged", 1, matched.size());
  }

  // =========================================================================
  // 9. Undisclosed Witness (WPP) Inspection Tests
  // =========================================================================

  public void testDisclosedWitnessAssignmentAllowed() {
    String code = """
        ledger authority: Field;
        witness secretKey(): Field;

        circuit set(): [] {
          const sk = secretKey();
          authority = disclose(sk);
        }
        """;
    myFixture.enableInspections(CompactUndisclosedWitnessInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> warnings = filterInspectionWarnings(myFixture.doHighlighting());
    assertTrue("Disclosed witness assignment should produce zero warnings: " + warnings, warnings.isEmpty());
  }

  public void testUndisclosedWitnessDirectAssignmentFails() {
    String code = """
        ledger authority: Field;
        witness secretKey(): Field;

        circuit set(): [] {
          authority = secretKey();
        }
        """;
    myFixture.enableInspections(CompactUndisclosedWitnessInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> warnings = filterInspectionWarnings(myFixture.doHighlighting());
    List<HighlightInfo> matched = warnings.stream()
        .filter(h -> h.getDescription() != null && h.getDescription().contains("without 'disclose(...)'"))
        .toList();
    assertEquals("Direct undisclosed witness assignment should be flagged", 1, matched.size());
  }

  public void testUndisclosedWitnessVariableAssignmentFails() {
    String code = """
        ledger authority: Field;
        witness secretKey(): Field;

        circuit set(): [] {
          const sk = secretKey();
          authority = sk;
        }
        """;
    myFixture.enableInspections(CompactUndisclosedWitnessInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> warnings = filterInspectionWarnings(myFixture.doHighlighting());
    List<HighlightInfo> matched = warnings.stream()
        .filter(h -> h.getDescription() != null && h.getDescription().contains("without 'disclose(...)'"))
        .toList();
    assertEquals("Undisclosed witness assignment via variable should be flagged", 1, matched.size());
  }
}

