package dev.verloren.midnight.psi;

import com.intellij.psi.tree.IElementType;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import dev.verloren.midnight.parser.CompactElementTypes;

public class ElementFactoryConsistencyTest extends BasePlatformTestCase {
  public void testDedicatedFactoryBranches() {
    assertDedicated(CompactElementTypes.PRAGMA_FORM);
    assertDedicated(CompactElementTypes.INCLUDE_FORM);
    assertDedicated(CompactElementTypes.IMPORT_FORM);
    assertDedicated(CompactElementTypes.EXPORT_FORM);
    assertDedicated(CompactElementTypes.MODULE_DEFINITION);
    assertDedicated(CompactElementTypes.STRUCT_DECLARATION);
    assertDedicated(CompactElementTypes.ENUM_DECLARATION);
    assertDedicated(CompactElementTypes.CONTRACT_DECLARATION);
    assertDedicated(CompactElementTypes.IMPLEMENTS_DECLARATION);
    assertDedicated(CompactElementTypes.TYPE_ALIAS_DECLARATION);
    assertDedicated(CompactElementTypes.LEDGER_DECLARATION);
    assertDedicated(CompactElementTypes.WITNESS_DECLARATION);
    assertDedicated(CompactElementTypes.CONSTRUCTOR_DEFINITION);
    assertDedicated(CompactElementTypes.CIRCUIT_DEFINITION);
    assertDedicated(CompactElementTypes.BLOCK);
    assertDedicated(CompactElementTypes.REFERENCE_EXPR);
  }

  private static void assertDedicated(IElementType elementType) {
    assertTrue(elementType.toString(), CompactElementFactory.hasDedicatedElement(elementType));
  }
}