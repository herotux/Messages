import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.parcelize)
    alias(libs.plugins.detekt)
    `maven-publish`
}

group = "org.fossify"
version = findProperty("VERSION")?.toString() ?: System.getenv("VERSION") ?: "1.0.0"

android {
    namespace = "org.fossify.commons"
    compileSdk = libs.versions.app.build.compileSDKVersion.get().toInt()
    defaultConfig {
        minSdk = libs.versions.app.build.minimumSDK.get().toInt()
        vectorDrawables.useSupportLibrary = true
        ksp { arg("room.schemaLocation", "$projectDir/schemas") }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            consumerProguardFiles("consumer-rules.pro")
        }
    }
    publishing { singleVariant("release") {} }
    buildFeatures { viewBinding = true; compose = true }
    compileOptions {
        val javaVersion = JavaVersion.valueOf(libs.versions.app.build.javaVersion.get())
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }
    tasks.withType<KotlinCompile> {
        compilerOptions.jvmTarget.set(JvmTarget.fromTarget(libs.versions.app.build.kotlinJVMTarget.get()))
        compilerOptions.freeCompilerArgs.set(listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.material.ExperimentalMaterialApi",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi",
            "-Xcontext-receivers"
        ))
    }
    lint {
        checkReleaseBuilds = false
        abortOnError = true
        warningsAsErrors = false
        baseline = file("lint-baseline.xml")
        lintConfig = rootProject.file("lint.xml")
    }
    sourceSets { getByName("main").java.directories.add("src/main/kotlin") }
}

publishing.publications {
    create<MavenPublication>("release") {
        afterEvaluate { from(components["release"]) }
    }
}

detekt {
    baseline = file("detekt-baseline.xml")
    config.setFrom("$rootDir/detekt.yml")
    buildUponDefaultConfig = true
    allRules = false
}

dependencies {
    implementation(commonsLibs.kotlinx.serialization.json)
    api(commonsLibs.kotlin.immutable.collections)
    implementation(commonsLibs.androidx.constraintlayout)
    implementation(commonsLibs.androidx.documentfile)
    implementation(commonsLibs.androidx.swiperefreshlayout)
    implementation(commonsLibs.androidx.exifinterface)
    implementation(commonsLibs.androidx.biometric.ktx)
    implementation(commonsLibs.androidx.lifecycle.process)
    implementation(commonsLibs.ez.vcard)
    implementation(commonsLibs.bundles.lifecycle)
    implementation(commonsLibs.bundles.compose)
    implementation(commonsLibs.compose.view.binding)
    debugImplementation(commonsLibs.bundles.compose.preview)
    api(commonsLibs.joda.time)
    api(commonsLibs.recyclerView.fastScroller)
    api(commonsLibs.reprint)
    api(commonsLibs.rtl.viewpager)
    api(commonsLibs.patternLockView)
    api(commonsLibs.androidx.core.ktx)
    api(commonsLibs.androidx.appcompat)
    api(commonsLibs.material)
    api(commonsLibs.gson)
    implementation(commonsLibs.glide.compose)
    api(commonsLibs.glide)
    ksp(commonsLibs.glide.compiler)
    api(commonsLibs.bundles.room)
    ksp(commonsLibs.androidx.room.compiler)
    detektPlugins(commonsLibs.compose.detekt)
}
