plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.appVanta.i230017_i230507"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.appVanta.i230017_i230507"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "AGORA_APP_ID", "\"\"")
        buildConfigField("String", "AGORA_CHANNEL", "\"\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    implementation("com.android.volley:volley:1.2.1")
    implementation("androidx.security:security-crypto:1.1.0")
    implementation("io.agora.rtc:full-sdk:4.3.1")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
