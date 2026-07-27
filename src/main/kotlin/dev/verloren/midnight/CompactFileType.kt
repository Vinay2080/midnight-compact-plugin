package dev.verloren.midnight

import com.intellij.openapi.fileTypes.LanguageFileType
import dev.verloren.midnight.icons.MidnightIcons
import javax.swing.Icon

class CompactFileType : LanguageFileType(CompactLanguage) {

    override fun getName() = "Compact"

    override fun getDescription() = "Compact language file"

    override fun getDefaultExtension() = "compact"

    override fun getIcon(): Icon = MidnightIcons.FILE

}

@JvmField
val INSTANCE = CompactFileType()