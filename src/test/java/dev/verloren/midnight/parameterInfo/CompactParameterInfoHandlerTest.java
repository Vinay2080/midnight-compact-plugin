package dev.verloren.midnight.parameterInfo;

import com.intellij.lang.parameterInfo.CreateParameterInfoContext;
import com.intellij.lang.parameterInfo.ParameterInfoHandler;
import com.intellij.lang.parameterInfo.ParameterInfoUIContext;
import com.intellij.lang.parameterInfo.UpdateParameterInfoContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.util.UserDataHolderEx;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import dev.verloren.midnight.CompactFileType;
import dev.verloren.midnight.psi.CompactCallExprImpl;
import dev.verloren.midnight.psi.CompactStructLiteralExprImpl;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;

public class CompactParameterInfoHandlerTest extends BasePlatformTestCase {

  private CompactParameterInfoHandler handler;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    handler = new CompactParameterInfoHandler();
  }

  public void testCircuitCallParameters() {
    myFixture.configureByText(CompactFileType.INSTANCE, """
        circuit transfer(recipient: Address, amount: Uint<64>): Boolean {
            return true;
        }

        circuit test(): [] {
            transfer(<caret>);
        }
        """);

    MockCreateParameterInfoContext createContext = new MockCreateParameterInfoContext(myFixture.getEditor(), myFixture.getFile());
    PsiElement owner = handler.findElementForParameterInfo(createContext);
    assertNotNull("Call expression should be recognized as parameter owner", owner);
    assertInstanceOf(owner, CompactCallExprImpl.class);

    Object[] items = createContext.getItemsToShow();
    assertNotNull("Items to show should not be null", items);
    assertEquals(1, items.length);
    CompactParametersDescription desc = (CompactParametersDescription) items[0];
    assertFalse(desc.isStructLiteral());
    assertEquals("recipient: Address, amount: Uint<64>", desc.getPresentableText());

    MockUpdateParameterInfoContext updateContext = new MockUpdateParameterInfoContext(myFixture.getEditor(), myFixture.getFile(), items);
    updateContext.setParameterOwner(owner);
    handler.updateParameterInfo(owner, updateContext);
    assertEquals(0, updateContext.getCurrentParameter());

    MockParameterInfoUIContext<PsiElement> uiContext = new MockParameterInfoUIContext<>(owner);
    uiContext.setCurrentParameterIndex(updateContext.getCurrentParameter());
    handler.updateUI(desc, uiContext);
    assertEquals(0, uiContext.getHighlightStart());
    assertEquals("recipient: Address".length(), uiContext.getHighlightEnd());
  }

  public void testCircuitCallSecondParameter() {
    myFixture.configureByText(CompactFileType.INSTANCE, """
        circuit transfer(recipient: Address, amount: Uint<64>): Boolean {
            return true;
        }

        circuit test(): [] {
            transfer(alice, <caret>);
        }
        """);

    MockCreateParameterInfoContext createContext = new MockCreateParameterInfoContext(myFixture.getEditor(), myFixture.getFile());
    PsiElement owner = handler.findElementForParameterInfo(createContext);
    assertNotNull(owner);

    Object[] items = createContext.getItemsToShow();
    assertNotNull(items);
    CompactParametersDescription desc = (CompactParametersDescription) items[0];

    MockUpdateParameterInfoContext updateContext = new MockUpdateParameterInfoContext(myFixture.getEditor(), myFixture.getFile(), items);
    updateContext.setParameterOwner(owner);
    handler.updateParameterInfo(owner, updateContext);
    assertEquals(1, updateContext.getCurrentParameter());

    MockParameterInfoUIContext<PsiElement> uiContext = new MockParameterInfoUIContext<>(owner);
    uiContext.setCurrentParameterIndex(updateContext.getCurrentParameter());
    handler.updateUI(desc, uiContext);
    int start = "recipient: Address, ".length();
    int end = start + "amount: Uint<64>".length();
    assertEquals(start, uiContext.getHighlightStart());
    assertEquals(end, uiContext.getHighlightEnd());
  }

  public void testCaretBeforeOpeningParenReturnsNegativeIndex() {
    myFixture.configureByText(CompactFileType.INSTANCE, """
        circuit transfer(recipient: Address, amount: Uint<64>): Boolean {
            return true;
        }

        circuit test(): [] {
            transfer<caret>(alice);
        }
        """);

    MockCreateParameterInfoContext createContext = new MockCreateParameterInfoContext(myFixture.getEditor(), myFixture.getFile());
    PsiElement owner = handler.findElementForParameterInfo(createContext);
    assertNotNull(owner);

    Object[] items = createContext.getItemsToShow();
    assertNotNull(items);
    CompactParametersDescription desc = (CompactParametersDescription) items[0];

    MockUpdateParameterInfoContext updateContext = new MockUpdateParameterInfoContext(myFixture.getEditor(), myFixture.getFile(), items);
    updateContext.setParameterOwner(owner);
    handler.updateParameterInfo(owner, updateContext);
    assertEquals(-1, updateContext.getCurrentParameter());

    MockParameterInfoUIContext<PsiElement> uiContext = new MockParameterInfoUIContext<>(owner);
    uiContext.setCurrentParameterIndex(updateContext.getCurrentParameter());
    handler.updateUI(desc, uiContext);
    assertEquals(0, uiContext.getHighlightStart());
    assertEquals(0, uiContext.getHighlightEnd());
  }

  public void testOversuppliedArgumentsDisablesPresentation() {
    myFixture.configureByText(CompactFileType.INSTANCE, """
        circuit transfer(recipient: Address, amount: Uint<64>): Boolean {
            return true;
        }

        circuit test(): [] {
            transfer(alice, 100, <caret>);
        }
        """);

    MockCreateParameterInfoContext createContext = new MockCreateParameterInfoContext(myFixture.getEditor(), myFixture.getFile());
    PsiElement owner = handler.findElementForParameterInfo(createContext);
    assertNotNull(owner);

    Object[] items = createContext.getItemsToShow();
    assertNotNull("Items to show should not be null", items);
    assertEquals(1, items.length);
    CompactParametersDescription desc = (CompactParametersDescription) items[0];

    MockUpdateParameterInfoContext updateContext = new MockUpdateParameterInfoContext(myFixture.getEditor(), myFixture.getFile(), items);
    updateContext.setParameterOwner(owner);
    handler.updateParameterInfo(owner, updateContext);
    assertEquals(2, updateContext.getCurrentParameter());

    MockParameterInfoUIContext<PsiElement> uiContext = new MockParameterInfoUIContext<>(owner);
    uiContext.setCurrentParameterIndex(updateContext.getCurrentParameter());
    handler.updateUI(desc, uiContext);
    assertFalse("UI should be disabled when arguments exceed declared parameters", uiContext.isUIComponentEnabled());
  }

  public void testZeroParameterCircuitCall() {
    myFixture.configureByText(CompactFileType.INSTANCE, """
        circuit noop(): [] {
        }

        circuit test(): [] {
            noop(<caret>);
        }
        """);

    MockCreateParameterInfoContext createContext = new MockCreateParameterInfoContext(myFixture.getEditor(), myFixture.getFile());
    PsiElement owner = handler.findElementForParameterInfo(createContext);
    assertNotNull(owner);

    Object[] items = createContext.getItemsToShow();
    assertNotNull(items);
    CompactParametersDescription desc = (CompactParametersDescription) items[0];
    assertEquals("<no parameters>", desc.getPresentableText());
  }

  public void testWitnessCallParameters() {
    myFixture.configureByText(CompactFileType.INSTANCE, """
        witness computeSecret(salt: Bytes<32>, index: Uint<32>): Bytes<32>;

        circuit test(): [] {
            computeSecret(salt, <caret>);
        }
        """);

    MockCreateParameterInfoContext createContext = new MockCreateParameterInfoContext(myFixture.getEditor(), myFixture.getFile());
    PsiElement owner = handler.findElementForParameterInfo(createContext);
    assertNotNull(owner);

    Object[] items = createContext.getItemsToShow();
    assertNotNull(items);
    CompactParametersDescription desc = (CompactParametersDescription) items[0];
    assertEquals("salt: Bytes<32>, index: Uint<32>", desc.getPresentableText());

    MockUpdateParameterInfoContext updateContext = new MockUpdateParameterInfoContext(myFixture.getEditor(), myFixture.getFile(), items);
    updateContext.setParameterOwner(owner);
    handler.updateParameterInfo(owner, updateContext);
    assertEquals(1, updateContext.getCurrentParameter());
  }

  public void testConstructorCallParameters() {
    myFixture.configureByText(CompactFileType.INSTANCE, """
        constructor(admin: Address, initialSupply: Uint<64>) {
        }

        circuit test(): [] {
            constructor(<caret>);
        }
        """);

    MockCreateParameterInfoContext createContext = new MockCreateParameterInfoContext(myFixture.getEditor(), myFixture.getFile());
    PsiElement owner = handler.findElementForParameterInfo(createContext);
    assertNotNull(owner);

    Object[] items = createContext.getItemsToShow();
    assertNotNull(items);
    CompactParametersDescription desc = (CompactParametersDescription) items[0];
    assertEquals("admin: Address, initialSupply: Uint<64>", desc.getPresentableText());
  }

  public void testStructLiteralAtFirstField() {
    myFixture.configureByText(CompactFileType.INSTANCE, """
        struct TokenState {
            id: Uint<32>,
            balance: Uint<64>,
            active: Boolean
        }

        circuit test(): [] {
            const s = TokenState { <caret> };
        }
        """);

    MockCreateParameterInfoContext createContext = new MockCreateParameterInfoContext(myFixture.getEditor(), myFixture.getFile());
    PsiElement owner = handler.findElementForParameterInfo(createContext);
    assertNotNull("Struct literal should be recognized as parameter owner", owner);
    assertInstanceOf(owner, CompactStructLiteralExprImpl.class);

    Object[] items = createContext.getItemsToShow();
    assertNotNull(items);
    CompactParametersDescription desc = (CompactParametersDescription) items[0];
    assertTrue(desc.isStructLiteral());
    assertEquals("id: Uint<32>, balance: Uint<64>, active: Boolean", desc.getPresentableText());

    MockUpdateParameterInfoContext updateContext = new MockUpdateParameterInfoContext(myFixture.getEditor(), myFixture.getFile(), items);
    updateContext.setParameterOwner(owner);
    handler.updateParameterInfo(owner, updateContext);
    assertEquals(0, updateContext.getCurrentParameter());

    MockParameterInfoUIContext<PsiElement> uiContext = new MockParameterInfoUIContext<>(owner);
    uiContext.setCurrentParameterIndex(updateContext.getCurrentParameter());
    handler.updateUI(desc, uiContext);
    assertEquals(0, uiContext.getHighlightStart());
    assertEquals("id: Uint<32>".length(), uiContext.getHighlightEnd());
  }

  public void testStructLiteralAtSecondFieldAfterComma() {
    myFixture.configureByText(CompactFileType.INSTANCE, """
        struct TokenState {
            id: Uint<32>,
            balance: Uint<64>,
            active: Boolean
        }

        circuit test(): [] {
            const s = TokenState { id: 1, <caret> };
        }
        """);

    MockCreateParameterInfoContext createContext = new MockCreateParameterInfoContext(myFixture.getEditor(), myFixture.getFile());
    PsiElement owner = handler.findElementForParameterInfo(createContext);
    assertNotNull(owner);

    Object[] items = createContext.getItemsToShow();
    assertNotNull("Items to show should not be null", items);
    assertEquals(1, items.length);
    CompactParametersDescription desc = (CompactParametersDescription) items[0];

    MockUpdateParameterInfoContext updateContext = new MockUpdateParameterInfoContext(myFixture.getEditor(), myFixture.getFile(), items);
    updateContext.setParameterOwner(owner);
    handler.updateParameterInfo(owner, updateContext);
    assertEquals("Second unassigned field 'balance' should be highlighted", 1, updateContext.getCurrentParameter());

    MockParameterInfoUIContext<PsiElement> uiContext = new MockParameterInfoUIContext<>(owner);
    uiContext.setCurrentParameterIndex(updateContext.getCurrentParameter());
    handler.updateUI(desc, uiContext);
    int start = "id: Uint<32>, ".length();
    int end = start + "balance: Uint<64>".length();
    assertEquals(start, uiContext.getHighlightStart());
    assertEquals(end, uiContext.getHighlightEnd());
  }

  public void testStructLiteralOutOfOrderAssignedField() {
    myFixture.configureByText(CompactFileType.INSTANCE, """
        struct TokenState {
            id: Uint<32>,
            balance: Uint<64>,
            active: Boolean
        }

        circuit test(): [] {
            const s = TokenState { balance: 500, <caret>id: 1 };
        }
        """);

    MockCreateParameterInfoContext createContext = new MockCreateParameterInfoContext(myFixture.getEditor(), myFixture.getFile());
    PsiElement owner = handler.findElementForParameterInfo(createContext);
    assertNotNull(owner);

    Object[] items = createContext.getItemsToShow();
    assertNotNull("Items to show should not be null", items);
    assertEquals(1, items.length);
    CompactParametersDescription desc = (CompactParametersDescription) items[0];

    MockUpdateParameterInfoContext updateContext = new MockUpdateParameterInfoContext(myFixture.getEditor(), myFixture.getFile(), items);
    updateContext.setParameterOwner(owner);
    handler.updateParameterInfo(owner, updateContext);
    assertEquals("id: Uint<32>, balance: Uint<64>, active: Boolean", desc.getPresentableText());
    assertEquals("Explicit field 'id' should be selected despite being declared first and placed second", 0, updateContext.getCurrentParameter());
  }

  public void testNestedCallParameterIndexIntegrity() {
    myFixture.configureByText(CompactFileType.INSTANCE, """
        circuit add(a: Uint<64>, b: Uint<64>): Uint<64> { return a + b; }
        circuit outer(x: Uint<64>, y: Uint<64>): Uint<64> { return x; }

        circuit test(): [] {
            outer(add(1, <caret>2), 3);
        }
        """);

    MockCreateParameterInfoContext createContext = new MockCreateParameterInfoContext(myFixture.getEditor(), myFixture.getFile());
    PsiElement owner = handler.findElementForParameterInfo(createContext);
    assertNotNull(owner);
    assertInstanceOf(owner, CompactCallExprImpl.class);

    Object[] items = createContext.getItemsToShow();
    assertNotNull("Items to show should not be null", items);
    assertEquals(1, items.length);
    CompactParametersDescription desc = (CompactParametersDescription) items[0];
    assertEquals("a: Uint<64>, b: Uint<64>", desc.getPresentableText());

    MockUpdateParameterInfoContext updateContext = new MockUpdateParameterInfoContext(myFixture.getEditor(), myFixture.getFile(), items);
    updateContext.setParameterOwner(owner);
    handler.updateParameterInfo(owner, updateContext);
    assertEquals("Caret is on second argument of inner call 'add'", 1, updateContext.getCurrentParameter());
  }

  public void testNestedStructLiteralInCall() {
    myFixture.configureByText(CompactFileType.INSTANCE, """
        struct Config { flag: Boolean }
        circuit init(cfg: Config): [] {}

        circuit test(): [] {
            init(Config { <caret> });
        }
        """);

    MockCreateParameterInfoContext createContext = new MockCreateParameterInfoContext(myFixture.getEditor(), myFixture.getFile());
    PsiElement owner = handler.findElementForParameterInfo(createContext);
    assertNotNull(owner);
    assertInstanceOf(owner, CompactStructLiteralExprImpl.class);

    Object[] items = createContext.getItemsToShow();
    assertNotNull("Items to show should not be null", items);
    assertEquals(1, items.length);
    CompactParametersDescription desc = (CompactParametersDescription) items[0];
    assertTrue(desc.isStructLiteral());
    assertEquals("flag: Boolean", desc.getPresentableText());
  }

  // =========================================================================
  // Mock Parameter Info Contexts for Testing
  // =========================================================================

  private static class MockCreateParameterInfoContext implements CreateParameterInfoContext {
    private final Editor editor;
    private final PsiFile file;
    private Object[] itemsToShow;
    private PsiElement highlightedElement;

    public MockCreateParameterInfoContext(Editor editor, PsiFile file) {
      this.editor = editor;
      this.file = file;
    }

    @Override
    public Object[] getItemsToShow() {
      return itemsToShow;
    }

    @Override
    public void setItemsToShow(Object[] items) {
      this.itemsToShow = items;
    }

    @Override
    public void showHint(PsiElement element, int offset, ParameterInfoHandler handler) {}

    @Override
    public int getParameterListStart() {
      return 0;
    }

    @Override
    public PsiElement getHighlightedElement() {
      return highlightedElement;
    }

    @Override
    public void setHighlightedElement(PsiElement elements) {
      this.highlightedElement = elements;
    }

    @Override
    public Project getProject() {
      return file.getProject();
    }

    @Override
    public PsiFile getFile() {
      return file;
    }

    @Override
    public int getOffset() {
      return editor.getCaretModel().getOffset();
    }

    @Override
    public @NotNull Editor getEditor() {
      return editor;
    }
  }

  private static class MockUpdateParameterInfoContext implements UpdateParameterInfoContext {
    private final Editor editor;
    private final PsiFile file;
    private final Object[] objectsToView;
    private final UserDataHolderEx customContext = new UserDataHolderBase();
    private PsiElement parameterOwner;
    private Object highlightedParameter;
    private int currentParameter = -1;
    private boolean preservedOnHintHidden = false;

    public MockUpdateParameterInfoContext(Editor editor, PsiFile file, Object[] objectsToView) {
      this.editor = editor;
      this.file = file;
      this.objectsToView = objectsToView;
    }

    @Override
    public void removeHint() {
    }

    @Override
    public void setParameterOwner(PsiElement o) {
      this.parameterOwner = o;
    }

    @Override
    public PsiElement getParameterOwner() {
      return parameterOwner;
    }

    @Override
    public void setHighlightedParameter(Object parameter) {
      this.highlightedParameter = parameter;
    }

    @Override
    public Object getHighlightedParameter() {
      return highlightedParameter;
    }

    @Override
    public void setCurrentParameter(int index) {
      this.currentParameter = index;
    }

    public int getCurrentParameter() {
      return currentParameter;
    }

    @Override
    public boolean isUIComponentEnabled(int index) {
      return true;
    }

    @Override
    public void setUIComponentEnabled(int index, boolean b) {}

    @Override
    public int getParameterListStart() {
      return 0;
    }

    @Override
    public Object[] getObjectsToView() {
      return objectsToView;
    }

    @Override
    public boolean isPreservedOnHintHidden() {
      return preservedOnHintHidden;
    }

    @Override
    public void setPreservedOnHintHidden(boolean preservedOnHintHidden) {
      this.preservedOnHintHidden = preservedOnHintHidden;
    }

    @Override
    public boolean isInnermostContext() {
      return true;
    }

    @Override
    public boolean isSingleParameterInfo() {
      return false;
    }

    @Override
    public UserDataHolderEx getCustomContext() {
      return customContext;
    }

    @Override
    public Project getProject() {
      return file.getProject();
    }

    @Override
    public PsiFile getFile() {
      return file;
    }

    @Override
    public int getOffset() {
      return editor.getCaretModel().getOffset();
    }

    @Override
    public @NotNull Editor getEditor() {
      return editor;
    }
  }

  private static class MockParameterInfoUIContext<T extends PsiElement> implements ParameterInfoUIContext {
    private final T parameterOwner;
    private int currentParameterIndex = -1;
    private String text;
    private int highlightStart = -1;
    private int highlightEnd = -1;
    private boolean uiComponentEnabled = true;

    public MockParameterInfoUIContext(T parameterOwner) {
      this.parameterOwner = parameterOwner;
    }

    @Override
    public String setupUIComponentPresentation(
        String text,
        int highlightStartOffset,
        int highlightEndOffset,
        boolean isDisabled,
        boolean strikeout,
        boolean isDisabledBeforeHighlight,
        Color background
    ) {
      this.text = text;
      this.highlightStart = highlightStartOffset;
      this.highlightEnd = highlightEndOffset;
      this.uiComponentEnabled = !isDisabled;
      return text;
    }

    @Override
    public void setupRawUIComponentPresentation(String htmlText) {
      this.text = htmlText;
    }

    @Override
    public boolean isUIComponentEnabled() {
      return uiComponentEnabled;
    }

    @Override
    public void setUIComponentEnabled(boolean enabled) {
      this.uiComponentEnabled = enabled;
    }

    @Override
    public int getCurrentParameterIndex() {
      return currentParameterIndex;
    }

    public void setCurrentParameterIndex(int index) {
      this.currentParameterIndex = index;
    }

    @Override
    public PsiElement getParameterOwner() {
      return parameterOwner;
    }

    @Override
    public boolean isSingleOverload() {
      return false;
    }

    @Override
    public boolean isSingleParameterInfo() {
      return false;
    }

    @Override
    public Color getDefaultParameterColor() {
      return Color.BLACK;
    }

    public int getHighlightStart() {
      return highlightStart;
    }

    public int getHighlightEnd() {
      return highlightEnd;
    }

  }
}
