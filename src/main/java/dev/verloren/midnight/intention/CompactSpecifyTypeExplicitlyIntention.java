package dev.verloren.midnight.intention;

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import dev.verloren.midnight.parser.CompactElementTypes;
import dev.verloren.midnight.psi.CompactConstBindingImpl;
import dev.verloren.midnight.psi.CompactExpression;
import dev.verloren.midnight.psi.CompactPatternImpl;
import dev.verloren.midnight.psi.CompactTypeElement;
import dev.verloren.midnight.type.CompactPrimitiveType;
import dev.verloren.midnight.type.CompactType;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * In-editor intention to add an explicit type annotation to an untyped {@code const} binding.
 *
 * <p>Available anywhere on the {@code const} statement line (general suggestion), as well as directly
 * on the variable name or assignment operator.</p>
 */
public class CompactSpecifyTypeExplicitlyIntention extends PsiElementBaseIntentionAction {

  @Override
  public @NotNull @Nls(capitalization = Nls.Capitalization.Sentence) String getFamilyName() {
    return "Compact type annotation";
  }

  @Override
  public boolean startInWriteAction() {
    return true;
  }

  /**
   * Finds the {@link CompactConstBindingImpl} associated with the given element, whether the caret
   * is directly inside the binding or anywhere on the surrounding {@code const} statement line.
   */
  @Nullable
  public static CompactConstBindingImpl findConstBinding(@NotNull PsiElement element) {
    CompactConstBindingImpl binding = PsiTreeUtil.getParentOfType(element, CompactConstBindingImpl.class, false);
    if (binding != null) {
      return binding;
    }

    // Check if element is part of the const statement (e.g. 'const' keyword, semicolon, or whitespace)
    PsiElement current = element;
    while (current != null) {
      if (current.getNode() != null && current.getNode().getElementType() == CompactElementTypes.CONST_STATEMENT) {
        return PsiTreeUtil.findChildOfType(current, CompactConstBindingImpl.class);
      }
      current = current.getParent();
    }
    return null;
  }

  @Nullable
  public static CompactTypeElement getDeclaredTypeElement(@NotNull CompactConstBindingImpl constBinding) {
    for (PsiElement child : constBinding.getChildren()) {
      if (child.getNode().getElementType() == CompactElementTypes.OPTIONALLY_TYPED_PATTERN
          || child.getNode().getElementType() == CompactElementTypes.TYPED_PATTERN) {
        for (PsiElement sub : child.getChildren()) {
          if (sub instanceof CompactTypeElement && !(sub instanceof CompactPatternImpl)) {
            return (CompactTypeElement) sub;
          }
        }
      }
    }
    return null;
  }

  @Override
  public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
    CompactConstBindingImpl binding = findConstBinding(element);
    if (binding == null) {
      return false;
    }

    if (getDeclaredTypeElement(binding) != null) {
      return false;
    }

    CompactPatternImpl pattern = PsiTreeUtil.findChildOfType(binding, CompactPatternImpl.class);
    if (pattern == null) {
      return false;
    }

    CompactExpression initializer = binding.getInitializer();
    if (initializer == null) {
      return false;
    }

    CompactType type = initializer.getType();
    if (type == null || CompactPrimitiveType.UNKNOWN.equals(type) || type.name().isBlank()) {
      return false;
    }

    setText("Specify type explicitly as '" + type.name() + "'");
    return true;
  }

  @Override
  public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element) throws IncorrectOperationException {
    CompactConstBindingImpl binding = findConstBinding(element);
    if (binding == null) {
      return;
    }

    CompactPatternImpl pattern = PsiTreeUtil.findChildOfType(binding, CompactPatternImpl.class);
    CompactExpression initializer = binding.getInitializer();
    if (pattern == null || initializer == null) {
      return;
    }

    CompactType type = initializer.getType();
    if (type == null || CompactPrimitiveType.UNKNOWN.equals(type)) {
      return;
    }

    Document document = editor.getDocument();
    int insertOffset = pattern.getTextRange().getEndOffset();
    document.insertString(insertOffset, ": " + type.name());
  }
}
