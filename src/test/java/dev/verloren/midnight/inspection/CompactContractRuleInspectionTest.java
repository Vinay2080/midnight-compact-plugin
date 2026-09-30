package dev.verloren.midnight.inspection;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import dev.verloren.midnight.CompactFileType;

import java.util.List;

public class CompactContractRuleInspectionTest extends CompactInspectionTestBase {

  // =========================================================================
  // Recursive Circuit Inspection Tests
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

  public void testWrapperCircuitCallingImportedCircuitWithSameNameNotFlagged() {
    myFixture.addFileToProject(
            "Base.compact",
            """
                    export circuit grantRole(): Void {}
                    """
    );
    String code = """
            import "./Base" prefix Base_;
            
            export circuit grantRole(): Void {
              Base_grantRole();
            }
            """;
    myFixture.enableInspections(CompactRecursiveCircuitInspection.class);
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> warnings = filterInspectionWarnings(myFixture.doHighlighting());
    List<HighlightInfo> recursionWarnings = warnings.stream()
            .filter(h -> h.getDescription() != null && (h.getDescription().contains("cannot be recursive") || h.getDescription().contains("recursion")))
            .toList();
    assertTrue("Forwarding wrapper circuit calling imported circuit with same name should not be flagged as recursive: " + recursionWarnings, recursionWarnings.isEmpty());
  }

  // =========================================================================
  // Constructor Restriction Inspection Tests
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
  // Undisclosed Witness (WPP) Inspection Tests
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

  // =========================================================================
  // Utils Module Semantic Resolution Tests
  // =========================================================================

  public void testUtilsModuleEitherAndFieldResolutionNoWarnings() {
    String code = """
            module Utils {
              import CompactStandardLibrary;

              export pure circuit isKeyOrAddressZero(
                  keyOrAddress: Either<ZswapCoinPublicKey, ContractAddress>): Boolean {
                return isContractAddress(keyOrAddress)
                    ? default<ContractAddress> == keyOrAddress.right : default<ZswapCoinPublicKey> == keyOrAddress.left;
              }

              export pure circuit isKeyZero(key: ZswapCoinPublicKey): Boolean {
                const zero = default<ZswapCoinPublicKey>;
                return zero == key;
              }

              export pure circuit isKeyOrAddressEqual(
                  keyOrAddress: Either<ZswapCoinPublicKey, ContractAddress>,
                  other: Either<ZswapCoinPublicKey, ContractAddress>): Boolean {
                if (keyOrAddress.is_left && other.is_left) {
                  return keyOrAddress.left == other.left;
                } else if (!keyOrAddress.is_left && !other.is_left) {
                  return keyOrAddress.right == other.right;
                } else {
                  return false;
                }
              }

              export pure circuit isContractAddress(
                  keyOrAddress: Either<ZswapCoinPublicKey, ContractAddress>): Boolean {
                return !keyOrAddress.is_left;
              }

              export pure circuit emptyString(): Opaque<"string"> {
                return default<Opaque<"string">>;
              }

              export pure circuit canonicalize<T1, T2>(
                  value: Either<T1, T2>): Either<T1, T2> {
                return value.is_left
                    ? Either<T1, T2> { is_left: true, left: value.left, right: default<T2> }
                    : Either<T1, T2> { is_left: false, left: default<T1>, right: value.right };
              }

              export pure circuit zeroAccount(): Either<Bytes<32>, ContractAddress> {
                return Either<Bytes<32>, ContractAddress> { is_left: true, left: default<Bytes<32>>, right: default<ContractAddress> };
              }

              export pure circuit isTargetZero(target: Either<Bytes<32>, ContractAddress>): Boolean {
                if (target.is_left) {
                  return target.left == default<Bytes<32>>;
                } else {
                  return target.right == default<ContractAddress>;
                }
              }

              export circuit selfAsRecipient(): Either<ZswapCoinPublicKey, ContractAddress> {
                return right<ZswapCoinPublicKey, ContractAddress>(kernel.self());
              }
            }
            """;
    enableAllInspections();
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> highlights = myFixture.doHighlighting();
    List<HighlightInfo> errors = highlights.stream()
            .filter(h -> h.getDescription() != null && (
                    h.getDescription().contains("Unresolved")
                            || h.getDescription().contains("Type mismatch")
            ))
            .toList();
    assertTrue("Utils module with Either operations should produce zero semantic errors: " + errors, errors.isEmpty());
  }
}
