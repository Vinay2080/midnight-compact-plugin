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
import org.jetbrains.annotations.NotNull;

import java.awt.*;

/**
 * Mock Parameter Info Contexts for testing CompactParameterInfoHandler.
 */
public final class MockParameterInfoContexts {

  private MockParameterInfoContexts() {}

  public static class MockCreateParameterInfoContext implements CreateParameterInfoContext {
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
    @SuppressWarnings({"rawtypes", "RedundantSuppression"})
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

  public static class MockUpdateParameterInfoContext implements UpdateParameterInfoContext {
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

  public static class MockParameterInfoUIContext<T extends PsiElement> implements ParameterInfoUIContext {
    private final T parameterOwner;
    private int currentParameterIndex = -1;
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
      this.highlightStart = highlightStartOffset;
      this.highlightEnd = highlightEndOffset;
      this.uiComponentEnabled = !isDisabled;
      return text;
    }

    @Override
    public void setupRawUIComponentPresentation(String htmlText) {
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
