import java.text.SimpleDateFormat
import java.util.Date

plugins {
    alias(libs.plugins.com.android.application)
    alias(libs.plugins.org.jetbrains.kotlin.android)
    alias(libs.plugins.com.google.dagger.hilt.android)
    kotlin("kapt")
}

android {
    namespace = "com.messege.alarmbot"

    compileSdk = libs.versions.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }

    val formattedDate: String = SimpleDateFormat("yyMMdd").format(Date())
    val code = formattedDate.toInt() * 100

    defaultConfig {
        applicationId = "com.messege.alarmbot"
        versionCode = 114
        versionName = "1.0.0"
        setProperty("archivesBaseName", "AlarmBot_${versionName}_$code")
        vectorDrawables.useSupportLibrary = true
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.kotlinCompilerExtensionVersion.get()
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar", "*.aar"))))

    implementation(libs.hilt.android)
    kapt(libs.hilt.android.compiler)

    implementation(platform(libs.bom.compose))
    implementation(libs.bundles.compose)
    implementation(libs.bundles.material)
    implementation(libs.bundles.navigation)

    implementation(libs.datastore)
    implementation(libs.bundles.network)
    implementation(libs.bundles.room)
    kapt(libs.room.compiler)

    implementation(libs.work.runtime)
}