plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.goodusestudios.pressbench"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.goodusestudios.pressbench"
        minSdk = 23
        targetSdk = 36
        versionCode = 1403
        versionName = "1.0.0-closed-v16-native"
        manifestPlaceholders["admobAppId"] = "ca-app-pub-3940256099942544~3347511713"
    }

    signingConfigs {
        create("releaseExternal") {
            val store = providers.environmentVariable("PRESSBENCH_KEYSTORE_PATH").orNull
            if (!store.isNullOrBlank()) {
                storeFile = file(store)
                storePassword = providers.environmentVariable("PRESSBENCH_KEYSTORE_PASSWORD").orNull
                keyAlias = providers.environmentVariable("PRESSBENCH_KEY_ALIAS").orNull
                keyPassword = providers.environmentVariable("PRESSBENCH_KEY_PASSWORD").orNull
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".qa"
            versionNameSuffix = "-qa"
            buildConfigField("boolean", "ADS_CONFIGURED", "true")
            buildConfigField("String", "ADMOB_BANNER_ID", "\"ca-app-pub-3940256099942544/9214589741\"")
        }
        release {
            isMinifyEnabled = false
            val admobAppId = providers.environmentVariable("PRESSBENCH_ADMOB_APP_ID").orNull
            val admobBannerId = providers.environmentVariable("PRESSBENCH_ADMOB_BANNER_ID").orNull
            manifestPlaceholders["admobAppId"] = admobAppId ?: "ca-app-pub-3940256099942544~3347511713"
            buildConfigField("boolean", "ADS_CONFIGURED", (!admobAppId.isNullOrBlank() && !admobBannerId.isNullOrBlank()).toString())
            buildConfigField("String", "ADMOB_BANNER_ID", "\"${admobBannerId.orEmpty()}\"")
            val vars = listOf(
                "PRESSBENCH_KEYSTORE_PATH",
                "PRESSBENCH_KEYSTORE_PASSWORD",
                "PRESSBENCH_KEY_ALIAS",
                "PRESSBENCH_KEY_PASSWORD",
            )
            if (vars.all { !providers.environmentVariable(it).orNull.isNullOrBlank() }) {
                signingConfig = signingConfigs.getByName("releaseExternal")
            }
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += setOf("META-INF/AL2.0", "META-INF/LGPL2.1")
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.06.01")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.activity:activity-ktx:1.13.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.datastore:datastore-preferences:1.2.1")
    implementation("com.google.android.gms:play-services-ads:25.4.0")
    implementation("com.google.android.ump:user-messaging-platform:4.0.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
}
