import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

// ---------------------------------------------------------------------------
// Release signing configuration.
//
// Signing values are read (in priority order) from:
//   1. Environment variables (used by CI / GitHub Actions).
//   2. A local, git-ignored keystore.properties file (used for local release).
//
// If a release build is requested and no valid credentials are present, the
// build fails clearly rather than silently falling back to the debug key.
// ---------------------------------------------------------------------------
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        load(FileInputStream(keystorePropertiesFile))
    }
}

fun signingValue(envName: String, propName: String): String? {
    val env = System.getenv(envName)
    if (!env.isNullOrBlank()) return env
    val prop = keystoreProperties.getProperty(propName)
    if (!prop.isNullOrBlank()) return prop
    return null
}

android {
    namespace = "com.taskora.home"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.taskora.home"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // No native libraries are bundled, so 16 KB page-size compatibility is
        // inherent. This flag documents the requirement explicitly.
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            val storeFilePath = signingValue("ANDROID_KEYSTORE_PATH", "storeFile")
            val storePwd = signingValue("ANDROID_KEYSTORE_PASSWORD", "storePassword")
            val alias = signingValue("ANDROID_KEY_ALIAS", "keyAlias")
            val keyPwd = signingValue("ANDROID_KEY_PASSWORD", "keyPassword")

            if (storeFilePath != null && storePwd != null && alias != null && keyPwd != null) {
                storeFile = file(storeFilePath)
                storePassword = storePwd
                keyAlias = alias
                keyPassword = keyPwd
            }
            // If values are absent the config remains empty; buildTypes.release
            // validates presence and fails clearly (see below).
        }
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
            isShrinkResources = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        getByName("release") {
            // STAGE 1: keep these false, verify a working non-minified release.
            // STAGE 2: switch both to true, re-test, then ship.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            val releaseSigning = signingConfigs.getByName("release")
            val hasCredentials = releaseSigning.storeFile != null &&
                releaseSigning.storePassword != null &&
                releaseSigning.keyAlias != null &&
                releaseSigning.keyPassword != null

            if (hasCredentials) {
                signingConfig = releaseSigning
            } else {
                // Fail clearly for any task that actually assembles a release
                // artifact. Configuration-time evaluation (e.g. IDE sync) is not
                // blocked so the project can still be opened without secrets.
                gradle.taskGraph.whenReady {
                    val assemblingRelease = allTasks.any {
                        it.name.contains("Release") &&
                            (it.name.startsWith("assemble") || it.name.startsWith("bundle"))
                    }
                    if (assemblingRelease) {
                        throw GradleException(
                            "Release signing credentials are missing. Provide " +
                                "ANDROID_KEYSTORE_PATH, ANDROID_KEYSTORE_PASSWORD, " +
                                "ANDROID_KEY_ALIAS and ANDROID_KEY_PASSWORD (env vars) " +
                                "or a keystore.properties file. Refusing to fall back " +
                                "to the debug signing key for a release build."
                        )
                    }
                }
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        // Enables java.time APIs on minSdk 24 devices.
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = false
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")

    // Material Components — provides the XML Theme.Material3.* app themes used
    // by the manifest/splash (Compose Material 3 does not ship XML themes).
    implementation("com.google.android.material:material:1.12.0")

    // Lifecycle / ViewModel
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")

    // Compose (BOM keeps versions aligned)
    val composeBom = platform("androidx.compose:compose-bom:2024.09.03")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.1")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // java.time desugaring
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.2")

    // Unit tests
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
}
