package dev.verloren.midnight.formatter;

import com.intellij.lang.LanguageFormatting;
import com.intellij.lang.LanguageParserDefinitions;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import dev.verloren.midnight.CompactFileType;
import dev.verloren.midnight.CompactLanguage;
import dev.verloren.midnight.parser.CompactParserDefinition;
import org.jetbrains.annotations.NotNull;

public abstract class CompactFormatterTestBase extends BasePlatformTestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    LanguageParserDefinitions.INSTANCE.addExplicitExtension(
        CompactLanguage.INSTANCE,
        new CompactParserDefinition()
    );
    LanguageFormatting.INSTANCE.addExplicitExtension(
        CompactLanguage.INSTANCE,
        new CompactFormattingModelBuilder()
    );
    CommonCodeStyleSettings.IndentOptions indentOptions =
        com.intellij.application.options.CodeStyle.getSettings(getProject()).getIndentOptions(CompactFileType.INSTANCE);
    indentOptions.INDENT_SIZE = 2;
    indentOptions.TAB_SIZE = 2;
    indentOptions.CONTINUATION_INDENT_SIZE = 2;
    indentOptions.USE_TAB_CHARACTER = false;
  }

  protected void doFormatTest(@NotNull String input, @NotNull String expected) {
    myFixture.configureByText(CompactFileType.INSTANCE, input);
    WriteCommandAction.runWriteCommandAction(getProject(), () -> {
      CodeStyleManager.getInstance(getProject()).reformat(myFixture.getFile());
    });
    assertEquals(expected, myFixture.getFile().getText());
  }

  protected void doIdempotenceTest(@NotNull String input) {
    myFixture.configureByText(CompactFileType.INSTANCE, input);
    WriteCommandAction.runWriteCommandAction(getProject(), () -> {
      CodeStyleManager.getInstance(getProject()).reformat(myFixture.getFile());
    });
    String formattedOnce = myFixture.getFile().getText();
    WriteCommandAction.runWriteCommandAction(getProject(), () -> {
      CodeStyleManager.getInstance(getProject()).reformat(myFixture.getFile());
    });
    String formattedTwice = myFixture.getFile().getText();
    assertEquals(formattedOnce, formattedTwice);
  }
}
