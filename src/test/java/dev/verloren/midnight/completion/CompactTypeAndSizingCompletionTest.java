package dev.verloren.midnight.completion;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.template.Expression;
import com.intellij.codeInsight.template.ExpressionContext;
import com.intellij.codeInsight.template.Result;
import com.intellij.codeInsight.template.TextResult;
import com.intellij.codeInsight.template.impl.ConstantNode;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiElement;
import dev.verloren.midnight.CompactFileType;
import dev.verloren.midnight.ide.templates.CompactTypeExpression;
import dev.verloren.midnight.ide.templates.CompactTypeMacro;

import java.util.Arrays;
import java.util.List;

public class CompactTypeAndSizingCompletionTest extends CompactCompletionTestBase {

  public void testExportLedgerTypeCompletion() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        export ledger ledger1: <caret>;
        """
    );
    myFixture.completeBasic();
    List<String> lookupStrings = myFixture.getLookupElementStrings();
    assertNotNull("Lookup strings should not be null", lookupStrings);
    assertTrue("Should suggest 'State'", lookupStrings.contains("State"));
    assertTrue("Should suggest 'Bytes'", lookupStrings.contains("Bytes"));
    assertTrue("Should suggest 'Bytes<32>'", lookupStrings.contains("Bytes<32>"));
    assertTrue("Should suggest 'Uint'", lookupStrings.contains("Uint"));
    assertTrue("Should suggest 'Uint<64>'", lookupStrings.contains("Uint<64>"));
    assertTrue("Should suggest 'Field'", lookupStrings.contains("Field"));
    assertFalse("Should NOT suggest 'circuit'", lookupStrings.contains("circuit"));
    assertFalse("Should NOT suggest 'ledger'", lookupStrings.contains("ledger"));
  }

  public void testExportLedgerPrefixTypeCompletion() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        export ledger ledger1: St<caret>;
        """
    );
    myFixture.completeBasic();
    List<String> lookupStrings = myFixture.getLookupElementStrings();
    assertNotNull("Lookup strings should not be null", lookupStrings);
    assertTrue("Should suggest 'State'", lookupStrings.contains("State"));
    assertFalse("Should NOT suggest 'circuit'", lookupStrings.contains("circuit"));
  }

  public void testBareLedgerTypeCompletion() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        ledger myLedger: <caret>;
        """
    );
    myFixture.completeBasic();
    List<String> lookupStrings = myFixture.getLookupElementStrings();
    assertNotNull("Lookup strings should not be null", lookupStrings);
    assertTrue("Should suggest 'State'", lookupStrings.contains("State"));
    assertTrue("Should suggest 'Counter'", lookupStrings.contains("Counter"));
  }

  public void testExportCircuitReturnTypeCompletion() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        export circuit myCircuit(): <caret> {
        }
        """
    );
    myFixture.completeBasic();
    List<String> lookupStrings = myFixture.getLookupElementStrings();
    assertNotNull("Lookup strings should not be null", lookupStrings);
    assertTrue("Should suggest 'Void'", lookupStrings.contains("Void"));
    assertTrue("Should suggest 'Field'", lookupStrings.contains("Field"));
    assertTrue("Should suggest 'Boolean'", lookupStrings.contains("Boolean"));
  }

  public void testStructFieldTypeCompletion() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        struct User {
            id: <caret>
        }
        """
    );
    myFixture.completeBasic();
    List<String> lookupStrings = myFixture.getLookupElementStrings();
    assertNotNull("Lookup strings should not be null", lookupStrings);
    assertTrue("Should suggest 'Field'", lookupStrings.contains("Field"));
    assertTrue("Should suggest 'Bytes'", lookupStrings.contains("Bytes"));
  }

  public void testTypeAliasTypeCompletion() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        type MyType = <caret>;
        """
    );
    myFixture.completeBasic();
    List<String> lookupStrings = myFixture.getLookupElementStrings();
    assertNotNull("Lookup strings should not be null", lookupStrings);
    assertTrue("Should suggest 'Field'", lookupStrings.contains("Field"));
    assertTrue("Should suggest 'Bytes'", lookupStrings.contains("Bytes"));
  }

  public void testTypeExpressionLookupItems() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        struct CustomRecord {}
        export ledger ledger1: <caret>State;
        """
    );
    CompactTypeExpression expr = new CompactTypeExpression("State");
    LookupElement[] items = expr.calculateLookupItems(createExpressionContext());
    assertNotNull("Lookup items should not be null", items);
    List<String> itemNames = Arrays.stream(items).map(LookupElement::getLookupString).toList();
    assertTrue("Should include 'State'", itemNames.contains("State"));
    assertTrue("Should include 'Bytes'", itemNames.contains("Bytes"));
    assertTrue("Should include 'Bytes<32>'", itemNames.contains("Bytes<32>"));
    assertTrue("Should include 'Uint'", itemNames.contains("Uint"));
    assertTrue("Should include 'Uint<64>'", itemNames.contains("Uint<64>"));
    assertTrue("Should include 'CustomRecord'", itemNames.contains("CustomRecord"));
  }

  private ExpressionContext createExpressionContext() {
    return new ExpressionContext() {
      @Override
      public Project getProject() {
        return myFixture.getProject();
      }

      @Override
      public Editor getEditor() {
        return myFixture.getEditor();
      }

      @Override
      public int getStartOffset() {
        return myFixture.getCaretOffset();
      }

      @Override
      public int getTemplateStartOffset() {
        return 0;
      }

      @Override
      public int getTemplateEndOffset() {
        return myFixture.getFile().getTextLength();
      }

      @Override
      public <T> T getProperty(Key<T> key) {
        return null;
      }

      @Override
      public PsiElement getPsiElementAtStartOffset() {
        return myFixture.getFile().findElementAt(myFixture.getCaretOffset());
      }

      @Override
      public TextResult getVariableValue(String variableName) {
        return null;
      }
    };
  }

  public void testTypeMacroDelegatesToExpression() {
    CompactTypeMacro macro = new CompactTypeMacro();
    assertEquals("compactType", macro.getName());
    assertEquals("compactType(defaultType)", macro.getPresentableName());

    Expression[] params = new Expression[]{new ConstantNode("State")};
    Result result = macro.calculateResult(params, null);
    assertNotNull(result);
    assertEquals("State", result.toString());
  }

  public void testBytesCompletionInsertsAngleBracketsAndPlacesCaretInside() {
    myFixture.configureByText(CompactFileType.INSTANCE, "export ledger ledger1: By<caret>;");
    myFixture.completeBasic();
    LookupElement[] elements = myFixture.getLookupElements();
    assertNotNull(elements);
    LookupElement bytesEl = null;
    for (LookupElement el : elements) {
      if ("Bytes".equals(el.getLookupString())) {
        bytesEl = el;
        break;
      }
    }
    assertNotNull("Should find 'Bytes' lookup element", bytesEl);
    myFixture.getLookup().setCurrentItem(bytesEl);
    myFixture.type('\n');
    myFixture.checkResult("export ledger ledger1: Bytes<<caret>>;");
  }

  public void testUintCompletionInsertsAngleBracketsAndPlacesCaretInside() {
    myFixture.configureByText(CompactFileType.INSTANCE, "const x: Ui<caret>;");
    myFixture.completeBasic();
    LookupElement[] elements = myFixture.getLookupElements();
    assertNotNull(elements);
    LookupElement uintEl = null;
    for (LookupElement el : elements) {
      if ("Uint".equals(el.getLookupString())) {
        uintEl = el;
        break;
      }
    }
    assertNotNull("Should find 'Uint' lookup element", uintEl);
    myFixture.getLookup().setCurrentItem(uintEl);
    myFixture.type('\n');
    myFixture.checkResult("const x: Uint<<caret>>;");
  }

  public void testBytesInsideBracketsSuggestsSizeOptions() {
    myFixture.configureByText(CompactFileType.INSTANCE, "export ledger l: Bytes<<caret>>;\n");
    myFixture.completeBasic();
    List<String> lookupStrings = myFixture.getLookupElementStrings();
    assertNotNull("Lookup strings should not be null", lookupStrings);
    assertTrue("Should suggest '32'", lookupStrings.contains("32"));
    assertTrue("Should suggest '64'", lookupStrings.contains("64"));
    assertTrue("Should suggest '16'", lookupStrings.contains("16"));
    assertTrue("Should suggest '8'", lookupStrings.contains("8"));
  }

  public void testUintInsideBracketsSuggestsBitWidthOptions() {
    myFixture.configureByText(CompactFileType.INSTANCE, "const x: Uint<<caret>>;\n");
    myFixture.completeBasic();
    List<String> lookupStrings = myFixture.getLookupElementStrings();
    assertNotNull("Lookup strings should not be null", lookupStrings);
    assertTrue("Should suggest '8'", lookupStrings.contains("8"));
    assertTrue("Should suggest '16'", lookupStrings.contains("16"));
    assertTrue("Should suggest '32'", lookupStrings.contains("32"));
    assertTrue("Should suggest '64'", lookupStrings.contains("64"));
    assertTrue("Should suggest '128'", lookupStrings.contains("128"));
    assertTrue("Should suggest '256'", lookupStrings.contains("256"));
  }

  public void testBytesSizeOptionCompletion() {
    myFixture.configureByText(CompactFileType.INSTANCE, "export ledger ledger1: Bytes<<caret>>;\n");
    myFixture.completeBasic();
    LookupElement[] elements = myFixture.getLookupElements();
    assertNotNull(elements);
    LookupElement item32 = null;
    for (LookupElement el : elements) {
      if ("32".equals(el.getLookupString())) {
        item32 = el;
        break;
      }
    }
    assertNotNull(item32);
    myFixture.getLookup().setCurrentItem(item32);
    myFixture.type('\n');
    myFixture.checkResult("export ledger ledger1: Bytes<32<caret>>;\n");
  }

  public void testUintSizeOptionCompletion() {
    myFixture.configureByText(CompactFileType.INSTANCE, "const x: Uint<<caret>>;\n");
    myFixture.completeBasic();
    LookupElement[] elements = myFixture.getLookupElements();
    assertNotNull(elements);
    LookupElement item64 = null;
    for (LookupElement el : elements) {
      if ("64".equals(el.getLookupString())) {
        item64 = el;
        break;
      }
    }
    assertNotNull(item64);
    myFixture.getLookup().setCurrentItem(item64);
    myFixture.type('\n');
    myFixture.checkResult("const x: Uint<64<caret>>;\n");
  }

  public void testPreconfiguredBytes32Completion() {
    myFixture.configureByText(CompactFileType.INSTANCE, "export ledger ledger1: <caret>;\n");
    myFixture.completeBasic();
    LookupElement[] elements = myFixture.getLookupElements();
    assertNotNull(elements);
    LookupElement bytes32 = null;
    for (LookupElement el : elements) {
      if ("Bytes<32>".equals(el.getLookupString())) {
        bytes32 = el;
        break;
      }
    }
    assertNotNull(bytes32);
    myFixture.getLookup().setCurrentItem(bytes32);
    myFixture.type('\n');
    myFixture.checkResult("export ledger ledger1: Bytes<32><caret>;\n");
  }

  public void testPreconfiguredUint64Completion() {
    myFixture.configureByText(CompactFileType.INSTANCE, "const x: <caret>;\n");
    myFixture.completeBasic();
    LookupElement[] elements = myFixture.getLookupElements();
    assertNotNull(elements);
    LookupElement uint64 = null;
    for (LookupElement el : elements) {
      if ("Uint<64>".equals(el.getLookupString())) {
        uint64 = el;
        break;
      }
    }
    assertNotNull(uint64);
    myFixture.getLookup().setCurrentItem(uint64);
    myFixture.type('\n');
    myFixture.checkResult("const x: Uint<64><caret>;\n");
  }
}
