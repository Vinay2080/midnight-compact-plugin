import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.changelog")
    id("org.jetbrains.intellij.platform")
}

dependencies {
    testImplementation(libs.junit)

    intellijPlatform {
        intellijIdea("2026.2.0.1")
        testFramework(TestFrameworkType.Platform)
        bundledModule("intellij.spellchecker")
        compatiblePlugin("com.chrisrm.idea.MaterialThemeUI")
    }
}

intellijPlatform {
    pluginConfiguration {
        version = providers.gradleProperty("version")
        ideaVersion {
            sinceBuild = "242"
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
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

sourceSets {
    main {
        java.srcDir("src/main/gen")
    }
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }
    buildSearchableOptions {
        enabled = false
    }
    instrumentCode {
        enabled = false
    }
    instrumentTestCode {
        enabled = false
    }
}


