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
 * In-editor intention action to add an explicit type annotation to an untyped {@code const} variable binding.
 *
 * <h3>IntelliJ Platform Subsystem Role & Calling Pipeline</h3>
 * <p>
 * Registered under {@code <intentionAction>} in {@code plugin.xml}:
 * <ul>
 *   <li><b>Contextual Availability:</b> Available anywhere on the {@code const} statement line
 *       or directly on the variable identifier or {@code =} token when no explicit type annotation exists.</li>
 *   <li><b>Static Type Inference:</b> Queries {@link CompactExpression#getType()} on the binding's initializer
 *       expression to infer the static type. If the type is known (not {@link CompactPrimitiveType#UNKNOWN}),
 *       the intention dynamically renders the action text (e.g. {@code "Specify type explicitly as 'Field'"}).</li>
 *   <li><b>Document Mutation:</b> Inserts {@code ": " + type.name()} immediately after the variable pattern node.</li>
 * </ul>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Read Action for Availability:</b> {@link #isAvailable} runs under a Read Action ({@code @RequiresReadLock})
 *       on the EDT.</li>
 *   <li><b>Write Action for Invocation:</b> {@link #invoke} mutates the editor {@link Document} on the EDT
 *       within the platform's default write action.</li>
 * </ul>
 *
 * @see dev.verloren.midnight.intention.CompactRemoveRedundantTypeIntention
 * @see dev.verloren.midnight.type.CompactType
 * @see dev.verloren.midnight.psi.CompactConstBindingImpl
 */
public class CompactSpecifyTypeExplicitlyIntention extends PsiElementBaseIntentionAction {

  @Override
  public @NotNull @Nls(capitalization = Nls.Capitalization.Sentence) String getFamilyName() {
    return "Compact type annotation";
  }

  /**
   * Finds the {@link CompactConstBindingImpl} associated with the given element, whether the caret
   * is directly inside the binding or anywhere on the surrounding {@code const} statement line.
   *
   * @param element the PSI element at the caret
   * @return matching {@link CompactConstBindingImpl}, or {@code null}
   */
  @Nullable
  public static CompactConstBindingImpl findConstBinding(@NotNull PsiElement element) {
    CompactConstBindingImpl binding = PsiTreeUtil.getParentOfType(element, CompactConstBindingImpl.class, false);
    if (binding != null) {
      return binding;
    }

    // Check if an element is part of the const statement (e.g. 'const' keyword, semicolon, or whitespace)
    PsiElement current = element;
    while (current != null) {
      if (current.getNode() != null && current.getNode().getElementType() == CompactElementTypes.CONST_STATEMENT) {
        return PsiTreeUtil.findChildOfType(current, CompactConstBindingImpl.class);
      }
      current = current.getParent();
    }
    return null;
  }

  /**
   * Extracts the explicit type element from a const binding pattern, if already annotated.
   *
   * @param constBinding binding to inspect
   * @return declared {@link CompactTypeElement}, or {@code null} if untyped
   */
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

  /**
   * Checks whether an untyped binding with an inferrable initializer is present under the caret.
   */
  @Override
  public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
    // Step 1: Locate enclosing const binding
    CompactConstBindingImpl binding = findConstBinding(element);
    if (binding == null) {
      return false;
    }

    // Step 2: Skip if an explicit type is already declared
    if (getDeclaredTypeElement(binding) != null) {
      return false;
    }

    // Step 3: Ensure a variable pattern and initializer expression exist
    CompactPatternImpl pattern = PsiTreeUtil.findChildOfType(binding, CompactPatternImpl.class);
    if (pattern == null) {
      return false;
    }

    CompactExpression initializer = binding.getInitializer();
    if (initializer == null) {
      return false;
    }

    // Step 4: Infer initializer type; skip if unknown
    CompactType type = initializer.getType();
    if (CompactPrimitiveType.UNKNOWN.equals(type) || type.name().isBlank()) {
      return false;
    }

    setText("Specify type explicitly as '" + type.name() + "'");
    return true;
  }

  /**
   * Inserts the explicit type annotation into the document.
   */
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
    if (CompactPrimitiveType.UNKNOWN.equals(type)) {
      return;
    }

    Document document = editor.getDocument();
    int insertOffset = pattern.getTextRange().getEndOffset();
    document.insertString(insertOffset, ": " + type.name());
  }
}
