package org.jetbrains.kotlin.native.interop.gen

import kotlinx.metadata.KmPackage
import java.io.File

// TODO: outCfile is not part of mode
sealed class InteropGenerationMode(val outCFile: File) {

    class Metadata(outCFile: File) : InteropGenerationMode(outCFile) {
        lateinit var result: KmPackage
    }

    class Textual(val outKtFile: File, outCFile: File) : InteropGenerationMode(outCFile)
}

