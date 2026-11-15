import java.util.Properties
import javax.xml.parsers.DocumentBuilderFactory

plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
// Local production IDs are never replaced with demo IDs in release variants.
val admobProperties = Properties().apply {
    val source = rootProject.file("admob.local.properties")
    if (source.isFile) source.inputStream().use { load(it) }
}
val admobKeys = listOf("ADMOB_APP_ID", "ADMOB_REWARDED_HINT_ID", "ADMOB_REWARDED_UNDO_ID", "ADMOB_INTERSTITIAL_ID")
fun productionAdmobId(key: String) = admobProperties.getProperty(key)?.trim().orEmpty()
val validateReleaseAdmob = tasks.register("validateReleaseAdmob") {
    group = "verification"
    doLast {
        val invalid = admobKeys.filter { key ->
            val value = productionAdmobId(key)
            val separator = if (key == "ADMOB_APP_ID") "~" else "/"
            !Regex("ca-app-pub-[0-9]{16}${separator}[0-9]{10}").matches(value) ||
                value.startsWith("ca-app-pub-3940256099942544")
        }
        check(invalid.isEmpty()) {
            "Release AdMob configuration invalid: ${invalid.joinToString()}. Set valid production IDs in admob.local.properties; Google demo IDs are forbidden."
        }
    }
}

android {
    namespace = "com.wamad.petaljong"
    compileSdk = 36
    buildFeatures { buildConfig = true }
    buildToolsVersion = "35.0.0"
    useLibrary("android.test.runner")
    useLibrary("android.test.base")
    defaultConfig {
        applicationId = "com.wamad.petaljong"
        resourceConfigurations += listOf("en", "fr", "es", "pt-rBR", "ar", "hi", "ja", "de")
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "0.1.1"
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
            buildConfigField("String", "INTERSTITIAL_AD_UNIT", "\"ca-app-pub-3940256099942544/1033173712\"")
            buildConfigField("String", "REWARDED_HINT_AD_UNIT", "\"ca-app-pub-3940256099942544/5224354917\"")
            buildConfigField("String", "REWARDED_UNDO_AD_UNIT", "\"ca-app-pub-3940256099942544/5224354917\"")
        }
        getByName("release") {
            manifestPlaceholders["admobAppId"] = productionAdmobId("ADMOB_APP_ID")
            mapOf(
                "INTERSTITIAL_AD_UNIT" to "ADMOB_INTERSTITIAL_ID",
                "REWARDED_HINT_AD_UNIT" to "ADMOB_REWARDED_HINT_ID",
                "REWARDED_UNDO_AD_UNIT" to "ADMOB_REWARDED_UNDO_ID"
            ).forEach { (field, key) ->
                // Escape even invalid input so validation can report property names safely.
                val value = productionAdmobId(key).replace("\\", "\\\\").replace("\"", "\\\"")
                buildConfigField("String", field, "\"$value\"")
            }
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
    if (name.contains("Release", ignoreCase = true) && name != "validateReleaseAdmob") {
        dependsOn(validateReleaseAdmob)
    }
}

// Compare generated variant outputs without printing production identifiers.
tasks.register("verifyAdmobVariants") {
    group = "verification"
    dependsOn("processDebugMainManifest", "processReleaseMainManifest",
        "generateDebugBuildConfig", "generateReleaseBuildConfig")
    doLast {
        listOf("debug", "release").forEach { variant ->
            val capitalized = variant.replaceFirstChar { it.uppercase() }
            val manifest = layout.buildDirectory.file(
                "intermediates/merged_manifest/$variant/process${capitalized}MainManifest/AndroidManifest.xml"
            ).get().asFile
            val document = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
                .newDocumentBuilder().parse(manifest)
            val androidNamespace = "http://schemas.android.com/apk/res/android"
            val metadata = document.getElementsByTagName("meta-data")
            val applicationId = (0 until metadata.length).map { metadata.item(it) as org.w3c.dom.Element }
                .single { it.getAttributeNS(androidNamespace, "name") == "com.google.android.gms.ads.APPLICATION_ID" }
                .getAttributeNS(androidNamespace, "value")
            val expectedApp = if (variant == "debug") "ca-app-pub-3940256099942544~3347511713" else productionAdmobId("ADMOB_APP_ID")
            check(applicationId == expectedApp) { "$variant merged Manifest AdMob App ID mismatch" }
            val config = layout.buildDirectory.file(
                "generated/source/buildConfig/$variant/com/wamad/petaljong/BuildConfig.java"
            ).get().asFile.readText()
            mapOf(
                "REWARDED_HINT_AD_UNIT" to "ADMOB_REWARDED_HINT_ID",
                "REWARDED_UNDO_AD_UNIT" to "ADMOB_REWARDED_UNDO_ID",
                "INTERSTITIAL_AD_UNIT" to "ADMOB_INTERSTITIAL_ID"
            ).forEach { (field, key) ->
                val expected = if (variant == "release") productionAdmobId(key)
                    else if (key == "ADMOB_INTERSTITIAL_ID") "ca-app-pub-3940256099942544/1033173712"
                    else "ca-app-pub-3940256099942544/5224354917"
                check(config.contains("$field = \"$expected\";")) { "$variant $field mismatch" }
            }
            check(config.contains("APPLICATION_ID = \"com.wamad.petaljong\";")) { "$variant application ID mismatch" }
            logger.lifecycle("$variant: merged Manifest, application ID and all three ad units verified")
        }
    }
}
