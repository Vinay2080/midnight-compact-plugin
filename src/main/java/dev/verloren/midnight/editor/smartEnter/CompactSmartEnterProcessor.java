package dev.verloren.midnight.editor.smartEnter;

import com.intellij.codeInsight.AutoPopupController;
import com.intellij.codeInsight.editorActions.smartEnter.SmartEnterProcessor;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import dev.verloren.midnight.psi.CompactFile;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

/**
 * Smart Enter processor (Ctrl+Shift+Enter / Cmd+Shift+Enter) for the Compact smart contract language.
 *
 * <p>Completes statements, circuit signatures, contract/struct blocks, control-flow statements,
 * and automatically appends closing parentheses, braces, and semicolons without hardcoding or
 * corrupting developer semantic intent.</p>
 */
public class CompactSmartEnterProcessor extends SmartEnterProcessor {

  private static final Pattern CIRCUIT_PATTERN = Pattern.compile("^.*\\bcircuit\\s+([a-zA-Z0-9_]+).*$");
  private static final Pattern CONSTRUCTOR_PATTERN = Pattern.compile("^.*\\bconstructor(\\s*\\(.*)?$");
  private static final Pattern BLOCK_DECL_PATTERN = Pattern.compile("^.*\\b(contract|struct|enum|module)\\s+([a-zA-Z0-9_]+).*$");
  private static final Pattern BARE_DECL_PATTERN = Pattern.compile("^(export\\s+)?(contract|struct|enum|module|circuit|constructor)$");
  private static final Pattern CONST_STATEMENT_PATTERN = Pattern.compile("^.*\\bconst\\s+.*$");
  private static final Pattern STATEMENT_START_PATTERN = Pattern.compile("^(return|ledger|type|import|include|assert|emit)\\b.*$");

  @Override
  public boolean process(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile psiFile) {
    if (!(psiFile instanceof CompactFile)) {
      return false;
    }

    Document document = editor.getDocument();
    PsiDocumentManager.getInstance(project).commitDocument(document);

    int caretOffset = editor.getCaretModel().getOffset();
    int lineNumber = document.getLineNumber(caretOffset);
    int lineStart = document.getLineStartOffset(lineNumber);
    int lineEnd = document.getLineEndOffset(lineNumber);

    String lineText = document.getText(new TextRange(lineStart, lineEnd));
    String trimmed = lineText.trim();

    if (trimmed.isEmpty()) {
      return false;
    }

    // Determine current line indentation
    int firstNonWs = 0;
    while (firstNonWs < lineText.length() && Character.isWhitespace(lineText.charAt(firstNonWs))) {
      firstNonWs++;
    }
    String indent = lineText.substring(0, firstNonWs);
    String innerIndent = indent + "  ";

    // Trim trailing whitespace from lineEnd so additions are cleanly appended to the code
    while (lineEnd > lineStart && Character.isWhitespace(document.getCharsSequence().charAt(lineEnd - 1))) {
      lineEnd--;
    }

    final int finalLineEnd = lineEnd;

    WriteCommandAction.runWriteCommandAction(project, "Compact Smart Enter", null, () -> {
      // 1. If statement completion
      if (trimmed.startsWith("if ") || trimmed.startsWith("if(")) {
        handleIfStatement(document, editor, lineNumber, finalLineEnd, trimmed, indent, innerIndent);
        return;
      }

      // 2. For statement completion
      if (trimmed.startsWith("for ") || trimmed.startsWith("for(")) {
        handleForStatement(document, editor, lineNumber, finalLineEnd, trimmed, indent, innerIndent);
        return;
      }

      // 3. Circuit definition completion
      if (CIRCUIT_PATTERN.matcher(trimmed).matches()) {
        handleCircuitDefinition(project, document, editor, lineNumber, finalLineEnd, trimmed, indent, innerIndent);
        return;
      }

      // 4. Constructor declaration completion
      if (CONSTRUCTOR_PATTERN.matcher(trimmed).matches()) {
        handleConstructorDeclaration(document, editor, lineNumber, finalLineEnd, trimmed, indent, innerIndent);
        return;
      }

      // 5. Contract, Struct, Enum, Module declaration completion with identifier
      if (BLOCK_DECL_PATTERN.matcher(trimmed).matches()) {
        handleBlockDeclaration(document, editor, lineNumber, finalLineEnd, trimmed, indent, innerIndent);
        return;
      }

      // 6. Bare declaration keyword (e.g. "contract", "enum", "struct" without an identifier yet)
      // Never append a semicolon! Instead, ensure trailing space exists and wait for identifier.
      if (BARE_DECL_PATTERN.matcher(trimmed).matches()) {
        if (!lineText.endsWith(" ")) {
          document.insertString(finalLineEnd, " ");
          editor.getCaretModel().moveToOffset(finalLineEnd + 1);
        } else {
          editor.getCaretModel().moveToOffset(finalLineEnd);
        }
        return;
      }

      // 7. Const statement completion: must ensure '=' exists before inserting ';'
      if (CONST_STATEMENT_PATTERN.matcher(trimmed).matches()) {
        handleConstStatement(project, document, editor, lineNumber, finalLineEnd, trimmed, indent);
        return;
      }

      // 8. Witness declaration completion
      if (trimmed.contains("witness ") || trimmed.startsWith("witness ")) {
        handleWitnessDeclaration(project, document, editor, lineNumber, finalLineEnd, trimmed, indent);
        return;
      }

      // 9. Type alias completion
      if (trimmed.startsWith("type ") || trimmed.contains(" type ")) {
        handleTypeAliasDeclaration(project, document, editor, lineNumber, finalLineEnd, trimmed, indent);
        return;
      }

      // 10. Statement semicolon completion (return, ledger, assert, etc.)
      if (STATEMENT_START_PATTERN.matcher(trimmed).matches()) {
        handleStatementSemicolon(document, editor, lineNumber, finalLineEnd, trimmed, indent);
        return;
      }

      // 11. General fallback: if line doesn't end with semicolon or brace, append semicolon
      if (!trimmed.endsWith(";") && !trimmed.endsWith("{") && !trimmed.endsWith("}")) {
        document.insertString(finalLineEnd, ";\n" + indent);
        editor.getCaretModel().moveToOffset(finalLineEnd + 2 + indent.length());
      } else if (trimmed.endsWith(";")) {
        document.insertString(finalLineEnd, "\n" + indent);
        editor.getCaretModel().moveToOffset(finalLineEnd + 1 + indent.length());
      }
    });

    return true;
  }

