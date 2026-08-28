package dev.verloren.midnight.run;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.lineMarker.RunLineMarkerContributor;
import com.intellij.lang.LanguageParserDefinitions;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import dev.verloren.midnight.CompactFileType;
import dev.verloren.midnight.CompactLanguage;
import dev.verloren.midnight.parser.CompactParserDefinition;
import dev.verloren.midnight.psi.CompactExternalContractDeclaration;
import org.jdom.Element;
import org.jspecify.annotations.NonNull;

import java.util.List;

import static org.junit.Assert.assertNotEquals;

public class CompactRunConfigurationTest extends BasePlatformTestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    LanguageParserDefinitions.INSTANCE.addExplicitExtension(
        CompactLanguage.INSTANCE,
        new CompactParserDefinition()
    );
  }

  public void testCommandLineArgsGeneration() {
    CompactConfigurationType type = new CompactConfigurationType();
    ConfigurationFactory factory = type.getConfigurationFactories()[0];
    CompactRunConfiguration config = new CompactRunConfiguration(getProject(), factory, "TestConfig");

    config.setCompactFilePath("src/contract.compact");
    config.setOutputDirectory("dist/output");
    config.setSkipZk(true);
    config.setCustomCompilerFlags("--target typescript");

    List<String> args = config.buildCommandLineArgs();
    assertTrue("Args should contain --vscode", args.contains("--vscode"));
    assertTrue("Args should contain --skip-zk", args.contains("--skip-zk"));
    assertTrue("Args should contain --target", args.contains("--target"));
    assertTrue("Args should contain typescript", args.contains("typescript"));
    assertTrue("Args should contain input file", args.contains("src/contract.compact"));
    assertTrue("Args should contain output dir", args.contains("dist/output"));
  }

  public void testSerializationAndDeserialization() {
    CompactConfigurationType type = new CompactConfigurationType();
    ConfigurationFactory factory = type.getConfigurationFactories()[0];
    CompactRunConfiguration restored = getRestored(factory);

    assertEquals("contracts/token.compact", restored.getCompactFilePath());
    assertEquals("artifacts", restored.getOutputDirectory());
    assertFalse(restored.isSkipZk());
    assertEquals("--debug", restored.getCustomCompilerFlags());
  }

  private @NonNull CompactRunConfiguration getRestored(ConfigurationFactory factory) {
    CompactRunConfiguration original = new CompactRunConfiguration(getProject(), factory, "OriginalConfig");

    original.setCompactFilePath("contracts/token.compact");
    original.setOutputDirectory("artifacts");
    original.setSkipZk(false);
    original.setCustomCompilerFlags("--debug");

    Element element = new Element("configuration");
    original.writeExternal(element);

    CompactRunConfiguration restored = new CompactRunConfiguration(getProject(), factory, "RestoredConfig");
    restored.readExternal(element);
    return restored;
  }

  public void testRunLineMarkerOnContract() {
    String code = """
        export contract MyToken {
            export circuit main(): Void {}
        }
        """;
    myFixture.configureByText(CompactFileType.INSTANCE, code);

    CompactExternalContractDeclaration contract = PsiTreeUtil.findChildOfType(myFixture.getFile(), CompactExternalContractDeclaration.class);
    assertNotNull("Contract declaration should exist", contract);

    PsiElement nameIdentifier = contract.getNameIdentifier();
    assertNotNull("Contract name identifier should exist", nameIdentifier);

    CompactRunLineMarkerContributor contributor = new CompactRunLineMarkerContributor();
    RunLineMarkerContributor.Info info = contributor.getInfo(nameIdentifier);

    assertNotNull("Run line marker should be present on contract name", info);
    assertNotNull("Icon should be execute action icon", info.icon);
  }

  public void testUniqueOutputDirectoriesForDifferentContracts() {
    CompactConfigurationType type = new CompactConfigurationType();
    ConfigurationFactory factory = type.getConfigurationFactories()[0];

    // Calculator
    CompactRunConfiguration calcConfig = new CompactRunConfiguration(getProject(), factory, "Calculator");
    calcConfig.setCompactFilePath("circuits/calculator.compact");
    assertEquals("gen/calculator", calcConfig.getOutputDirectory());
    List<String> calcArgs = calcConfig.buildCommandLineArgs();
    assertTrue("Args must contain circuits/calculator.compact", calcArgs.contains("circuits/calculator.compact"));
    assertTrue("Args must contain gen/calculator", calcArgs.contains("gen/calculator"));

    // Counter
    CompactRunConfiguration counterConfig = new CompactRunConfiguration(getProject(), factory, "Counter");
    counterConfig.setCompactFilePath("circuits/counter.compact");
    assertEquals("gen/counter", counterConfig.getOutputDirectory());
    List<String> counterArgs = counterConfig.buildCommandLineArgs();
    assertTrue("Args must contain circuits/counter.compact", counterArgs.contains("circuits/counter.compact"));
    assertTrue("Args must contain gen/counter", counterArgs.contains("gen/counter"));

    // Bboard
    CompactRunConfiguration bboardConfig = new CompactRunConfiguration(getProject(), factory, "Bboard");
    bboardConfig.setCompactFilePath("circuits/bboard.compact");
    assertEquals("gen/bboard", bboardConfig.getOutputDirectory());
    List<String> bboardArgs = bboardConfig.buildCommandLineArgs();
    assertTrue("Args must contain circuits/bboard.compact", bboardArgs.contains("circuits/bboard.compact"));
    assertTrue("Args must contain gen/bboard", bboardArgs.contains("gen/bboard"));

    // Verify calculator, counter, and bboard output directories are all distinct
    assertNotEquals(calcConfig.getOutputDirectory(), counterConfig.getOutputDirectory());
    assertNotEquals(counterConfig.getOutputDirectory(), bboardConfig.getOutputDirectory());
    assertNotEquals(calcConfig.getOutputDirectory(), bboardConfig.getOutputDirectory());
  }

  public void testCompilerSubdirectoryStructureCompatibility() {
    // Verify directory paths for compiler sub-artifacts
    String baseDir = CompactToolchainUtil.deriveOutputDirectory(getProject(), "circuits/calculator.compact");
    assertEquals("gen/calculator", baseDir);

    String compilerDir = baseDir + "/compiler";
    String contractDir = baseDir + "/contract";
    String keysDir = baseDir + "/keys";
    String zkirDir = baseDir + "/zkir";

    assertEquals("gen/calculator/compiler", compilerDir);
    assertEquals("gen/calculator/contract", contractDir);
    assertEquals("gen/calculator/keys", keysDir);
    assertEquals("gen/calculator/zkir", zkirDir);
  }
}
