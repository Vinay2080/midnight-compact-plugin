import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.changelog")
    id("org.jetbrains.intellij.platform")
}

val enableMtui = providers.environmentVariable("MTUI")
    .orElse(providers.environmentVariable("ENABLE_MTUI"))
    .orElse(providers.gradleProperty("mtui"))
    .map { it.isBlank() || it.equals("true", ignoreCase = true) || it == "1" }
    .getOrElse(false)

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
}
