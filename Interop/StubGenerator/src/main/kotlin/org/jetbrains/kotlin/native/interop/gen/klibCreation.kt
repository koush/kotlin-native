package org.jetbrains.kotlin.native.interop.gen

import kotlinx.metadata.KmPackage
import kotlinx.metadata.klib.KlibPackageWriter
import org.jetbrains.kotlin.backend.common.serialization.metadata.KlibMetadataStringTable
import org.jetbrains.kotlin.konan.CURRENT
import org.jetbrains.kotlin.konan.KonanVersion
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.library.KonanLibrary
import org.jetbrains.kotlin.konan.library.defaultResolver
import org.jetbrains.kotlin.konan.library.impl.KonanLibraryWriterImpl
import org.jetbrains.kotlin.konan.properties.Properties
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.KonanLibraryVersioning
import org.jetbrains.kotlin.library.KotlinAbiVersion
import org.jetbrains.kotlin.library.SerializedMetadata

fun createKlib(
        outputKlibPath: String,
        mode: InteropGenerationMode.Metadata,
        target: KonanTarget,
        bitcodeBridgesPath: String,
        moduleName: String,
        manifest: Properties
) {
    val packageName = "interop"
    val serializedMetadata = createSerializedMetadataFrom(moduleName, packageName, mode.result)
    val dependencies = resolveDependencies(target)
    val version = createLibraryVersion()

    KonanLibraryWriterImpl(File(outputKlibPath), moduleName, version, target).apply {
        addLinkDependencies(dependencies)
        addMetadata(serializedMetadata)
        addNativeBitcode(bitcodeBridgesPath)
        addManifestAddend(manifest)
        commit()
    }
}

private fun createLibraryVersion(): KonanLibraryVersioning =
    KonanLibraryVersioning(
            null,
            abiVersion = KotlinAbiVersion.CURRENT,
            compilerVersion = KonanVersion.CURRENT
    )

private fun resolveDependencies(target: KonanTarget): List<KonanLibrary> {
    val repositories: List<String> = listOf("stdlib")
    val resolver = defaultResolver(repositories, target)
    return resolver.defaultLinks(noStdLib = false, noDefaultLibs = true, noEndorsedLibs = true)
}


private fun createSerializedMetadataFrom(moduleName: String, packageName: String, pkg: KmPackage): SerializedMetadata =
        KlibPackageWriter(KlibMetadataStringTable()).apply {
            pkg.accept(this)
        }.write(moduleName, packageName)
