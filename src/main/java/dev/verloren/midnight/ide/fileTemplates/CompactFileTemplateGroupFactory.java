package dev.verloren.midnight.ide.fileTemplates;

import com.intellij.ide.fileTemplates.FileTemplateDescriptor;
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptor;
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptorFactory;
import dev.verloren.midnight.icons.MidnightIcons;

/**
 * Registers Compact file templates in IntelliJ IDEA's File Templates manager.
 */
public class CompactFileTemplateGroupFactory implements FileTemplateGroupDescriptorFactory {
  public static final String COMPACT_FILE = "Compact File.compact";
  public static final String COMPACT_CONTRACT = "Compact Contract.compact";
  public static final String COMPACT_MODULE = "Compact Module.compact";
  public static final String COMPACT_INTERFACE = "Compact Interface.compact";

  @Override
  public FileTemplateGroupDescriptor getFileTemplatesDescriptor() {
    FileTemplateGroupDescriptor group = new FileTemplateGroupDescriptor("Midnight Compact", MidnightIcons.FILE);
    group.addTemplate(new FileTemplateDescriptor(COMPACT_FILE, MidnightIcons.FILE));
    group.addTemplate(new FileTemplateDescriptor(COMPACT_CONTRACT, MidnightIcons.FILE));
    group.addTemplate(new FileTemplateDescriptor(COMPACT_MODULE, MidnightIcons.FILE));
    group.addTemplate(new FileTemplateDescriptor(COMPACT_INTERFACE, MidnightIcons.FILE));
    return group;
  }
}
