/*
Copyright 2017 Google Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package com.google.androidstudiopoet.models

import com.google.androidstudiopoet.input.*
import com.google.androidstudiopoet.utils.joinPath
import com.google.androidstudiopoet.utils.joinPaths

class AndroidModuleBlueprint(name: String,
                             private val numOfActivities: Int,
                             private val resourcesConfig: ResourcesConfig?,
                             projectRoot: String,
                             val hasLaunchActivity: Boolean,
                             useKotlin: Boolean,
                             dependencies: Set<Dependency>,
                             productFlavorConfigs: List<FlavorConfig>?,
                             buildTypeConfigs: List<BuildTypeConfig>?,
                             javaConfig: CodeConfig?, kotlinConfig: CodeConfig?,
                             extraLines: List<String>?,
                             generateTests: Boolean,
                             dataBindingConfig: DataBindingConfig?,
                             val androidBuildConfig: AndroidBuildConfig,
                             pluginConfigs: List<PluginConfig>?,
                             generateBazelFiles: Boolean?
) : AbstractModuleBlueprint(name, projectRoot, useKotlin, dependencies, javaConfig, kotlinConfig, extraLines,
        generateTests) {

    val packageName = "com.$name"
    val srcPath = moduleRoot.joinPath("src")
    val mainPath = srcPath.joinPath("main")
    val resDirPath = mainPath.joinPath("res")
    val codePath = mainPath.joinPath("java")
    val packagePath = codePath.joinPaths(packageName.split("."))

    private val resourcesToReferWithin by lazy {
        dependencies
            .filterIsInstance<AndroidModuleDependency>()
            .toSortedSet()
            .map { it.resourcesToRefer }
            .fold(ResourcesToRefer(listOf(), listOf(), listOf())) { acc, resourcesToRefer -> resourcesToRefer.combine(acc) }
    }

    val resourcesBlueprint by lazy {
        when (resourcesConfig) {
            null -> null
            else -> ResourcesBlueprint(name, resDirPath, resourcesConfig.stringCount ?: 0,
                    resourcesConfig.imageCount ?: 0, resourcesConfig.layoutCount ?: 0, resourcesToReferWithin,
                    listenerClassesForDataBinding)
        }
    }

    private val layoutBlueprints by lazy {
        resourcesBlueprint?.layoutBlueprints ?: listOf()
    }
    val activityNames = (0 until numOfActivities).map { "Activity$it" }

    val resourcesToReferFromOutside by lazy {
        resourcesBlueprint?.resourcesToReferFromOutside ?: ResourcesToRefer(listOf(), listOf(), listOf())
    }

    val activityBlueprints by lazy {
        (0 until numOfActivities).map {
            ActivityBlueprint(activityNames[it], layoutBlueprints[it], packagePath, packageName,
                    classToReferFromActivity, listenerClassesForDataBindingPerLayout[it], hasButterknifeDependency())
        }
    }

    private val listenerClassesForDataBindingPerLayout by lazy {
        resourcesBlueprint?.dataBindingListenersPerLayout ?: listOf()
    }

    private val classBlueprintSequence: Sequence<ClassBlueprint> by lazy {
        (packagesBlueprint.javaPackageBlueprints.asSequence() + packagesBlueprint.kotlinPackageBlueprints.asSequence())
                .flatMap { it.classBlueprints.asSequence() }
    }

    private val classToReferFromActivity: ClassBlueprint by lazy {
        classBlueprintSequence.first()
    }

    val hasDataBinding: Boolean = dataBindingConfig?.listenerCount?.let { it > 0 } ?: false
    private val listenerClassesForDataBinding: List<ClassBlueprint> by lazy {
        classBlueprintSequence.filter { it.getMethodToCallFromOutside() != null }
                .take(dataBindingConfig?.listenerCount ?: 0).toList()
    }

    val buildGradleBlueprint: AndroidBuildGradleBlueprint by lazy {
        AndroidBuildGradleBlueprint(hasLaunchActivity, useKotlin, hasDataBinding, moduleRoot, androidBuildConfig,
                packageName, extraLines, productFlavorConfigs, buildTypeConfigs, dependencies, pluginConfigs)
    }

    val buildBazelBlueprint: AndroidBuildBazelBlueprint? by lazy {
        when (generateBazelFiles) {
            true -> AndroidBuildBazelBlueprint(hasLaunchActivity, moduleRoot, packageName, dependencies, name)
            else -> null
        }
    }

    private fun hasButterknifeDependency(): Boolean = buildGradleBlueprint.plugins
            .find { it == "com.jakewharton.butterknife" } != null &&
            dependencies.filterIsInstance<LibraryDependency>()
                    .find { it.name.startsWith("com.jakewharton:butterknife") } != null
}
