pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        // Huawei (AppGallery Connect plugin, for Wear Engine)
        maven { url = uri("https://developer.huawei.com/repo/") }
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Huawei Wear Engine SDK
        maven { url = uri("https://developer.huawei.com/repo/") }
    }
}

rootProject.name = "ha-companion"
include(":app")
