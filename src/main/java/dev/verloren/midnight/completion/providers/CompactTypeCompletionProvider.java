package dev.verloren.midnight.completion.providers;

import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.PrioritizedLookupElement;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.editor.Document;
import dev.verloren.midnight.completion.CompactParameterizedTypeInsertHandler;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Completion provider for Compact built-in types, sized types, container types, and pragma directives.
 *
 * <p>Populates prioritized type lookup elements in type annotation positions (after {@code :}, {@code as},
 * {@code <}, and {@code =}) and sized numeric literals inside parameterized type arguments (such as {@code Bytes<32>}
 * or {@code Uint<64>}). Automatically attaches {@link CompactParameterizedTypeInsertHandler} to parameterized types.</p>
 *
 * <h3>IntelliJ Platform Lifecycle &amp; Threading Contract</h3>
 * <ul>
 *   <li><b>Subsystem Role:</b> Delegate type completion provider called by {@link dev.verloren.midnight.completion.CompactCompletionContributor}.</li>
 *   <li><b>Calling Pipeline:</b> Executed during asynchronous basic completion by {@code CodeCompletionHandlerBase}
 *       when the context is classified as {@code TYPE}, {@code BYTES_SIZE}, {@code UINT_SIZE}, or {@code AFTER_PRAGMA}.</li>
 *   <li><b>Threading Model:</b> Operates under a <b>Read Action</b> ({@code @RequiresReadLock}) on background pooled
 *       analysis threads. Item insertion behaviors are encapsulated within registered {@link InsertHandler} instances.</li>
 *   <li><b>Cancellation Support:</b> Operates over static data tables and fast array iterators; responsive to cooperative
 *       cancellation without locking.</li>
 * </ul>
 *
 * <h3>Type Hierarchy &amp; Categorization Pipeline</h3>
 * <pre>{@code
 *   Type Completion Category Routing
 *              │
 *              ├── Bytes Sizing ────────► Bytes<32>, Bytes<64>, Bytes<>
 *              ├── Uint Sizing  ────────► Uint<8>, Uint<16>, Uint<32>, Uint<64>, Uint<128>, Uint<256>
 *              ├── Primitives   ────────► Field, Boolean, State, Counter, Void, Addresses, Curves
 *              ├── Containers   ────────► Vector<>, Opaque<"">
 *              ├── Sum Types    ────────► Either<Left, Right>, Maybe<Type>
 *              └── Pragmas      ────────► language_version, compiler_version
 * }</pre>
 *
 * <h3>Design Decisions &amp; Architectural Rationale</h3>
 * <ul>
 *   <li><b>Pre-Configured Sized Variants:</b> In addition to bare generic types ({@code Bytes}, {@code Uint}),
 *       pre-populates canonical sizes (e.g. {@code Bytes<32>} for hashes/keys/addresses, {@code Uint<64>} for balances)
 *       with explanatory tail text so developers can complete the full type in one selection.</li>
 *   <li><b>Insert Handler Binding:</b> Automatically binds {@link CompactParameterizedTypeInsertHandler#BRACKETS}
 *       and {@link CompactParameterizedTypeInsertHandler#OPAQUE_BRACKETS} to position the caret inside the angle brackets.</li>
 *   <li><b>Memory Leak Guards:</b> All constants and lookup lists are immutable or transiently instantiated without holding
 *       references to PSI, Project, or Editor objects.</li>
 * </ul>
 *
 * @see dev.verloren.midnight.completion.CompactCompletionContributor
 * @see dev.verloren.midnight.completion.CompactParameterizedTypeInsertHandler
 * @see dev.verloren.midnight.completion.CompactCompletionContext
 */
public final class CompactTypeCompletionProvider {

  /**
   * Complete catalog of Compact built-in primitive, container, cryptographic, and sum types.
   */
  public static final String[] BUILTIN_TYPES = {
      "Boolean", "Bytes", "Field", "Opaque", "Uint", "Vector", "State", "Counter", "Void",
      "Either", "Maybe", "ContractAddress", "PublicAddress", "Cell", "Set", "Map",
      "JubjubScalar", "JubjubPoint", "Secp256k1Base", "Secp256k1Scalar", "Secp256k1Point",
      "Secp256r1", "Curve25519"
  };

  /**
   * Private constructor enforcing static utility usage.
   */
  private CompactTypeCompletionProvider() {}

  /**
   * Populates all built-in Compact primitive, sized, container, and sum types into {@code result}.
   *
   * @param result the active completion result set
   */
  public static void addBuiltinTypeCompletions(@NotNull CompletionResultSet result) {
    for (LookupElement element : createBuiltinTypeLookupElements()) {
      result.addElement(element);
    }
  }

  /**
   * Constructs the complete list of prioritized {@link LookupElement} instances for built-in Compact types.
   *
   * @return list of pre-configured type lookup elements
   */
  public static List<LookupElement> createBuiltinTypeLookupElements() {
    List<LookupElement> elements = new ArrayList<>();
    addBytesTypeLookups(elements);
    addUintTypeLookups(elements);
    addPrimitiveTypeLookups(elements);
    addContainerTypeLookups(elements);
    addSumTypeLookups(elements);
    return elements;
  }

  /**
   * Adds generic {@code Bytes} and common canonical sized types (such as {@code Bytes<32>}).
   *
   * @param elements target accumulator list
   */
  private static void addBytesTypeLookups(@NotNull List<LookupElement> elements) {
    elements.add(PrioritizedLookupElement.withPriority(
        LookupElementBuilder.create("Bytes")
            .withPresentableText("Bytes")
            .withTailText("<> (length: 32, 64, etc.)", true)
            .withTypeText("type")
            .bold()
            .withInsertHandler(CompactParameterizedTypeInsertHandler.BRACKETS),
        100.0
    ));
    elements.add(PrioritizedLookupElement.withPriority(
        LookupElementBuilder.create("Bytes<32>")
            .withPresentableText("Bytes<32>")
            .withTailText(" (32 bytes - standard hash/key/address)", true)
            .withTypeText("type")
            .bold(),
        99.0
    ));
    elements.add(PrioritizedLookupElement.withPriority(
        LookupElementBuilder.create("Bytes<64>")
            .withPresentableText("Bytes<64>")
            .withTailText(" (64 bytes - signature)", true)
            .withTypeText("type")
            .bold(),
        98.0
    ));
  }

  /**
   * Adds generic {@code Uint} and standard power-of-two bit width variants (8, 16, 32, 64, 128, 256).
   *
   * @param elements target accumulator list
   */
  private static void addUintTypeLookups(@NotNull List<LookupElement> elements) {
    elements.add(PrioritizedLookupElement.withPriority(
        LookupElementBuilder.create("Uint")
            .withPresentableText("Uint")
            .withTailText("<> (bit width: 8, 16, 32, 64, 128, 256)", true)
            .withTypeText("type")
            .bold()
            .withInsertHandler(CompactParameterizedTypeInsertHandler.BRACKETS),
        100.0
    ));
    int[] uintWidths = {8, 16, 32, 64, 128, 256};
    double uintPriority = 99.0;
    for (int width : uintWidths) {
      elements.add(PrioritizedLookupElement.withPriority(
          LookupElementBuilder.create("Uint<" + width + ">")
              .withPresentableText("Uint<" + width + ">")
              .withTailText(" (" + width + "-bit)", true)
              .withTypeText("type")
              .bold(),
          uintPriority
      ));
      uintPriority -= 0.5;
    }
  }

  /**
   * Adds simple scalar primitives, address types, state wrappers, and elliptic curve types.
   *
   * @param elements target accumulator list
   */
  private static void addPrimitiveTypeLookups(@NotNull List<LookupElement> elements) {
    String[] simpleTypes = {"Boolean", "Field", "State", "Counter", "Void", "ContractAddress",
        "PublicAddress", "Secp256r1", "Curve25519",
        "JubjubScalar", "JubjubPoint", "Secp256k1Base", "Secp256k1Scalar", "Secp256k1Point"};
    for (String simple : simpleTypes) {
      elements.add(PrioritizedLookupElement.withPriority(
          LookupElementBuilder.create(simple).withTypeText("type").bold(),
          95.0
      ));
    }
  }

  /**
   * Adds container types ({@code Vector} and {@code Opaque}) with corresponding insert handlers.
   *
   * @param elements target accumulator list
   */
  private static void addContainerTypeLookups(@NotNull List<LookupElement> elements) {
    elements.add(PrioritizedLookupElement.withPriority(
        LookupElementBuilder.create("Vector")
            .withPresentableText("Vector")
            .withTailText("<> (Vector<length, type>)", true)
            .withTypeText("type")
            .bold()
            .withInsertHandler(CompactParameterizedTypeInsertHandler.BRACKETS),
        90.0
    ));
    elements.add(PrioritizedLookupElement.withPriority(
        LookupElementBuilder.create("Opaque")
            .withPresentableText("Opaque")
            .withTailText("<> (Opaque<\"name\">)", true)
            .withTypeText("type")
            .bold()
            .withInsertHandler(CompactParameterizedTypeInsertHandler.OPAQUE_BRACKETS),
        85.0
    ));
  }

  /**
   * Adds sum types ({@code Either} and {@code Maybe}) with parameterized insert handlers.
   *
   * @param elements target accumulator list
   */
  private static void addSumTypeLookups(@NotNull List<LookupElement> elements) {
    elements.add(PrioritizedLookupElement.withPriority(
        LookupElementBuilder.create("Either")
            .withPresentableText("Either")
            .withTailText("<Left, Right>", true)
            .withTypeText("type")
            .bold()
            .withInsertHandler(CompactParameterizedTypeInsertHandler.BRACKETS),
        95.0
    ));
    elements.add(PrioritizedLookupElement.withPriority(
        LookupElementBuilder.create("Either<Bytes<32>, ContractAddress>")
            .withPresentableText("Either<Bytes<32>, ContractAddress>")
            .withTypeText("type")
            .bold(),
        90.0
    ));
    elements.add(PrioritizedLookupElement.withPriority(
        LookupElementBuilder.create("Maybe")
            .withPresentableText("Maybe")
            .withTailText("<Type>", true)
            .withTypeText("type")
            .bold()
            .withInsertHandler(CompactParameterizedTypeInsertHandler.BRACKETS),
        92.0
    ));
  }

  /**
   * Suggests standard byte count values when the cursor sits within a {@code Bytes<...>} size context.
   *
   * @param result active completion result set
   */
  public static void addBytesSizeCompletions(@NotNull CompletionResultSet result) {
    int[] sizes = {32, 64, 16, 8, 48, 20};
    String[] descs = {
        " (32 bytes - 256 bits, standard hash/key/address)",
        " (64 bytes - 512 bits, signature)",
        " (16 bytes - 128 bits)",
        " (8 bytes - 64 bits)",
        " (48 bytes - 384 bits)",
        " (20 bytes - 160 bits, Ethereum address)"
    };
    double priority = 100.0;
    for (int i = 0; i < sizes.length; i++) {
      String str = String.valueOf(sizes[i]);
      result.addElement(PrioritizedLookupElement.withPriority(
          LookupElementBuilder.create(str)
              .withPresentableText(str)
              .withTailText(descs[i], true)
              .bold(),
          priority
      ));
      priority -= 10.0;
    }
  }

  /**
   * Suggests valid unsigned integer bit-widths when inside a {@code Uint<...>} width context.
   *
   * @param result active completion result set
   */
  public static void addUintSizeCompletions(@NotNull CompletionResultSet result) {
    int[] bitWidths = {8, 16, 32, 64, 128, 256};
    double priority = 100.0;
    for (int width : bitWidths) {
      String str = String.valueOf(width);
      result.addElement(PrioritizedLookupElement.withPriority(
          LookupElementBuilder.create(str)
              .withPresentableText(str)
              .withTailText(" (" + width + "-bit unsigned integer)", true)
              .bold(),
          priority
      ));
      priority -= 5.0;
    }
  }

  /**
   * Suggests compiler and language version options immediately following a {@code pragma} directive.
   *
   * @param result active completion result set
   */
  public static void addAfterPragmaCompletions(@NotNull CompletionResultSet result) {
    result.addElement(PrioritizedLookupElement.withPriority(
        LookupElementBuilder.create("language_version")
            .withPresentableText("language_version")
            .withTailText(" >= <version>", true)
            .withTypeText("pragma")
            .bold()
            .withInsertHandler(createPragmaInsertHandler()),
        100.0
    ));

    result.addElement(PrioritizedLookupElement.withPriority(
        LookupElementBuilder.create("compiler_version")
            .withPresentableText("compiler_version")
            .withTailText(" >= <version>", true)
            .withTypeText("pragma")
            .bold()
            .withInsertHandler(createPragmaInsertHandler()),
        90.0
    ));
  }

  /**
   * Constructs an insert handler that ensures a trailing whitespace character separates pragma keywords.
   *
   * @return pragma space insert handler
   */
  public static @NotNull InsertHandler<LookupElement> createPragmaInsertHandler() {
    return (context, _) -> {
      int tailOffset = context.getTailOffset();
      Document doc = context.getDocument();
      CharSequence chars = doc.getCharsSequence();
      if (tailOffset >= chars.length() || chars.charAt(tailOffset) != ' ') {
        doc.insertString(tailOffset, " ");
        context.getEditor().getCaretModel().moveToOffset(tailOffset + 1);
      }
    };
  }
}
