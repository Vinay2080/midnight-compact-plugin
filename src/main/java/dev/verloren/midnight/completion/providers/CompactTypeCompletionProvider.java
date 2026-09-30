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
 * Completion provider for Compact builtin types, sized types, and pragma directives.
 */
public final class CompactTypeCompletionProvider {

  public static final String[] BUILTIN_TYPES = {
      "Boolean", "Bytes", "Field", "Opaque", "Uint", "Vector", "State", "Counter", "Void",
      "Either", "Maybe", "ContractAddress", "Cell", "Set", "Map",
      "JubjubScalar", "JubjubPoint", "Secp256k1Base", "Secp256k1Scalar", "Secp256k1Point"
  };

  private CompactTypeCompletionProvider() {}

  public static void addBuiltinTypeCompletions(@NotNull CompletionResultSet result) {
    for (LookupElement element : createBuiltinTypeLookupElements()) {
      result.addElement(element);
    }
  }

  public static List<LookupElement> createBuiltinTypeLookupElements() {
    List<LookupElement> elements = new ArrayList<>();
    addBytesTypeLookups(elements);
    addUintTypeLookups(elements);
    addPrimitiveTypeLookups(elements);
    addContainerTypeLookups(elements);
    addSumTypeLookups(elements);
    return elements;
  }

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

  private static void addPrimitiveTypeLookups(@NotNull List<LookupElement> elements) {
    String[] simpleTypes = {"Boolean", "Field", "State", "Counter", "Void", "ContractAddress",
        "JubjubScalar", "JubjubPoint", "Secp256k1Base", "Secp256k1Scalar", "Secp256k1Point"};
    for (String simple : simpleTypes) {
      elements.add(PrioritizedLookupElement.withPriority(
          LookupElementBuilder.create(simple).withTypeText("type").bold(),
          95.0
      ));
    }
  }

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
