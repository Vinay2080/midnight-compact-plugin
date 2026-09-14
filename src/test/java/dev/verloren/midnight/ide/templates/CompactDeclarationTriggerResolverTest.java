package dev.verloren.midnight.ide.templates;

import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.*;

public class CompactDeclarationTriggerResolverTest {

  @Test
  public void testBareDeclarationTriggers() {
    // Circuit
    assertResolved("ci", CompactDeclarationType.CIRCUIT, false);
    assertResolved("cir", CompactDeclarationType.CIRCUIT, false);
    assertResolved("circ", CompactDeclarationType.CIRCUIT, false);
    assertResolved("circuit", CompactDeclarationType.CIRCUIT, false);

    // Witness
    assertResolved("wi", CompactDeclarationType.WITNESS, false);
    assertResolved("wit", CompactDeclarationType.WITNESS, false);
    assertResolved("witn", CompactDeclarationType.WITNESS, false);
    assertResolved("witness", CompactDeclarationType.WITNESS, false);

    // Struct
    assertResolved("st", CompactDeclarationType.STRUCT, false);
    assertResolved("str", CompactDeclarationType.STRUCT, false);
    assertResolved("struct", CompactDeclarationType.STRUCT, false);

    // Enum
    assertResolved("en", CompactDeclarationType.ENUM, false);
    assertResolved("enum", CompactDeclarationType.ENUM, false);

    // Module
    assertResolved("mo", CompactDeclarationType.MODULE, false);
    assertResolved("mod", CompactDeclarationType.MODULE, false);
    assertResolved("module", CompactDeclarationType.MODULE, false);

    // Contract
    assertResolved("cct", CompactDeclarationType.CONTRACT, false);
    assertResolved("contract", CompactDeclarationType.CONTRACT, false);

    // Ledger
    assertResolved("led", CompactDeclarationType.LEDGER, false);
    assertResolved("ledg", CompactDeclarationType.LEDGER, false);
    assertResolved("ledger", CompactDeclarationType.LEDGER, false);

    // Type
    assertResolved("ty", CompactDeclarationType.TYPE, false);
    assertResolved("type", CompactDeclarationType.TYPE, false);
  }

  @Test
  public void testCompactModifierDeclarationTriggers() {
    // Circuit with ex, exp, export prefixes
    assertResolved("exci", CompactDeclarationType.CIRCUIT, true);
    assertResolved("excir", CompactDeclarationType.CIRCUIT, true);
    assertResolved("excirc", CompactDeclarationType.CIRCUIT, true);
    assertResolved("excircuit", CompactDeclarationType.CIRCUIT, true);
    assertResolved("expci", CompactDeclarationType.CIRCUIT, true);
    assertResolved("expcir", CompactDeclarationType.CIRCUIT, true);
    assertResolved("exportci", CompactDeclarationType.CIRCUIT, true);
    assertResolved("exportcir", CompactDeclarationType.CIRCUIT, true);
    assertResolved("exportcircuit", CompactDeclarationType.CIRCUIT, true);

    // Witness with modifier prefixes
    assertResolved("exwi", CompactDeclarationType.WITNESS, true);
    assertResolved("exwit", CompactDeclarationType.WITNESS, true);
    assertResolved("exportwit", CompactDeclarationType.WITNESS, true);
    assertResolved("exportwitness", CompactDeclarationType.WITNESS, true);

    // Struct with modifier prefixes
    assertResolved("exstr", CompactDeclarationType.STRUCT, true);
    assertResolved("exportstruct", CompactDeclarationType.STRUCT, true);

    // Enum with modifier prefixes
    assertResolved("exen", CompactDeclarationType.ENUM, true);
    assertResolved("exportenum", CompactDeclarationType.ENUM, true);

    // Ledger with modifier prefixes
    assertResolved("exled", CompactDeclarationType.LEDGER, true);
    assertResolved("exportledger", CompactDeclarationType.LEDGER, true);
  }

  @Test
  public void testSeparatedModifierDeclarationTriggers() {
    assertResolved("export circ", CompactDeclarationType.CIRCUIT, true);
    assertResolved("export ci", CompactDeclarationType.CIRCUIT, true);
    assertResolved("export cir", CompactDeclarationType.CIRCUIT, true);
    assertResolved("export circuit", CompactDeclarationType.CIRCUIT, true);
    assertResolved("exp cir", CompactDeclarationType.CIRCUIT, true);
    assertResolved("ex cir", CompactDeclarationType.CIRCUIT, true);

    assertResolved("export wit", CompactDeclarationType.WITNESS, true);
    assertResolved("export witness", CompactDeclarationType.WITNESS, true);

    assertResolved("export str", CompactDeclarationType.STRUCT, true);
    assertResolved("export struct", CompactDeclarationType.STRUCT, true);

    assertResolved("export led", CompactDeclarationType.LEDGER, true);
    assertResolved("export ledger", CompactDeclarationType.LEDGER, true);
  }

  @Test
  public void testProhibitedExportConst() {
    assertNull("Export const must be rejected", CompactDeclarationTriggerResolver.resolve("exconst"));
    assertNull("Export const must be rejected", CompactDeclarationTriggerResolver.resolve("export const"));
    assertNull("Export const must be rejected", CompactDeclarationTriggerResolver.resolve("exp const"));
    assertNull("Export const must be rejected", CompactDeclarationTriggerResolver.resolve("ex const"));
  }

  @Test
  public void testInvalidTriggers() {
    assertNull(CompactDeclarationTriggerResolver.resolve(null));
    assertNull(CompactDeclarationTriggerResolver.resolve(""));
    assertNull(CompactDeclarationTriggerResolver.resolve(" "));
    assertNull(CompactDeclarationTriggerResolver.resolve("c"));
    assertNull(CompactDeclarationTriggerResolver.resolve("unknown"));
    assertNull(CompactDeclarationTriggerResolver.resolve("export unknown"));
  }

  @Test
  public void testGenerateLookupStrings() {
    Set<String> circuitBare = CompactDeclarationTriggerResolver.generateLookupStrings(CompactDeclarationType.CIRCUIT, false);
    assertTrue(circuitBare.contains("ci"));
    assertTrue(circuitBare.contains("cir"));
    assertTrue(circuitBare.contains("circuit"));

    Set<String> circuitExport = CompactDeclarationTriggerResolver.generateLookupStrings(CompactDeclarationType.CIRCUIT, true);
    assertTrue(circuitExport.contains("exci"));
    assertTrue(circuitExport.contains("excir"));
    assertTrue(circuitExport.contains("exportci"));
    assertTrue(circuitExport.contains("exportcir"));
    assertTrue(circuitExport.contains("export circ"));

    Set<String> constExport = CompactDeclarationTriggerResolver.generateLookupStrings(CompactDeclarationType.CONST, true);
    assertTrue("Non-exportable type should produce empty export lookups", constExport.isEmpty());
  }

  private static void assertResolved(String input, CompactDeclarationType expectedType, boolean expectedExported) {
    CompactDeclarationTriggerResolver.TriggerResult result = CompactDeclarationTriggerResolver.resolve(input);
    assertNotNull("Expected resolution for '" + input + "'", result);
    assertEquals("Declaration type mismatch for '" + input + "'", expectedType, result.declarationType());
    assertEquals("Export state mismatch for '" + input + "'", expectedExported, result.isExported());
  }
}
