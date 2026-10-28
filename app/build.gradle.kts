import java.util.Properties

plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
    namespace = "com.tilebloom.game"
    compileSdk = 36
    buildFeatures { buildConfig = true }
    buildToolsVersion = "35.0.0"
    useLibrary("android.test.runner")
    useLibrary("android.test.base")
    defaultConfig {
        applicationId = "com.tilebloom.game"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1"
        testInstrumentationRunner = "android.test.InstrumentationTestRunner"
    }
    // Optional untracked local signing configuration; never store credentials in source.
    val signingFile = rootProject.file("signing.local.properties")
    if (signingFile.isFile) {
        val localSigning = Properties().apply { signingFile.inputStream().use { load(it) } }
        signingConfigs.getByName("debug") {
            storeFile = rootProject.file(requireNotNull(localSigning.getProperty("storeFile")))
            storePassword = requireNotNull(localSigning.getProperty("storePassword"))
            keyAlias = requireNotNull(localSigning.getProperty("keyAlias"))
            keyPassword = requireNotNull(localSigning.getProperty("keyPassword"))
        }
    }
    buildTypes {
        getByName("debug") {
            manifestPlaceholders["admobAppId"] = "ca-app-pub-3940256099942544~3347511713"
            buildConfigField("String", "REWARDED_AD_UNIT", "\"ca-app-pub-3940256099942544/5224354917\"")
        }
        getByName("release") {
            // Release is deliberately disabled until publishing configuration is reviewed.
            manifestPlaceholders["admobAppId"] = "RELEASE_NOT_CONFIGURED"
            buildConfigField("String", "REWARDED_AD_UNIT", "\"\"")
        }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation("com.google.android.gms:play-services-ads:25.5.0")
    implementation("com.google.android.ump:user-messaging-platform:4.0.0")
    testImplementation("junit:junit:4.13.2")
}

tasks.configureEach {
    if (name == "preReleaseBuild") doFirst { error("Release blocked: configure and review production AdMob IDs and privacy requirements first") }
}
