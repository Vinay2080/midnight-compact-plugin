package dev.verloren.midnight.inspection;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.intention.IntentionAction;
import dev.verloren.midnight.CompactFileType;

import java.util.List;

public class CompactDuplicateAndUnusedInspectionTest extends CompactInspectionTestBase {

  // =========================================================================
  // Duplicate Declaration Inspection Tests
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
  // Unused Local Variable Inspection Tests
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
            struct Point { x: Field; y: Field; }\n            """;
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
    IntentionAction action = myFixture.findSingleIntention("Remove unused variable 'unusedVar'");
    assertNotNull("Quick-fix to remove unused variable should be available", action);
    myFixture.launchAction(action);
    String expected = """
            circuit test(): [] {
              const y = 10;
            }
            """;
    myFixture.checkResult(expected);
  }
}
