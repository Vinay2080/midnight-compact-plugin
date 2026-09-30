package dev.verloren.midnight.inspection;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import dev.verloren.midnight.CompactFileType;

import java.util.List;

public class CompactReturnTypeInspectionTest extends CompactInspectionTestBase {

  public void testReturnTypeMismatchNumericIntoBoolean() {
    String code = """
            circuit testZkir(): Boolean {\n              return 0;\n            }\n            """;
    myFixture.enableInspections(CompactTypeMismatchInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> mismatches = myFixture.doHighlighting().stream()
            .filter(h -> h.getDescription() != null && h.getDescription().contains("Type mismatch: expected 'Boolean', got 'Field'"))
            .toList();
    assertEquals("Should report 1 type mismatch for return 0 in Boolean circuit", 1, mismatches.size());
  }

  public void testReturnTypeMismatchBooleanIntoField() {
    String code = """
            circuit test(): Field {\n              return true;\n            }\n            """;
    myFixture.enableInspections(CompactTypeMismatchInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> mismatches = myFixture.doHighlighting().stream()
            .filter(h -> h.getDescription() != null && h.getDescription().contains("Type mismatch: expected 'Field', got 'Boolean'"))
            .toList();
    assertEquals("Should report 1 type mismatch for return true in Field circuit", 1, mismatches.size());
  }

  public void testReturnTypeMatchValidBoolean() {
    String code = """
            circuit test(): Boolean {\n              return true;\n            }\n            """;
    myFixture.enableInspections(CompactTypeMismatchInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> warnings = filterInspectionWarnings(myFixture.doHighlighting());
    assertTrue("Valid boolean return should produce zero warnings: " + warnings, warnings.isEmpty());
  }

  public void testReturnTypeMismatchVoidWithValue() {
    String code = """
            circuit test(): [] {\n              return 10;\n            }\n            """;
    myFixture.enableInspections(CompactTypeMismatchInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> mismatches = myFixture.doHighlighting().stream()
            .filter(h -> h.getDescription() != null && h.getDescription().contains("Type mismatch: expected 'Void', got 'Field'"))
            .toList();
    assertEquals("Should report 1 type mismatch for non-empty return in Void callable", 1, mismatches.size());
  }

  public void testReturnTypeValidVoidEmptyReturn() {
    String code = """
            circuit test(): [] {\n              return;\n            }\n            """;
    myFixture.enableInspections(CompactTypeMismatchInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> warnings = filterInspectionWarnings(myFixture.doHighlighting());
    assertTrue("Valid empty return in void callable should produce zero warnings: " + warnings, warnings.isEmpty());
  }

  public void testReturnTypeMatchSimpleTernaryExpression() {
    String code = """
            circuit test(cond: Boolean): Field {\n              return cond ? 1 : 2;\n            }\n            """;
    myFixture.enableInspections(CompactTypeMismatchInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> warnings = filterInspectionWarnings(myFixture.doHighlighting());
    assertTrue("Valid ternary return of Field should produce zero warnings: " + warnings, warnings.isEmpty());
  }

  public void testReturnTypeMatchTernaryExpressionGenericStruct() {
    String code = """
            struct Either<T1, T2> {
              is_left: Boolean;
              left: T1;
              right: T2;
            }
            
            export pure circuit canonicalize<T1, T2>(value: Either<T1, T2>): Either<T1, T2> {
              return value.is_left
                  ? Either<T1, T2> { is_left: true, left: value.left, right: default<T2> }
                  : Either<T1, T2> { is_left: false, left: default<T1>, right: value.right };
            }
            """;
    myFixture.enableInspections(CompactTypeMismatchInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> warnings = filterInspectionWarnings(myFixture.doHighlighting());
    assertTrue("Valid ternary return of struct should produce zero warnings: " + warnings, warnings.isEmpty());
  }

  public void testReturnTypeMismatchTernaryExpression() {
    String code = """
            circuit test(cond: Boolean): Field {\n              return cond ? true : false;\n            }\n            """;
    myFixture.enableInspections(CompactTypeMismatchInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> mismatches = myFixture.doHighlighting().stream()
            .filter(h -> h.getDescription() != null && h.getDescription().contains("Type mismatch: expected 'Field', got 'Boolean'"))
            .toList();
    assertEquals("Should report 1 type mismatch for return boolean ternary in Field circuit", 1, mismatches.size());
  }

  public void testTernaryConditionNonBooleanFails() {
    String code = """
            circuit test(cond: Field): Field {\n              return cond ? 1 : 2;\n            }\n            """;
    myFixture.enableInspections(CompactTypeMismatchInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> mismatches = myFixture.doHighlighting().stream()
            .filter(h -> h.getDescription() != null && h.getDescription().contains("Boolean expected in ternary condition, got 'Field'"))
            .toList();
    assertEquals("Should report boolean expected in ternary condition", 1, mismatches.size());
  }

  public void testTernaryBranchesTypeMismatchFails() {
    String code = """
            circuit test(cond: Boolean): Boolean {\n              return cond ? true : 10;\n            }\n            """;
    myFixture.enableInspections(CompactTypeMismatchInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> mismatches = myFixture.doHighlighting().stream()
            .filter(h -> h.getDescription() != null && h.getDescription().contains("Type mismatch in ternary branches"))
            .toList();
    assertEquals("Should report branch mismatch between Boolean and numeric literal", 1, mismatches.size());
  }

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
}
