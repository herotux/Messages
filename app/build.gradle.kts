import org.gradle.api.tasks.Copy
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.konan.properties.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.detekt)
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.10"
    id("org.jetbrains.kotlin.plugin.parcelize")
}

val keystorePropertiesFile: File = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

fun hasSigningVars(): Boolean {
    return providers.environmentVariable("SIGNING_KEY_ALIAS").orNull != null
            && providers.environmentVariable("SIGNING_KEY_PASSWORD").orNull != null
            && providers.environmentVariable("SIGNING_STORE_FILE").orNull != null
            && providers.environmentVariable("SIGNING_STORE_PASSWORD").orNull != null
}

fun buildConfigStringFromEnv(name: String): String {
    val value = providers.environmentVariable(name).orNull ?: ""
    return "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""
}

base {
    val versionCode = project.property("VERSION_CODE").toString().toInt()
    archivesName = "messages-$versionCode"
}

val copyPersianFont by tasks.registering(Copy::class) {
    from(rootProject.file("Vazirmatn-Regular.ttf"))
    into("$buildDir/generated/res/persianFont/font")
    rename { "vazirmatn_regular.ttf" }
}

android {
    compileSdk = project.libs.versions.app.build.compileSDKVersion.get().toInt()

    defaultConfig {
        applicationId = project.property("APP_ID").toString()
        minSdk = project.libs.versions.app.build.minimumSDK.get().toInt()
        targetSdk = project.libs.versions.app.build.targetSDK.get().toInt()
        versionName = project.property("VERSION_NAME").toString()
        versionCode = project.property("VERSION_CODE").toString().toInt()

        buildConfigField("String", "TAPSELL_APP_KEY", buildConfigStringFromEnv("TAPSELL_APP_KEY"))
        buildConfigField("String", "TAPSELL_BANNER_ZONE", buildConfigStringFromEnv("TAPSELL_BANNER_ZONE"))
    }

    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            register("release") {
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
            }
        } else if (hasSigningVars()) {
            register("release") {
                keyAlias = providers.environmentVariable("SIGNING_KEY_ALIAS").get()
                keyPassword = providers.environmentVariable("SIGNING_KEY_PASSWORD").get()
                storeFile = file(providers.environmentVariable("SIGNING_STORE_FILE").get())
                storePassword = providers.environmentVariable("SIGNING_STORE_PASSWORD").get()
                storeType = "PKCS12"
            }
        } else {
            logger.warn("Warning: No signing config found. Build will be unsigned.")
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
        compose = true
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (keystorePropertiesFile.exists() || hasSigningVars()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    flavorDimensions.add("variants")
    productFlavors {
        register("core")
        register("foss")
        register("gplay")
    }

    sourceSets {
        getByName("main").java.directories.add("src/main/kotlin")
        getByName("main").res.srcDir("$buildDir/generated/res/persianFont")
    }

    compileOptions {
        val currentJavaVersionFromLibs = JavaVersion.valueOf(libs.versions.app.build.javaVersion.get())
        sourceCompatibility = currentJavaVersionFromLibs
        targetCompatibility = currentJavaVersionFromLibs
    }

    dependenciesInfo {
        includeInApk = false
    }

    androidResources {
        @Suppress("UnstableApiUsage")
        generateLocaleConfig = true
    }

    tasks.withType<KotlinCompile> {
        compilerOptions.jvmTarget.set(
            JvmTarget.fromTarget(project.libs.versions.app.build.kotlinJVMTarget.get())
        )
        compilerOptions.freeCompilerArgs.set(
            listOf(
                "-opt-in=kotlin.RequiresOptIn",
                "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
                "-opt-in=androidx.compose.material.ExperimentalMaterialApi",
                "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
                "-opt-in=com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi",
                "-Xcontext-receivers"
            )
        )
    }

    namespace = "org.fossify.messages"

    lint {
        checkReleaseBuilds = false
        abortOnError = true
        warningsAsErrors = false
        baseline = file("lint-baseline.xml")
        lintConfig = rootProject.file("lint.xml")
    }

    bundle {
        language {
            enableSplit = false
        }
    }
}

tasks.named("preBuild") {
    dependsOn(copyPersianFont)
}

detekt {
    baseline = file("detekt-baseline.xml")
    config.setFrom("$rootDir/detekt.yml")
    buildUponDefaultConfig = true
    allRules = false
}

dependencies {
    implementation(libs.eventbus)
    implementation(libs.indicator.fast.scroll)
    implementation(libs.mmslib)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.documentfile)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.ez.vcard)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.bundles.room)
    implementation(libs.zxing)
    implementation(libs.mlkit.text.recognition)
    implementation(libs.tapsell.plus)

    // Fossify Commons runtime/API dependencies required by the vendored subset.
    implementation("androidx.core:core-ktx:1.18.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.biometric:biometric-ktx:1.4.0-alpha02")
    implementation("androidx.exifinterface:exifinterface:1.4.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.5.1")

    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.animation:animation:1.7.6")
    implementation("androidx.compose.foundation:foundation:1.7.6")
    implementation("androidx.compose.material:material:1.7.6")
    implementation("androidx.compose.material3:material3:1.4.0")
    implementation("androidx.compose.material:material-icons-extended:1.7.6")
    implementation("androidx.compose.runtime:runtime:1.7.6")
    implementation("androidx.compose.ui:ui:1.7.6")
    implementation("androidx.compose.ui:ui-tooling-preview:1.7.6")
    implementation("androidx.compose.ui:ui-viewbinding:1.7.6")

    implementation("com.github.bumptech.glide:glide:5.0.7")
    implementation("com.github.bumptech.glide:compose:4.14.0")
    ksp("com.github.bumptech.glide:compiler:5.0.7")

    implementation("com.google.code.gson:gson:2.14.0")
    implementation("com.github.aritraroy:patternLockView:a90b0d4bf0")
    implementation("com.github.tibbi:reprint:2cb206415d")
    implementation("com.github.tibbi:RecyclerView-FastScroller:5a95285b1f")
    implementation("com.github.naveensingh:rtl-viewpager:2.0.2")
    implementation("joda-time:joda-time:2.14.3")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("com.google.android.material:material:1.14.0")

    ksp(libs.androidx.room.compiler)
    detektPlugins(libs.compose.detekt)
    testImplementation("junit:junit:4.13.2")
}
