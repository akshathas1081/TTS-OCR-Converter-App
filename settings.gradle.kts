pluginManagement {
    repositories {
        google()             // Google repository for Android plugins
        mavenCentral()       // Maven Central repository for other dependencies
        gradlePluginPortal() // Gradle Plugin Portal for Gradle-specific plugins
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()     // Google repository for dependencies
        mavenCentral() // Maven Central repository for other dependencies
    }
}

rootProject.name = "TTS-OCR Application"
include(":app") // Include app module. Add other modules here if necessary