  private void handleIfStatement(Document doc, Editor editor, int lineNumber, int lineEnd,
                                  String trimmed, String indent, String innerIndent) {
    if (trimmed.endsWith("{")) {
      return;
    }

    String addition;
    if (!trimmed.contains(")")) {
      // Missing closing parenthesis and body
      addition = ") {\n" + innerIndent + "\n" + indent + "}";
    } else {
      // Has closing parenthesis, missing body
      addition = " {\n" + innerIndent + "\n" + indent + "}";
    }

    doc.insertString(lineEnd, addition);
    int caretTarget = lineEnd + addition.indexOf('\n') + 1 + innerIndent.length();
    editor.getCaretModel().moveToOffset(caretTarget);
  }

  private void handleForStatement(Document doc, Editor editor, int lineNumber, int lineEnd,
                                   String trimmed, String indent, String innerIndent) {
    if (trimmed.endsWith("{")) {
      return;
    }

    String addition;
    if (!trimmed.contains(")")) {
      addition = ") {\n" + innerIndent + "\n" + indent + "}";
    } else {
      addition = " {\n" + innerIndent + "\n" + indent + "}";
    }

    doc.insertString(lineEnd, addition);
    int caretTarget = lineEnd + addition.indexOf('\n') + 1 + innerIndent.length();
    editor.getCaretModel().moveToOffset(caretTarget);
  }

