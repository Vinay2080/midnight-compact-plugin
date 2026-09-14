import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.changelog")
    id("org.jetbrains.intellij.platform")
}

val requestedTasks = gradle.startParameter.taskNames

// Tasks that build, test, verify, or publish the plugin for distribution/CI
val isBuildOrPublish = requestedTasks.any { task ->
    task.contains("buildPlugin", ignoreCase = true) ||
    task.contains("publishPlugin", ignoreCase = true) ||
    task.contains("signPlugin", ignoreCase = true) ||
    task.contains("verifyPlugin", ignoreCase = true) ||
    task.contains("build", ignoreCase = true) ||
    task.contains("assemble", ignoreCase = true) ||
    task.contains("check", ignoreCase = true) ||
    task.contains("test", ignoreCase = true)
}

// Tasks or flags to run sandbox WITHOUT Material Theme UI
val isExplicitlyDisabled = requestedTasks.any { task ->
    task.contains("runIdeNoMtui", ignoreCase = true) ||
    task.contains("withoutMtui", ignoreCase = true) ||
    task.contains("noMtui", ignoreCase = true)
} || providers.gradleProperty("noMtui").orNull?.let { it.isBlank() || it.equals("true", ignoreCase = true) || it == "1" } == true
  || providers.gradleProperty("disableMtui").orNull?.let { it.isBlank() || it.equals("true", ignoreCase = true) || it == "1" } == true
  || providers.gradleProperty("mtui").orNull?.let { it.equals("false", ignoreCase = true) || it == "0" } == true
  || providers.environmentVariable("NO_MTUI").orNull?.let { it.isBlank() || it.equals("true", ignoreCase = true) || it == "1" } == true
  || providers.environmentVariable("DISABLE_MTUI").orNull?.let { it.isBlank() || it.equals("true", ignoreCase = true) || it == "1" } == true
  || providers.environmentVariable("MTUI").orNull?.let { it.equals("false", ignoreCase = true) || it == "0" } == true
  || providers.environmentVariable("ENABLE_MTUI").orNull?.let { it.equals("false", ignoreCase = true) || it == "0" } == true

// Running in development environment (e.g. running the sandbox IDE)
val isDevRun = requestedTasks.any { task -> task.contains("runIde", ignoreCase = true) }

// By default in development environment (runIde / sandbox), enable Material Theme UI.
// If run via another command (e.g. runIdeNoMtui, -PnoMtui, -Pmtui=false, NO_MTUI=1, etc.), disable it.
// When building, packaging, or publishing the plugin (buildPlugin, publishPlugin), NEVER include Material Theme UI.
val enableMtui = !isBuildOrPublish && !isExplicitlyDisabled && (isDevRun || requestedTasks.isEmpty())

logger.lifecycle("[Midnight] Material Theme UI enabled in sandbox: $enableMtui")

dependencies {
    testImplementation(libs.junit)

    intellijPlatform {
        intellijIdea("2026.2.0.1")
        testFramework(TestFrameworkType.Platform)
        bundledModule("intellij.spellchecker")
        if (enableMtui) {
            compatiblePlugin("com.chrisrm.idea.MaterialThemeUI")
        }
    }
}

intellijPlatform {
    pluginConfiguration {
        version = providers.gradleProperty("version")
        ideaVersion {
            sinceBuild = "262"
            untilBuild = provider { null }
        }
        changeNotes = provider {
            changelog.renderItem(
                (changelog.getOrNull(providers.gradleProperty("version").get())
                    ?: changelog.getUnreleased()),
                org.jetbrains.changelog.Changelog.OutputType.HTML
            )
        }
    }

    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }

    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
    }
}

kotlin {
    jvmToolchain(25)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

sourceSets {
    main {
        java.srcDir("src/main/gen")
    }
}

tasks {
    withType<JavaCompile> {
        options.compilerArgs.addAll(listOf("-Xlint:all", "-Xlint:-processing", "-Xlint:-serial"))
    }
    buildSearchableOptions {
        enabled = false
    }
    prepareJarSearchableOptions {
        enabled = false
    }
    jarSearchableOptions {
        enabled = false
    }
    instrumentCode {
        enabled = false
    }
    instrumentTestCode {
        enabled = false
    }

    register("runIdeNoMtui") {
        group = "intellij platform"
        description = "Runs the IDE sandbox without Material Theme UI."
        dependsOn(named("runIde"))
    }
}
