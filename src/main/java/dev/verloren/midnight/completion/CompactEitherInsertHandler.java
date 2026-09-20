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
 * Insert handler for completing {@code Either} struct literals in value contexts.
 * Starts an interactive live template allowing developers to tab through
 * {@code is_left}, {@code left}, and {@code right} fields.
 */
public class CompactEitherInsertHandler implements InsertHandler<LookupElement> {

  public static final CompactEitherInsertHandler INFERRED = new CompactEitherInsertHandler(null, null, false);
  public static final CompactEitherInsertHandler INSTANCE = INFERRED;
  public static final CompactEitherInsertHandler GENERIC_PARAMETERIZED = new CompactEitherInsertHandler(null, null, true);

  private final @Nullable String leftType;
  private final @Nullable String rightType;
  private final boolean isGeneric;

  public CompactEitherInsertHandler(@Nullable String leftType, @Nullable String rightType) {
    this(leftType, rightType, false);
  }

  private CompactEitherInsertHandler(@Nullable String leftType, @Nullable String rightType, boolean isGeneric) {
    this.leftType = leftType;
    this.rightType = rightType;
    this.isGeneric = isGeneric;
  }

  @Override
  public void handleInsert(@NotNull InsertionContext context, @NotNull LookupElement item) {
    Editor editor = context.getEditor();
    Document document = context.getDocument();
    int tailOffset = context.getTailOffset();
    CharSequence chars = document.getCharsSequence();

    // Check if '<' or '{' already follows
    int offset = tailOffset;
    while (offset < chars.length() && Character.isWhitespace(chars.charAt(offset))) {
      offset++;
    }
    boolean hasAngle = offset < chars.length() && chars.charAt(offset) == '<';
    boolean hasBrace = offset < chars.length() && chars.charAt(offset) == '{';

    Project project = context.getProject();
    TemplateManager templateManager = TemplateManager.getInstance(project);
    boolean lookupIncludesAngle = item.getLookupString().startsWith("Either<");

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