  /**
   * Handles circuit signature completion without hardcoding return types when the user has typed ':'.
   */
  private void handleCircuitDefinition(Project project, Document doc, Editor editor, int lineNumber, int lineEnd,
                                       String trimmed, String indent, String innerIndent) {
    if (trimmed.endsWith("{")) {
      return;
    }

    // Case A: Missing parameter parentheses entirely: e.g. "circuit transfer"
    if (!trimmed.contains("(")) {
      String addition = "(): Void {\n" + innerIndent + "\n" + indent + "}";
      doc.insertString(lineEnd, addition);
      int caretTarget = lineEnd + addition.indexOf('\n') + 1 + innerIndent.length();
      editor.getCaretModel().moveToOffset(caretTarget);
      return;
    }

    // Case B: Unclosed parameter list: e.g. "circuit transfer("
    if (trimmed.endsWith("(")) {
      String addition = "): Void {\n" + innerIndent + "\n" + indent + "}";
      doc.insertString(lineEnd, addition);
      int caretTarget = lineEnd + addition.indexOf('\n') + 1 + innerIndent.length();
      editor.getCaretModel().moveToOffset(caretTarget);
      return;
    }

    // Case C: User finished parameter list but did NOT type a colon: e.g. "circuit transfer(to: Address)"
    // In Compact, an omitted return type defaults to : Void.
    if (trimmed.endsWith(")")) {
      String addition = ": Void {\n" + innerIndent + "\n" + indent + "}";
      doc.insertString(lineEnd, addition);
      int caretTarget = lineEnd + addition.indexOf('\n') + 1 + innerIndent.length();
      editor.getCaretModel().moveToOffset(caretTarget);
      return;
    }

    // Case D: User typed a colon ':' or ': ' with no type specified yet.
    // NEVER hardcode 'Void'! The user explicitly typed ':' to specify a custom return type.
    // Ensure clean spacing and leave caret positioned after ': ' for typing / auto-popup.
    int colonIndex = trimmed.lastIndexOf(':');
    String afterColon = colonIndex != -1 ? trimmed.substring(colonIndex + 1).trim() : "";

    if (afterColon.isEmpty()) {
      int docColon = doc.getCharsSequence().subSequence(0, lineEnd).toString().lastIndexOf(':');
      if (docColon != -1) {
        if (lineEnd == docColon + 1) {
          doc.insertString(docColon + 1, " ");
          editor.getCaretModel().moveToOffset(docColon + 2);
        } else {
          editor.getCaretModel().moveToOffset(lineEnd);
        }
        AutoPopupController.getInstance(project).scheduleAutoPopup(editor);
        return;
      }
    }

    // Case E: User typed an incomplete type after colon
    // In Compact, Bytes and Uint are ALWAYS parameterized with angle brackets <...>, e.g. Bytes<32>, Uint<64>!
    if (afterColon.equals("Byte") || afterColon.equals("Bytes")) {
      String replacement = "Bytes<>";
      int startTypeOffset = lineEnd - afterColon.length();
      doc.replaceString(startTypeOffset, lineEnd, replacement);
      editor.getCaretModel().moveToOffset(startTypeOffset + "Bytes<".length());
      return;
    }

    if (afterColon.equals("Uint")) {
      String replacement = "Uint<>";
      int startTypeOffset = lineEnd - afterColon.length();
      doc.replaceString(startTypeOffset, lineEnd, replacement);
      editor.getCaretModel().moveToOffset(startTypeOffset + "Uint<".length());
      return;
    }

    if (afterColon.equals("Vector")) {
      String replacement = "Vector<>";
      int startTypeOffset = lineEnd - afterColon.length();
      doc.replaceString(startTypeOffset, lineEnd, replacement);
      editor.getCaretModel().moveToOffset(startTypeOffset + "Vector<".length());
      return;
    }

    // e.g. Unclosed angle bracket: "circuit transfer(): Bytes<32" or "circuit transfer(): Uint<64"
    if (afterColon.contains("<") && !afterColon.contains(">")) {
      String addition = "> {\n" + innerIndent + "\n" + indent + "}";
      doc.insertString(lineEnd, addition);
      int caretTarget = lineEnd + addition.indexOf('\n') + 1 + innerIndent.length();
      editor.getCaretModel().moveToOffset(caretTarget);
      return;
    }

    // e.g. Unclosed tuple: "circuit transfer(): [Field, Boolean"
    if (afterColon.contains("[") && !afterColon.contains("]")) {
      String addition = "] {\n" + innerIndent + "\n" + indent + "}";
      doc.insertString(lineEnd, addition);
      int caretTarget = lineEnd + addition.indexOf('\n') + 1 + innerIndent.length();
      editor.getCaretModel().moveToOffset(caretTarget);
      return;
    }

    // Case F: Complete return type present (e.g. "circuit query(): Uint<64>", "circuit query(): Bytes<32>", "circuit query(): Field", etc.)
    String addition = " {\n" + innerIndent + "\n" + indent + "}";
    doc.insertString(lineEnd, addition);
    int caretTarget = lineEnd + addition.indexOf('\n') + 1 + innerIndent.length();
    editor.getCaretModel().moveToOffset(caretTarget);
  }

