package dev.verloren.midnight.psi

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.psi.FileViewProvider
import dev.verloren.midnight.CompactLanguage
import dev.verloren.midnight.INSTANCE

class CompactFile(viewProvider: FileViewProvider) :
    PsiFileBase(viewProvider, CompactLanguage) {

    override fun getFileType() = INSTANCE

    override fun toString(): String = "Compact File"
}