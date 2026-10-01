package dev.verloren.midnight.ide.fileTemplates;

import com.intellij.ide.fileTemplates.FileTemplateDescriptor;
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptor;
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptorFactory;
import dev.verloren.midnight.icons.MidnightIcons;

/**
 * Registers Compact file templates in IntelliJ IDEA's File Templates manager.
 */
public class CompactFileTemplateGroupFactory implements FileTemplateGroupDescriptorFactory {
  public static final String COMPACT_FILE = "Compact File";
  public static final String COMPACT_CONTRACT = "Compact Contract";
  public static final String COMPACT_MODULE = "Compact Module";
  public static final String COMPACT_INTERFACE = "Compact Interface";

  public static final String COMPACT_FILE_NAME = "Compact File.compact";
  public static final String COMPACT_CONTRACT_NAME = "Compact Contract.compact";
  public static final String COMPACT_MODULE_NAME = "Compact Module.compact";
  public static final String COMPACT_INTERFACE_NAME = "Compact Interface.compact";

  @Override
  public FileTemplateGroupDescriptor getFileTemplatesDescriptor() {
    FileTemplateGroupDescriptor group = new FileTemplateGroupDescriptor("Midnight Compact", MidnightIcons.FILE);
    group.addTemplate(new FileTemplateDescriptor(COMPACT_FILE_NAME, MidnightIcons.FILE));
    group.addTemplate(new FileTemplateDescriptor(COMPACT_CONTRACT_NAME, MidnightIcons.FILE));
    group.addTemplate(new FileTemplateDescriptor(COMPACT_MODULE_NAME, MidnightIcons.FILE));
    group.addTemplate(new FileTemplateDescriptor(COMPACT_INTERFACE_NAME, MidnightIcons.FILE));
    return group;
  }
}
