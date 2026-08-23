package dev.verloren.midnight.highlighter;

import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.options.colors.AttributesDescriptor;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

import java.util.Map;

/**
 * Automated test suite for {@link CompactColorSettingsPage}.
 */
public class CompactColorSettingsPageTest extends BasePlatformTestCase {

  public void testColorSettingsPageBasicProperties() {
    CompactColorSettingsPage page = new CompactColorSettingsPage();

    assertEquals("Compact", page.getDisplayName());
    assertNotNull(page.getIcon());
    assertNotNull(page.getHighlighter());
    assertNotNull(page.getColorDescriptors());
    assertEquals(0, page.getColorDescriptors().length);

    AttributesDescriptor[] descriptors = page.getAttributeDescriptors();
    assertNotNull(descriptors);
    assertTrue("Descriptors should have at least 25 entries", descriptors.length >= 25);

    Map<String, TextAttributesKey> tags = page.getAdditionalHighlightingTagToDescriptorMap();
    assertNotNull(tags);
    assertTrue("Additional tags map should contain entries", tags.size() >= 20);

    String demoText = page.getDemoText();
    assertNotNull(demoText);
    assertTrue("Demo text should contain pragma", demoText.contains("pragma"));
    assertTrue("Demo text should contain circuit_decl tag", demoText.contains("<circuit_decl>"));
    assertTrue("Demo text should contain enum_decl tag", demoText.contains("<enum_decl>"));
    assertTrue("Demo text should contain escape_valid tag", demoText.contains("<escape_valid>"));
    assertTrue("Demo text should contain doc_tag tag", demoText.contains("<doc_tag>"));
    assertTrue("Demo text should contain doc_tag_value tag", demoText.contains("<doc_tag_value>"));
  }
}
