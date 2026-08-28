package dev.verloren.midnight.navigation;

import com.intellij.lang.LanguageParserDefinitions;
import com.intellij.navigation.ItemPresentation;
import com.intellij.navigation.NavigationItem;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.util.CommonProcessors;
import com.intellij.util.indexing.FindSymbolParameters;
import dev.verloren.midnight.CompactFileType;
import dev.verloren.midnight.CompactLanguage;
import dev.verloren.midnight.parser.CompactParserDefinition;
import dev.verloren.midnight.psi.CompactNamedElement;

import java.util.ArrayList;
import java.util.List;

public class CompactChooseByNameTest extends BasePlatformTestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    LanguageParserDefinitions.INSTANCE.addExplicitExtension(
        CompactLanguage.INSTANCE,
        new CompactParserDefinition()
    );
  }

  public void testGotoClassFindsContractsModulesStructsEnumsAndTypes() {
    String code = """
        contract MyContract {
        }
        module MathModule {
        }
        struct Point {
            x: Field;
            y: Field;
        }
        enum Color {
            Red,
            Green,
            Blue
        }
        type CustomHash = Bytes<32>;
        """;
    myFixture.configureByText(CompactFileType.INSTANCE, code);

    CompactGotoClassContributor contributor = new CompactGotoClassContributor();
    CommonProcessors.CollectProcessor<String> namesCollector = new CommonProcessors.CollectProcessor<>();
    contributor.processNames(namesCollector, GlobalSearchScope.projectScope(getProject()), null);

    List<String> names = new ArrayList<>(namesCollector.getResults());
    assertTrue("Should contain MyContract", names.contains("MyContract"));
    assertTrue("Should contain MathModule", names.contains("MathModule"));
    assertTrue("Should contain Point", names.contains("Point"));
    assertTrue("Should contain Color", names.contains("Color"));
    assertTrue("Should contain CustomHash", names.contains("CustomHash"));

    List<NavigationItem> items = new ArrayList<>();
    FindSymbolParameters params = FindSymbolParameters.simple(getProject(), false);
    contributor.processElementsWithName("Point", items::add, params);

    assertEquals("Should find 1 Point struct", 1, items.size());
    assertEquals("Point", items.getFirst().getName());
  }

  public void testGotoSymbolFindsCircuitsWitnessesLedgersAndFields() {
    String code = """
        ledger totalBalance: Field;
        witness fetchSecret(): Bytes<32>;
        circuit transfer(): [] {
            const localVar = 1;
        }
        struct Account {
            owner: Field;
        }
        """;
    myFixture.configureByText(CompactFileType.INSTANCE, code);

    CompactGotoSymbolContributor contributor = new CompactGotoSymbolContributor();
    CommonProcessors.CollectProcessor<String> namesCollector = new CommonProcessors.CollectProcessor<>();
    contributor.processNames(namesCollector, GlobalSearchScope.projectScope(getProject()), null);

    List<String> names = new ArrayList<>(namesCollector.getResults());
    assertTrue("Should contain totalBalance", names.contains("totalBalance"));
    assertTrue("Should contain fetchSecret", names.contains("fetchSecret"));
    assertTrue("Should contain transfer", names.contains("transfer"));
    assertTrue("Should contain Account", names.contains("Account"));
    assertTrue("Should contain owner", names.contains("owner"));
    assertFalse("Should NOT contain local block variable", names.contains("localVar"));

    List<NavigationItem> items = new ArrayList<>();
    FindSymbolParameters params = FindSymbolParameters.simple(getProject(), false);
    contributor.processElementsWithName("transfer", items::add, params);

    assertEquals("Should find 1 transfer circuit", 1, items.size());
    assertEquals("transfer", items.getFirst().getName());

    NavigationItem item = items.getFirst();
    assertTrue(item instanceof CompactNamedElement);
    ItemPresentation presentation = item.getPresentation();
    assertNotNull(presentation);
    assertEquals("transfer", presentation.getPresentableText());
    assertNotNull(presentation.getIcon(false));
  }
}
