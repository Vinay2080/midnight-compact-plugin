package dev.verloren.midnight.ide.fileTemplates;

import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import dev.verloren.midnight.CompactFileType;
import dev.verloren.midnight.psi.CompactExternalContractDeclaration;
import dev.verloren.midnight.psi.CompactModuleDefinition;

public class CompactFileTemplateTest extends BasePlatformTestCase {

  public void testFileTemplateGroupDescriptor() {
    CompactFileTemplateGroupFactory factory = new CompactFileTemplateGroupFactory();
    FileTemplateGroupDescriptor descriptor = factory.getFileTemplatesDescriptor();
    assertNotNull(descriptor);
    assertEquals("Midnight Compact", descriptor.getTitle());
    assertEquals(4, descriptor.getTemplates().size());
  }

  public void testContractTemplateParses() {
    String contractCode = """
            pragma language_version >= 0.20.0;
            
            export contract MyContract {
              circuit hello(): Void;
            }
            """;

    PsiFile file = myFixture.configureByText(CompactFileType.INSTANCE, contractCode);
    CompactExternalContractDeclaration contract = PsiTreeUtil.findChildOfType(file, CompactExternalContractDeclaration.class);
    assertNotNull(contract);
    assertEquals("MyContract", contract.getName());
  }

  public void testModuleTemplateParses() {
    String moduleCode = """
            pragma language_version >= 0.20.0;
            
            export module MyModule {
              // Define module functions, circuits and types here
            }
            """;

    PsiFile file = myFixture.configureByText(CompactFileType.INSTANCE, moduleCode);
    CompactModuleDefinition module = PsiTreeUtil.findChildOfType(file, CompactModuleDefinition.class);
    assertNotNull(module);
    assertEquals("MyModule", module.getName());
  }
}
