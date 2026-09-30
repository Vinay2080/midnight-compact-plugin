package dev.verloren.midnight.completion.providers;

import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.PrioritizedLookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import dev.verloren.midnight.completion.CompactEitherHelperInsertHandler;
import dev.verloren.midnight.completion.CompactEitherInsertHandler;
import dev.verloren.midnight.completion.CompactParenthesesInsertHandler;
import dev.verloren.midnight.type.CompactType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Completion provider for generic struct literals and helper constructor expressions.
 *
 * <p>Implements generalized generic argument parsing and struct literal completions
 * for two-parameter sum types and structs, satisfying the Generalization Invariant.</p>
 */
public final class CompactStructLiteralCompletionProvider {

  public record TypeArgs(String left, String right) {}

  private CompactStructLiteralCompletionProvider() {}

  public static @Nullable TypeArgs parseGenericTypeArgs(@NotNull String typeName, @NotNull String typePrefix) {
    String prefix = typePrefix + "<";
    if (!typeName.startsWith(prefix) || !typeName.endsWith(">")) {
      return null;
    }
    String inner = typeName.substring(prefix.length(), typeName.length() - 1).trim();
    int depth = 0;
    int commaIndex = -1;
    for (int i = 0; i < inner.length(); i++) {
      char c = inner.charAt(i);
      if (c == '<' || c == '(' || c == '[') {
        depth++;
      } else if (c == '>' || c == ')' || c == ']') {
        depth--;
      } else if (c == ',' && depth == 0) {
        commaIndex = i;
        break;
      }
    }
    if (commaIndex == -1) {
      return null;
    }
    String left = inner.substring(0, commaIndex).trim();
    String right = inner.substring(commaIndex + 1).trim();
    if (left.isEmpty() || right.isEmpty()) {
      return null;
    }
    return new TypeArgs(left, right);
  }

  public static @Nullable TypeArgs parseEitherTypeArgs(@NotNull String typeName) {
    return parseGenericTypeArgs(typeName, "Either");
  }

  public static void addEitherAndHelperCompletions(
      @NotNull CompletionResultSet result,
      @Nullable CompactType expectedType,
      double priority
  ) {
    TypeArgs args = expectedType != null ? parseEitherTypeArgs(expectedType.name()) : null;
    if (args != null) {
      addTypedEitherCompletions(result, args, priority);
    } else {
      addGenericEitherCompletions(result, priority);
    }
  }

  private static void addTypedEitherCompletions(
      @NotNull CompletionResultSet result,
      @NotNull TypeArgs args,
      double priority
  ) {
    String fullType = "Either<" + args.left() + ", " + args.right() + ">";
    CompactEitherInsertHandler typedHandler = new CompactEitherInsertHandler(args.left(), args.right());
    result.addElement(PrioritizedLookupElement.withPriority(
        LookupElementBuilder.create(fullType)
            .withPresentableText(fullType)
            .withTailText(" { is_left: true, left: ..., right: default<" + args.right() + "> }", true)
            .withTypeText("struct")
            .bold()
            .withInsertHandler(typedHandler),
        priority
    ));
    result.addElement(PrioritizedLookupElement.withPriority(
        LookupElementBuilder.create("Either")
            .withPresentableText(fullType)
            .withTailText(" { is_left: true, left: ..., right: default<" + args.right() + "> }", true)
            .withTypeText("struct")
            .bold()
            .withInsertHandler(typedHandler),
        priority
    ));
    result.addElement(PrioritizedLookupElement.withPriority(
        LookupElementBuilder.create("Either")
            .withPresentableText("Either")
            .withTailText(" { is_left: true, left: ..., right: default }", true)
            .withTypeText("struct")
            .bold()
            .withInsertHandler(CompactEitherInsertHandler.INFERRED),
        priority - 5.0
    ));
    addTypedHelperLookups(result, args, priority);
  }

  private static void addTypedHelperLookups(
      @NotNull CompletionResultSet result,
      @NotNull TypeArgs args,
      double priority
  ) {
    String typedLeft = "left<" + args.left() + ", " + args.right() + ">";
    String typedRight = "right<" + args.left() + ", " + args.right() + ">";
    CompactEitherHelperInsertHandler typedHelperHandler = new CompactEitherHelperInsertHandler(args.left(), args.right());

    addHelperLookup(result, typedRight, "(value: " + args.right() + ")", typedRight, typedHelperHandler, priority);
    addHelperLookup(result, "right", "(value: " + args.right() + ")", typedRight, typedHelperHandler, priority);
    addHelperLookup(result, typedLeft, "(value: " + args.left() + ")", typedLeft, typedHelperHandler, priority);
    addHelperLookup(result, "left", "(value: " + args.left() + ")", typedLeft, typedHelperHandler, priority);
    addBareHelperLookups(result, priority - 5.0);
  }

  private static void addHelperLookup(
      @NotNull CompletionResultSet result,
      @NotNull String lookupString,
      @NotNull String tailText,
      @NotNull String presentableText,
      @NotNull CompactEitherHelperInsertHandler handler,
      double priority
  ) {
    result.addElement(PrioritizedLookupElement.withPriority(
        LookupElementBuilder.create(lookupString)
            .withPresentableText(presentableText)
            .withTailText(tailText, true)
            .withTypeText("Either")
            .bold()
            .withInsertHandler(handler),
        priority
    ));
  }

  private static void addBareHelperLookups(@NotNull CompletionResultSet result, double priority) {
    result.addElement(PrioritizedLookupElement.withPriority(
        LookupElementBuilder.create("left")
            .withPresentableText("left")
            .withTailText("(val)", true)
            .withTypeText("Either")
            .bold()
            .withInsertHandler(CompactParenthesesInsertHandler.WITH_PARENS),
        priority
    ));
    result.addElement(PrioritizedLookupElement.withPriority(
        LookupElementBuilder.create("right")
            .withPresentableText("right")
            .withTailText("(val)", true)
            .withTypeText("Either")
            .bold()
            .withInsertHandler(CompactParenthesesInsertHandler.WITH_PARENS),
        priority
    ));
  }

  private static void addGenericEitherCompletions(@NotNull CompletionResultSet result, double priority) {
    result.addElement(PrioritizedLookupElement.withPriority(
        LookupElementBuilder.create("Either<Left, Right>")
            .withPresentableText("Either<Left, Right>")
            .withTailText(" { is_left: true, left: ..., right: default }", true)
            .withTypeText("struct")
            .bold()
            .withInsertHandler(CompactEitherInsertHandler.GENERIC_PARAMETERIZED),
        priority
    ));
    result.addElement(PrioritizedLookupElement.withPriority(
        LookupElementBuilder.create("Either")
            .withPresentableText("Either")
            .withTailText(" { is_left: true, left: ..., right: default }", true)
            .withTypeText("struct")
            .bold()
            .withInsertHandler(CompactEitherInsertHandler.INFERRED),
        priority
    ));
    result.addElement(PrioritizedLookupElement.withPriority(
        LookupElementBuilder.create("left<Left, Right>")
            .withPresentableText("left<Left, Right>")
            .withTailText("(val)", true)
            .withTypeText("Either")
            .bold()
            .withInsertHandler(new CompactEitherHelperInsertHandler(true)),
        priority
    ));
    result.addElement(PrioritizedLookupElement.withPriority(
        LookupElementBuilder.create("right<Left, Right>")
            .withPresentableText("right<Left, Right>")
            .withTailText("(val)", true)
            .withTypeText("Either")
            .bold()
            .withInsertHandler(new CompactEitherHelperInsertHandler(true)),
        priority
    ));
    addBareHelperLookups(result, priority);
  }
}
