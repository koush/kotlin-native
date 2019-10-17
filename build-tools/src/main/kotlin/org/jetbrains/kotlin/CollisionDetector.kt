/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import java.io.File

/**
 * Task to find out collisions before producing fat jar.
 *
 * @property configurations added to fat jar configurations
 * @property ignoredFiles excluded from analysis files
 * @property resolvingRules rules to resolve files
 * @property resolvingRulesWithRegexes rules to resolve files described with regexes
 * @property librariesWithIgnoredClassCollisions libraries which collision in class files are ignored
 */
open class CollisionDetector : DefaultTask() {
    @InputFiles
    var configurations = listOf<Configuration>()
    @Input
    val ignoredFiles = mutableListOf<String>()
    @Input
    val resolvingRules = mutableMapOf<String, String>()
    @Input
    val resolvingRulesWithRegexes = mutableMapOf<Regex, String>()
    @Input
    val librariesWithIgnoredClassCollisions = mutableListOf<String>()
    val resolvedConflicts = mutableMapOf<String, File>()

    // Key - filename, value - jar file contained it.
    private val filesInfo = mutableMapOf<String, String>()

    @TaskAction
    fun run() {
        configurations.forEach { configuration ->
            configuration.files.filter { it.name.endsWith(".jar") }.forEach { processedFile ->
                project.zipTree("${processedFile.absolutePath}").matching{ it.exclude(ignoredFiles) }.forEach {
                    val outputPath = it.absolutePath.substringAfter(processedFile.name).substringAfter("/")
                    if (outputPath in filesInfo.keys) {
                        val rule = resolvingRules.getOrElse(outputPath) {
                            var foundValue: String? = null
                            resolvingRulesWithRegexes.forEach {
                                if (it.key.matches(outputPath)) {
                                    foundValue = it.value
                                }
                            }
                            foundValue
                        }
                        var ignoreJar = false
                        if (rule != null && processedFile.name.startsWith(rule)) {
                            resolvedConflicts[outputPath] = processedFile
                        } else {
                            // Skip class files from ignored libraries if version of libraries had collision are the same.
                            val versionRegex = "\\d+\\.\\d+(\\.\\d+)?-\\w+(-\\d+)?".toRegex()
                            val currentVersion = versionRegex.find(processedFile.name)?.groupValues?.get(0)
                            val collisionLibVersion = versionRegex.find(filesInfo[outputPath]!!)?.groupValues?.get(0)
                            if (outputPath.endsWith(".class") && currentVersion == collisionLibVersion) {
                                if (processedFile.name == filesInfo[outputPath]) {
                                    ignoreJar = true
                                } else {
                                    librariesWithIgnoredClassCollisions.forEach {
                                        if (processedFile.name.startsWith(it)) {
                                            ignoreJar = true
                                        }
                                    }
                                }
                            }
                        }
                        if (rule == null && !ignoreJar) {
                            error("Collision is detected. File $outputPath is found in ${filesInfo[outputPath]} and ${processedFile.name}")
                        }
                    } else {
                        filesInfo[outputPath] = processedFile.name
                    }
                }
            }
        }
    }
}