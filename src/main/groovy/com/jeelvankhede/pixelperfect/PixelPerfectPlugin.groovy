package com.jeelvankhede.pixelperfect

import org.gradle.api.Plugin
import org.gradle.api.Project

class PixelPerfectPlugin implements Plugin<Project> {
    public final static String EXT_DIMENSIONS = "dimens"

    @Override
    void apply(Project target) {
        DimensionContainerExtension extension = target.getExtensions().create(EXT_DIMENSIONS, DimensionContainerExtension.class)
        for (PluginType pluginType : PluginType.values()) {
            for (String plugin : pluginType.plugins()) {
                if (target.plugins.hasPlugin(plugin)) {
                    setupPlugin(target, pluginType, extension)
                    return
                }
            }
        }
    }

    private static void setupPlugin(Project project, PluginType pluginType, def extension) {
        switch (pluginType) {
            case PluginType.APPLICATION:
                project.android.applicationVariants.all { variant ->
                    handleVariant(project, variant, extension)
                }
                break
            case PluginType.LIBRARY:
                project.android.libraryVariants.all { variant ->
                    handleVariant(project, variant, extension)
                }
                break
            case PluginType.FEATURE:
                project.android.featureVariants.all { variant ->
                    handleVariant(project, variant, extension)
                }
                break
            case PluginType.MODEL_APPLICATION:
                project.model.android.applicationVariants.all { variant ->
                    handleVariant(project, variant, extension)
                }
                break
            case PluginType.MODEL_LIBRARY:
                project.model.android.libraryVariants.all { variant ->
                    handleVariant(project, variant, extension)
                }
                break
        }
    }

    private static void handleVariant(Project project, def variant, def extension) {
        File outputDir =
                project.file("$project.buildDir/generated/res/dimens")

        DimensionGeneratorTask task = project.tasks
                .create("process${variant.name.capitalize()}DimensGenerator", DimensionGeneratorTask) as DimensionGeneratorTask

        task.setIntermediateDir(outputDir)
//        task.setVariantDir(variant.dirName)
//        task.setCurrentFlavor(getCurrentFlavor(project))
        task.setExtension(extension)
//        task.setFileName(extension.fileName)

        // This is necessary for backwards compatibility with versions of gradle that do not support
        // this new API.
        if (variant.respondsTo("applicationIdTextResource")) {
            task.setPackageNameXOR2(variant.applicationIdTextResource)
            task.dependsOn(variant.applicationIdTextResource)
        } else {
            task.setPackageNameXOR1(variant.applicationId)
        }

        // This is necessary for backwards compatibility with versions of gradle that do not support
        // this new API.
        if (variant.respondsTo("registerGeneratedResFolders")) {
            task.ext.generatedResFolders = project.files(outputDir).builtBy(task)
            variant.registerGeneratedResFolders(task.generatedResFolders)
            if (variant.respondsTo("getMergeResourcesProvider")) {
                variant.mergeResourcesProvider.configure { dependsOn(task) }
            } else {
                //noinspection GrDeprecatedAPIUsage
                variant.mergeResources.dependsOn(task)
            }
        } else {
            //noinspection GrDeprecatedAPIUsage
            variant.registerResGeneratingTask(task, outputDir)
        }
    }

    static class DimensionContainerExtension {
        Set<String> dpis = new HashSet<>()
    }

    // These are the plugin types and the set of associated plugins whose presence should be checked for.
    private final static enum PluginType {
        APPLICATION([
                "android",
                "com.android.application"
        ]),
        LIBRARY([
                "android-library",
                "com.android.library"
        ]),
        FEATURE([
                "android-feature",
                "com.android.feature"
        ]),
        MODEL_APPLICATION([
                "com.android.model.application"
        ]),
        MODEL_LIBRARY(["com.android.model.library"])

        PluginType(Collection plugins) {
            this.plugins = plugins
        }
        private final Collection plugins

        Collection plugins() {
            return plugins
        }
    }
}