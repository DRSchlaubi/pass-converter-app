import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

val properties = Properties()

val file = rootProject.file("local.properties")
if (file.exists()) {
    file.bufferedReader().use {
        properties.load(it)
    }
}

android {
    namespace = "dev.schlaubi.passconverter"
    compileSdk = 34

    defaultConfig {
        applicationId = "dev.schlaubi.passconverter"
        minSdk = 24
        targetSdk = 34
        versionCode = System.getenv("GITHUB_RUN_ID")?.toInt() ?: 1
        versionName = "1.0"

        resourceConfigurations.addAll(listOf("en", "de"))

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        all {
            buildConfigField("String", "API_SERVICE", """"${properties["api.service"]}"""")
            buildConfigField("String", "API_KEY", """"${properties["api.key"]}"""")
        }

        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.auth)
    implementation(libs.compose.wallet.button)
    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.activity.compose)
    implementation(platform("androidx.compose:compose-bom:2023.03.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.appcompat)

}