package dev.verloren.midnight.run;

import com.intellij.execution.actions.ConfigurationContext;

import com.intellij.lang.LanguageParserDefinitions;

import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiElement;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import dev.verloren.midnight.CompactFileType;
import dev.verloren.midnight.CompactLanguage;
import dev.verloren.midnight.parser.CompactParserDefinition;


public class CompactRunConfigurationProducerTest extends BasePlatformTestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    LanguageParserDefinitions.INSTANCE.addExplicitExtension(
        CompactLanguage.INSTANCE,
        new CompactParserDefinition()
    );
  }

  public void testProduceConfigurationFromContext() {
    String code = """
        export contract <caret>MyToken {
            circuit mint(): [] {}
        }
        """;
    var file = myFixture.configureByText(CompactFileType.INSTANCE, code);
    PsiElement elementAtCaret = file.findElementAt(myFixture.getCaretOffset());
    assertNotNull(elementAtCaret);

    CompactRunConfigurationProducer producer = new CompactRunConfigurationProducer();
    CompactConfigurationType type = new CompactConfigurationType();
    CompactRunConfiguration config = new CompactRunConfiguration(getProject(), type.getConfigurationFactories()[0], "");

    ConfigurationContext context = new ConfigurationContext(elementAtCaret);
    Ref<PsiElement> sourceElement = new Ref<>();
    boolean setup = producer.setupConfigurationFromContext(config, context, sourceElement);

    assertTrue("Producer should setup configuration from Compact context", setup);
    assertEquals("Compile " + file.getName(), config.getName());
    assertEquals(file.getViewProvider().getVirtualFile().getPath(), config.getCompactFilePath());
    assertEquals(CompactToolchainUtil.deriveOutputDirectory(getProject(), file.getViewProvider().getVirtualFile().getPath()), config.getOutputDirectory());
    assertTrue("Should match configuration from context", producer.isConfigurationFromContext(config, context));
  }

  public void testProduceConfigurationForCircuitsContracts() {
    var calcFile = myFixture.addFileToProject("circuits/calculator.compact", "export contract Calculator {}");
    var counterFile = myFixture.addFileToProject("circuits/counter.compact", "export contract Counter {}");
    var bboardFile = myFixture.addFileToProject("circuits/bboard.compact", "export contract Bboard {}");

    CompactRunConfigurationProducer producer = new CompactRunConfigurationProducer();
    CompactConfigurationType type = new CompactConfigurationType();

    // 1. Calculator
    CompactRunConfiguration configCalc = new CompactRunConfiguration(getProject(), type.getConfigurationFactories()[0], "");
    boolean setupCalc = producer.setupConfigurationFromContext(configCalc, new ConfigurationContext(calcFile), new Ref<>());
    assertTrue(setupCalc);
    assertEquals("Compile calculator.compact", configCalc.getName());
    assertEquals("gen/calculator", configCalc.getOutputDirectory());

    // 2. Counter
    CompactRunConfiguration configCounter = new CompactRunConfiguration(getProject(), type.getConfigurationFactories()[0], "");
    boolean setupCounter = producer.setupConfigurationFromContext(configCounter, new ConfigurationContext(counterFile), new Ref<>());
    assertTrue(setupCounter);
    assertEquals("Compile counter.compact", configCounter.getName());
    assertEquals("gen/counter", configCounter.getOutputDirectory());

    // 3. BBoard
    CompactRunConfiguration configBBoard = new CompactRunConfiguration(getProject(), type.getConfigurationFactories()[0], "");
    boolean setupBBoard = producer.setupConfigurationFromContext(configBBoard, new ConfigurationContext(bboardFile), new Ref<>());
    assertTrue(setupBBoard);
    assertEquals("Compile bboard.compact", configBBoard.getName());
    assertEquals("gen/bboard", configBBoard.getOutputDirectory());
  }
}
