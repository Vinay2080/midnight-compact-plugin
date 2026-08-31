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

sourceSets {
    main {
        java.srcDir("src/main/gen")
    }
}

tasks {
    instrumentCode {
        enabled = false
    }
    instrumentTestCode {
        enabled = false
    }
}
