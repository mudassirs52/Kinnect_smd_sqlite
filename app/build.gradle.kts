import groovy.json.JsonSlurper
import java.util.Properties
plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.kinnect.i230507_i230017"
    compileSdk = 37

    buildFeatures { buildConfig = true; resValues = true }
    defaultConfig {
        val firebaseFile = project.file("google-services.json")
        if(firebaseFile.exists()) {
            val config=JsonSlurper().parse(firebaseFile) as Map<*,*>
            val info=config["project_info"] as Map<*,*>
            val client=(config["client"] as List<*>).first() as Map<*,*>
            val clientInfo=client["client_info"] as Map<*,*>
            val api=((client["api_key"] as List<*>).first() as Map<*,*>)["current_key"].toString()
            resValue("string","google_app_id",clientInfo["mobilesdk_app_id"].toString())
            resValue("string","google_api_key",api)
            resValue("string","gcm_defaultSenderId",info["project_number"].toString())
            resValue("string","project_id",info["project_id"].toString())
        }

        fun setting(name: String, fallback: String=""): String {
            val properties=Properties()
            rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { properties.load(it) }
            return properties.getProperty(name, fallback)
        }
        fun quoted(value: String)= "\""+value.replace("\\","\\\\").replace("\"","\\\"")+"\""
        buildConfigField("String", "API_BASE_URL", quoted(setting("KINNECT_API_URL", "http://10.0.2.2:8088/")))
        buildConfigField("String", "AGORA_APP_ID", quoted(setting("AGORA_APP_ID")))
        buildConfigField("String", "AGORA_CHANNEL", "\"\"")

        applicationId = "com.kinnect.i230507_i230017"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        buildConfig = true; resValues = true
    }
}

dependencies {
    implementation("com.android.volley:volley:1.2.1")
    implementation("androidx.work:work-runtime-ktx:2.10.1")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("com.google.firebase:firebase-messaging:25.0.0")
    implementation("io.agora.rtc:full-rtc-basic:4.5.0")

    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    implementation("com.android.volley:volley:1.2.1")

    testImplementation(libs.junit)
    implementation("androidx.test.espresso:espresso-idling-resource:3.6.1")
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation("androidx.test:rules:1.6.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.espresso:espresso-intents:3.6.1")
}
