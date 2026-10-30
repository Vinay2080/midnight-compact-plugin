package dev.verloren.midnight.completion;

import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.template.Template;
import com.intellij.codeInsight.template.TemplateManager;
import com.intellij.codeInsight.template.impl.ConstantNode;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Insert handler for completing Compact {@code Either} struct literals in value contexts.
 *
 * <p>Constructs an interactive live template allowing developers to tab through
 * {@code is_left}, {@code left}, and {@code right} field initializers, automatically
 * binding either statically inferred type parameters (e.g. {@code <ZswapCoin, ContractAddress>})
 * or interactive generic placeholders.</p>
 *
 * <h3>IntelliJ Platform Lifecycle &amp; Threading Contract</h3>
 * <ul>
 *   <li><b>Subsystem Role:</b> {@link InsertHandler} implementation configured on {@code Either} struct
 *       lookup elements by {@link dev.verloren.midnight.completion.providers.CompactStructLiteralCompletionProvider}
 *       and {@link dev.verloren.midnight.completion.providers.CompactValueCompletionProvider}.</li>
 *   <li><b>Calling Pipeline:</b> Executed on the Event Dispatch Thread (EDT) by
 *       {@code com.intellij.codeInsight.completion.CodeCompletionHandlerBase} immediately upon selection.</li>
 *   <li><b>Threading Model:</b> Runs synchronously on the EDT within an active Write Action command.</li>
 *   <li><b>Headless Fallback:</b> Falls back to direct document insertion when {@link TemplateManager} is null.</li>
 * </ul>
 *
 * <h3>Design Decisions &amp; Architectural Rationale</h3>
 * <ul>
 *   <li><b>Stateless Singletons:</b> Provides {@link #INFERRED} / {@link #INSTANCE} for untyped context completions
 *       and {@link #GENERIC_PARAMETERIZED} for generic templates, avoiding object allocation overhead.</li>
 *   <li><b>Automatic Right Variant Defaults:</b> Automatically generates {@code right: default<RightType>} or
 *       {@code right: default} to ensure the resulting struct literal satisfies Compact typing invariants immediately.</li>
 *   <li><b>Memory Leak Guards:</b> Zero UI/PSI state retained in fields; holds only immutable String type names.</li>
 * </ul>
 *
 * <h3>Expansion Flow</h3>
 * <pre>{@code
 *   handleInsert(context, item)
 *              │
 *              ├── Step 1: Lookahead past whitespace for '<' and '{'
 *              │
 *              ├── Branch 1: Statically Typed (leftType != null && rightType != null)
 *              │     └── Template: <Left, Right> { is_left: true, left: , right: default<Right> }
 *              │
 *              ├── Branch 2: Generic Parameterized (isGeneric == true)
 *              │     └── Template: <$LEFT_TYPE$, $RIGHT_TYPE$> { is_left: true, left: , right: default }
 *              │
 *              └── Branch 3: Inferred / Bare
 *                    └── Template: { is_left: true, left: , right: default }
 * }</pre>
 *
 * @see CompactEitherHelperInsertHandler
 * @see dev.verloren.midnight.completion.providers.CompactStructLiteralCompletionProvider
 * @see TemplateManager
 */
public class CompactEitherInsertHandler implements InsertHandler<LookupElement> {

  /** Singleton insert handler for the inferred context where angle brackets are omitted or already exist. */
  public static final CompactEitherInsertHandler INFERRED = new CompactEitherInsertHandler(null, null, false);

  /** Default alias for {@link #INFERRED}. */
  public static final CompactEitherInsertHandler INSTANCE = INFERRED;

  /** Singleton insert handler generating interactive {@code <Field, Boolean>} generic type tab stops. */
  public static final CompactEitherInsertHandler GENERIC_PARAMETERIZED = new CompactEitherInsertHandler(null, null, true);

  /** Expected concrete left variant type name, or {@code null}. */
  private final @Nullable String leftType;

  /** Expected concrete right variant type name, or {@code null}. */
  private final @Nullable String rightType;

  /** Whether generic type placeholder variables should be emitted. */
  private final boolean isGeneric;

  /**
   * Constructs a typed Either insert handler with known variant types.
   *
   * @param leftType  the left variant type name
   * @param rightType the right variant type name
   */
  public CompactEitherInsertHandler(@Nullable String leftType, @Nullable String rightType) {
    this(leftType, rightType, false);
  }

  /**
   * Internal constructor for typed or generic variations.
   */
  private CompactEitherInsertHandler(@Nullable String leftType, @Nullable String rightType, boolean isGeneric) {
    this.leftType = leftType;
    this.rightType = rightType;
    this.isGeneric = isGeneric;
  }

  /**
   * Performs document modification and live template instantiation for the Either literal.
   *
   * @param context insertion context containing editor and document state
   * @param item    selected lookup item
   */
  @Override
  public void handleInsert(@NotNull InsertionContext context, @NotNull LookupElement item) {
    Editor editor = context.getEditor();
    Document document = context.getDocument();
    int tailOffset = context.getTailOffset();
    CharSequence chars = document.getCharsSequence();

    // Step 1: Lookahead forward past whitespace to check if '<' or '{' already follows
    int offset = tailOffset;
    while (offset < chars.length() && Character.isWhitespace(chars.charAt(offset))) {
      offset++;
    }
    boolean hasAngle = offset < chars.length() && chars.charAt(offset) == '<';
    boolean hasBrace = offset < chars.length() && chars.charAt(offset) == '{';

    Project project = context.getProject();
    TemplateManager templateManager = TemplateManager.getInstance(project);
    boolean lookupIncludesAngle = item.getLookupString().startsWith("Either<");

    // Step 2: Handle statically typed Either literal expansion
    if (leftType != null && rightType != null) {
      if (templateManager != null) {
        Template template = templateManager.createTemplate("", "");
        template.setToReformat(false);
        if (!lookupIncludesAngle && !hasAngle) {
          template.addTextSegment("<" + leftType + ", " + rightType + ">");
        }
        if (!hasBrace) {
          appendStructLiteralBody(template, "default<" + rightType + ">");
        }
        template.addEndVariable();
        templateManager.startTemplate(editor, template);
      } else {
        String snippet = (!lookupIncludesAngle && !hasAngle ? "<" + leftType + ", " + rightType + ">" : "")
            + (!hasBrace ? " { is_left: true, left: , right: default<" + rightType + "> }" : "");
        document.insertString(tailOffset, snippet);
      }
    } else if (isGeneric) {
      // Step 3: Handle generic parameterized Either literal with interactive type variables
      if (templateManager != null) {
        Template template = templateManager.createTemplate("", "");
        template.setToReformat(false);
        if (!lookupIncludesAngle && !hasAngle) {
          template.addTextSegment("<");
          template.addVariable("LEFT_TYPE", new ConstantNode("Field"), true);
          template.addTextSegment(", ");
          template.addVariable("RIGHT_TYPE", new ConstantNode("Boolean"), true);
          template.addTextSegment(">");
        }
        if (!hasBrace) {
          appendStructLiteralBody(template, "default");
        }
        template.addEndVariable();
        templateManager.startTemplate(editor, template);
      } else {
        String snippet = (!lookupIncludesAngle && !hasAngle ? "<Field, Boolean>" : "")
            + (!hasBrace ? " { is_left: true, left: , right: default }" : "");
        document.insertString(tailOffset, snippet);
      }
    } else {
      // Step 4: Handle bare inferred Either literal without angle brackets
      if (!hasBrace) {
        if (templateManager != null) {
          Template template = templateManager.createTemplate("", "");
          template.setToReformat(false);
          appendStructLiteralBody(template, "default");
          template.addEndVariable();
          templateManager.startTemplate(editor, template);
        } else {
          document.insertString(tailOffset, " { is_left: true, left: , right: default }");
        }
      }
    }
  }

  /**
   * Appends the common {@code { is_left: ..., left: ..., right: ... }} struct literal template segments.
   *
   * @param template          the target live template builder
   * @param rightDefaultValue default expression for the unselected right variant
   */
  private static void appendStructLiteralBody(@NotNull Template template, @NotNull String rightDefaultValue) {
    template.addTextSegment(" { is_left: ");
    template.addVariable("IS_LEFT", new ConstantNode("true"), true);
    template.addTextSegment(", left: ");
    template.addVariable("LEFT", new ConstantNode(""), true);
    template.addTextSegment(", right: ");
    template.addVariable("RIGHT", new ConstantNode(rightDefaultValue), true);
    template.addTextSegment(" }");
  }
}