  private void handleConstructorDeclaration(Document doc, Editor editor, int lineNumber, int lineEnd,
                                            String trimmed, String indent, String innerIndent) {
    if (trimmed.endsWith("{")) {
      return;
    }

    String addition;
    if (!trimmed.contains("(")) {
      addition = "() {\n" + innerIndent + "\n" + indent + "}";
    } else if (trimmed.endsWith("(")) {
      addition = ") {\n" + innerIndent + "\n" + indent + "}";
    } else {
      addition = " {\n" + innerIndent + "\n" + indent + "}";
    }

    doc.insertString(lineEnd, addition);
    int caretTarget = lineEnd + addition.indexOf('\n') + 1 + innerIndent.length();
    editor.getCaretModel().moveToOffset(caretTarget);
  }

  private void handleBlockDeclaration(Document doc, Editor editor, int lineNumber, int lineEnd,
                                      String trimmed, String indent, String innerIndent) {
    if (trimmed.endsWith("{")) {
      return;
    }

    String addition = " {\n" + innerIndent + "\n" + indent + "}";
    doc.insertString(lineEnd, addition);
    int caretTarget = lineEnd + addition.indexOf('\n') + 1 + innerIndent.length();
    editor.getCaretModel().moveToOffset(caretTarget);
  }

  /**
   * Handles const statements:
   * In Compact, const declarations MUST have an initializer expression '='.
   * If '=' is missing, append ' = ' and move caret there rather than appending ';'.
   */
  private void handleConstStatement(Project project, Document doc, Editor editor, int lineNumber, int lineEnd,
                                    String trimmed, String indent) {
    if (!trimmed.contains("=")) {
      doc.insertString(lineEnd, " = ");
      editor.getCaretModel().moveToOffset(lineEnd + 3);
      AutoPopupController.getInstance(project).scheduleAutoPopup(editor);
      return;
    }

    if (trimmed.endsWith("=")) {
      doc.insertString(lineEnd, " ");
      editor.getCaretModel().moveToOffset(lineEnd + 1);
      AutoPopupController.getInstance(project).scheduleAutoPopup(editor);
      return;
    }

    if (!trimmed.endsWith(";")) {
      doc.insertString(lineEnd, ";\n" + indent);
      editor.getCaretModel().moveToOffset(lineEnd + 2 + indent.length());
    } else {
      doc.insertString(lineEnd, "\n" + indent);
      editor.getCaretModel().moveToOffset(lineEnd + 1 + indent.length());
    }
  }

  /**
   * Handles type aliases: e.g. "type Balance = Uint<64>;"
   */
  private void handleTypeAliasDeclaration(Project project, Document doc, Editor editor, int lineNumber, int lineEnd,
                                          String trimmed, String indent) {
    if (!trimmed.contains("=")) {
      doc.insertString(lineEnd, " = ");
      editor.getCaretModel().moveToOffset(lineEnd + 3);
      AutoPopupController.getInstance(project).scheduleAutoPopup(editor);
      return;
    }

    if (trimmed.endsWith("=")) {
      doc.insertString(lineEnd, " ");
      editor.getCaretModel().moveToOffset(lineEnd + 1);
      AutoPopupController.getInstance(project).scheduleAutoPopup(editor);
      return;
    }

    if (!trimmed.endsWith(";")) {
      doc.insertString(lineEnd, ";\n" + indent);
      editor.getCaretModel().moveToOffset(lineEnd + 2 + indent.length());
    } else {
      doc.insertString(lineEnd, "\n" + indent);
      editor.getCaretModel().moveToOffset(lineEnd + 1 + indent.length());
    }
  }

  private void handleWitnessDeclaration(Project project, Document doc, Editor editor, int lineNumber, int lineEnd,
                                        String trimmed, String indent) {
    if (trimmed.endsWith(":")) {
      doc.insertString(lineEnd, " ");
      editor.getCaretModel().moveToOffset(lineEnd + 1);
      AutoPopupController.getInstance(project).scheduleAutoPopup(editor);
      return;
    }

    if (!trimmed.endsWith(";")) {
      doc.insertString(lineEnd, ";\n" + indent);
      editor.getCaretModel().moveToOffset(lineEnd + 2 + indent.length());
    } else {
      doc.insertString(lineEnd, "\n" + indent);
      editor.getCaretModel().moveToOffset(lineEnd + 1 + indent.length());
    }
  }

  private void handleStatementSemicolon(Document doc, Editor editor, int lineNumber, int lineEnd,
                                        String trimmed, String indent) {
    if (!trimmed.endsWith(";")) {
      doc.insertString(lineEnd, ";\n" + indent);
      editor.getCaretModel().moveToOffset(lineEnd + 2 + indent.length());
    } else {
      doc.insertString(lineEnd, "\n" + indent);
      editor.getCaretModel().moveToOffset(lineEnd + 1 + indent.length());
    }
  }
}
