package dev.verloren.midnight

import com.intellij.openapi.fileTypes.LanguageFileType

import dev.verloren.midnight.icons.MidnightIcons
import javax.swing.Icon

class MidnightFileType : LanguageFileType(MidnightLanguage) {
    override fun getName() = "Compact"

    override fun getDescription() = "Compact source file"

    override fun getDefaultExtension() = "compact"

    override fun getIcon(): Icon = MidnightIcons.FILE
}