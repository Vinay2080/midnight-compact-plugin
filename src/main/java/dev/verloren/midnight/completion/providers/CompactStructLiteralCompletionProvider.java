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
 * Completion provider for generic struct literals, sum types, and constructor helper circuits.
 *
 * <p>Implements generalized generic type argument decomposition and struct literal synthesis
 * for two-parameter sum types (e.g. {@code Either<Left, Right>}) and generic structs. Adheres to
 * the <i>Generalization Invariant</i> by ensuring type parameter parsing accurately balances nested
 * angle brackets, parentheses, and square brackets.</p>
 *
 * <h3>IntelliJ Platform Lifecycle &amp; Threading Contract</h3>
 * <ul>
 *   <li><b>Subsystem Role:</b> Domain completion provider invoked by {@link dev.verloren.midnight.completion.CompactCompletionContributor}
 *       and {@link CompactValueCompletionProvider} during expression operand completion.</li>
 *   <li><b>Calling Pipeline:</b> Invoked during background completion passes by {@code CodeCompletionHandlerBase}.</li>
 *   <li><b>Threading Model:</b> Runs within an active <b>Read Action</b> ({@code @RequiresReadLock})
 *       on background worker threads. Generates lookup elements with customized insert handlers.</li>
 *   <li><b>Cancellation Support:</b> Pure in-memory string parsing and lookup construction; no external I/O or
 *       blocking locks. Cooperatively responsive to daemon thread cancellation.</li>
 * </ul>
 *
 * <h3>Bracket Depth &amp; Generic Parsing Logic</h3>
 * <pre>{@code
 *   Type String: Either<Vector<32, Field>, Bytes<64>>
 *                         │      │
 *      depth tracker:     1──────1  (comma ignored: depth > 0)
 *                                  │
 *      root comma found:           0  (comma split: depth == 0)
 *                                  │
 *      Result: TypeArgs[ left: "Vector<32, Field>", right: "Bytes<64>" ]
 * }</pre>
 *
 * <h3>Design Decisions &amp; Architectural Rationale</h3>
 * <ul>
 *   <li><b>Generalized Invariant Parsing:</b> Rather than using fragile regular expressions that break on
 *       nested generic parameters, {@link #parseGenericTypeArgs(String, String)} performs stateful bracket
 *       depth tracking over {@code '<'}, {@code '('}, and {@code '['}.</li>
 *   <li><b>Context-Aware Typing:</b> When the contextual expected type is known (e.g. {@code Either<A, B>}),
 *       directly synthesizes fully-typed struct literals and helper constructors with corresponding types.
 *       When unconstrained, supplies generic templates with tab-stop placeholders.</li>
 *   <li><b>Memory Leak Guards:</b> Zero PSI or Project references stored as static state.</li>
 * </ul>
 *
 * @see CompactEitherInsertHandler
 * @see CompactEitherHelperInsertHandler
 * @see CompactParenthesesInsertHandler
 * @see CompactValueCompletionProvider
 */
public final class CompactStructLiteralCompletionProvider {

  /**
   * Container record holding decomposed left and right type argument strings.
   *
   * @param left  left variant type expression string
   * @param right right variant type expression string
   */
  public record TypeArgs(String left, String right) {}

  /**
   * Private constructor enforcing static utility usage.
   */
  private CompactStructLiteralCompletionProvider() {}

  /**
   * Parses the top-level two generic type arguments from a parameterized type signature string,
   * respecting nested bracket scopes across angle brackets {@code <>}, parentheses {@code ()},
   * and square brackets {@code []}.
   *
   * @param typeName   the full type string (e.g. {@code "Either<Vector<32, Field>, Bytes<64>>"})
   * @param typePrefix the generic base name prefix (e.g. {@code "Either"})
   * @return decomposed {@link TypeArgs}, or {@code null} if unparseable or not matching the prefix
   */
  public static @Nullable TypeArgs parseGenericTypeArgs(@NotNull String typeName, @NotNull String typePrefix) {
    String prefix = typePrefix + "<";
    if (!typeName.startsWith(prefix) || !typeName.endsWith(">")) {
      return null;
    }
    String inner = typeName.substring(prefix.length(), typeName.length() - 1).trim();

    // Step 1: Scan characters while tracking bracket nesting depth to find root-level comma
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

    // Step 2: Extract and trim left and right type argument segments
    String left = inner.substring(0, commaIndex).trim();
    String right = inner.substring(commaIndex + 1).trim();
    if (left.isEmpty() || right.isEmpty()) {
      return null;
    }
    return new TypeArgs(left, right);
  }

  /**
   * Specialization of {@link #parseGenericTypeArgs} for the {@code Either} sum type.
   *
   * @param typeName full type signature string
   * @return decomposed {@link TypeArgs}, or {@code null} if not an Either signature
   */
  public static @Nullable TypeArgs parseEitherTypeArgs(@NotNull String typeName) {
    return parseGenericTypeArgs(typeName, "Either");
  }

  /**
   * Main entry point generating {@code Either} struct literals and {@code left}/{@code right} helper calls.
   *
   * @param result       active completion result set
   * @param expectedType contextual expected type, or {@code null}
   * @param priority     base priority score
   */
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

  /**
   * Generates statically typed {@code Either} struct literals and constructor helpers matching {@code args}.
   *
   * @param result   the active completion result set
   * @param args     decomposed Left and Right type argument names
   * @param priority base priority score
   */
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

  /**
   * Adds typed {@code left<...>} and {@code right<...>} constructor helper lookups with custom insert handlers.
   *
   * @param result   the active completion result set
   * @param args     decomposed Left and Right type argument names
   * @param priority base priority score
   */
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

  /**
   * Helper adding a single parameterized constructor lookup element.
   *
   * @param result          active completion result set
   * @param lookupString    lookup trigger string
   * @param tailText        preview tail text
   * @param presentableText main item presentation text
   * @param handler         insert handler attached to item
   * @param priority        priority score
   */
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

  /**
   * Registers bare {@code left(val)} and {@code right(val)} calls with bare parentheses insert handlers.
   *
   * @param result   active completion result set
   * @param priority priority weight
   */
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

  /**
   * Generates unconstrained generic {@code Either<Left, Right>} struct literals and template helpers.
   *
   * @param result   active completion result set
   * @param priority base priority weight
   */
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
