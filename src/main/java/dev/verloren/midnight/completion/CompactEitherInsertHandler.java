package dev.verloren.midnight.completion;

import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import org.jetbrains.annotations.NotNull;

/**
 * Insert handler for completing {@code Either} struct literals in value contexts.
 * Appends standard struct literal fields and places caret at the value position of the left field.
 */
public class CompactEitherInsertHandler implements InsertHandler<LookupElement> {

  public static final CompactEitherInsertHandler INSTANCE = new CompactEitherInsertHandler();

  private static final String STRUCT_SNIPPET = " { is_left: true, left: , right: default }";
  private static final int CARET_OFFSET_IN_SNIPPET = " { is_left: true, left: ".length();

  @Override
  public void handleInsert(@NotNull InsertionContext context, @NotNull LookupElement item) {
    Editor editor = context.getEditor();
    Document document = context.getDocument();
    int tailOffset = context.getTailOffset();
    CharSequence chars = document.getCharsSequence();

    // Check if '{' already follows
    int offset = tailOffset;
    while (offset < chars.length() && Character.isWhitespace(chars.charAt(offset))) {
      offset++;
    }
    boolean hasBrace = offset < chars.length() && chars.charAt(offset) == '{';

    if (!hasBrace) {
      document.insertString(tailOffset, STRUCT_SNIPPET);
      editor.getCaretModel().moveToOffset(tailOffset + CARET_OFFSET_IN_SNIPPET);
    }
  }
}
