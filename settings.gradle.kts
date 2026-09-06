pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { setUrl("https://www.jitpack.io") }
        mavenLocal()
    }
}

// In CI, the Homa Commons fork is checked out locally to avoid depending on
// JitPack availability/timeouts. Keep the JitPack dependency as a fallback
// for normal local checkouts that do not contain the CI-provided directory.
if (file("commons").isDirectory) {
    includeBuild("commons") {
        dependencySubstitution {
            substitute(module("com.github.herotux.commons:commons"))
                .using(project(":commons"))
        }
    }
}

include(":app")
