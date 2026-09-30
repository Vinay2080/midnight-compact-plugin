package dev.verloren.midnight.inspection;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import dev.verloren.midnight.CompactFileType;

import java.util.List;

public class CompactTypeMismatchInspectionTest extends CompactInspectionTestBase {

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
}
