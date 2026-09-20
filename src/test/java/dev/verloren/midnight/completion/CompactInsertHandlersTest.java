package dev.verloren.midnight.completion;

import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.completion.OffsetMap;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.codeInsight.template.Template;
import com.intellij.codeInsight.template.TemplateManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import dev.verloren.midnight.CompactFileType;

public class CompactInsertHandlersTest extends BasePlatformTestCase {

  public void testParameterizedTypeInsertHandlerWithoutTemplate() {
    myFixture.configureByText(CompactFileType.INSTANCE, "export ledger ledger1: Op<caret>");
    LookupElement item = LookupElementBuilder.create("Opaque");
    InsertionContext context = new InsertionContext(
        new OffsetMap(myFixture.getEditor().getDocument()),
        (char) 0,
        new LookupElement[]{item},
        myFixture.getFile(),
        myFixture.getEditor(),
        false
    );

    WriteCommandAction.runWriteCommandAction(getProject(), () ->
        CompactParameterizedTypeInsertHandler.OPAQUE_BRACKETS.handleInsert(context, item));

    assertTrue(myFixture.getEditor().getDocument().getText().contains("Op<\"\">"));
  }

  public void testAssertInsertHandlerWithoutTemplate() {
    myFixture.configureByText(CompactFileType.INSTANCE, "circuit test() { as<caret> }");
    LookupElement item = LookupElementBuilder.create("assert");
    InsertionContext context = new InsertionContext(
        new OffsetMap(myFixture.getEditor().getDocument()),
        (char) 0,
        new LookupElement[]{item},
        myFixture.getFile(),
        myFixture.getEditor(),
        false
    );

    WriteCommandAction.runWriteCommandAction(getProject(), () ->
        CompactAssertInsertHandler.INSTANCE.handleInsert(context, item));

    assertTrue(myFixture.getEditor().getDocument().getText().contains("as()"));
  }

  public void testParameterizedTypeInsertHandlerWithTemplateStateDoesNotThrow() {
    myFixture.configureByText(CompactFileType.INSTANCE, "export ledger ledger1: <caret>");
    TemplateManager templateManager = TemplateManager.getInstance(getProject());
    Template template = templateManager.createTemplate("t", "user", "Op$VAR$");
    template.addVariable("VAR", "var", "var", true);

    WriteCommandAction.runWriteCommandAction(getProject(), () ->
        templateManager.startTemplate(myFixture.getEditor(), template));

    LookupElement item = LookupElementBuilder.create("Opaque");
    InsertionContext context = new InsertionContext(
        new OffsetMap(myFixture.getEditor().getDocument()),
        (char) 0,
        new LookupElement[]{item},
        myFixture.getFile(),
        myFixture.getEditor(),
        false
    );

    WriteCommandAction.runWriteCommandAction(getProject(), () ->
        CompactParameterizedTypeInsertHandler.OPAQUE_BRACKETS.handleInsert(context, item));

    assertTrue(myFixture.getEditor().getDocument().getText().contains("<\"\">"));
  }

  public void testAssertInsertHandlerWithTemplateStateDoesNotThrow() {
    myFixture.configureByText(CompactFileType.INSTANCE, "circuit test() { <caret> }");
    TemplateManager templateManager = TemplateManager.getInstance(getProject());
    Template template = templateManager.createTemplate("t", "user", "assert$VAR$");
    template.addVariable("VAR", "var", "var", true);

    WriteCommandAction.runWriteCommandAction(getProject(), () ->
        templateManager.startTemplate(myFixture.getEditor(), template));

    LookupElement item = LookupElementBuilder.create("assert");
    InsertionContext context = new InsertionContext(
        new OffsetMap(myFixture.getEditor().getDocument()),
        (char) 0,
        new LookupElement[]{item},
        myFixture.getFile(),
        myFixture.getEditor(),
        false
    );


    WriteCommandAction.runWriteCommandAction(getProject(), () ->
        CompactAssertInsertHandler.INSTANCE.handleInsert(context, item));

    assertTrue(myFixture.getEditor().getDocument().getText().contains("assert"));
  }

  public void testEitherInsertHandlerExpandsStructTemplate() {
    myFixture.configureByText(CompactFileType.INSTANCE, "circuit test() { const x = Either<caret> }");
    LookupElement item = LookupElementBuilder.create("Either");
    InsertionContext context = new InsertionContext(
        new OffsetMap(myFixture.getEditor().getDocument()),
        (char) 0,
        new LookupElement[]{item},
        myFixture.getFile(),
        myFixture.getEditor(),
        false
    );

    WriteCommandAction.runWriteCommandAction(getProject(), () -> {
      CompactEitherInsertHandler.INSTANCE.handleInsert(context, item);
    });

    assertTrue(myFixture.getEditor().getDocument().getText().contains("Either { is_left: true, left: , right: default }"));
  }

  public void testParenthesesInsertHandlerWithoutTemplate() {
    myFixture.configureByText(CompactFileType.INSTANCE, "circuit test() { const x = left<caret> }");
    LookupElement item = LookupElementBuilder.create("left");
    InsertionContext context = new InsertionContext(
        new OffsetMap(myFixture.getEditor().getDocument()),
        (char) 0,
        new LookupElement[]{item},
        myFixture.getFile(),
        myFixture.getEditor(),
        false
    );

    WriteCommandAction.runWriteCommandAction(getProject(), () ->
        CompactParenthesesInsertHandler.WITH_PARENS.handleInsert(context, item));

    assertTrue(myFixture.getEditor().getDocument().getText().contains("left()"));
  }
}
