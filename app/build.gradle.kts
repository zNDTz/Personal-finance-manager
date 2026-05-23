tasks.register("renameResources") {
    doLast {
        val xmlFile = file("src/main/res/drawable/bg_app_main_image.xml")
        if (xmlFile.exists()) {
            xmlFile.renameTo(file("src/main/res/drawable/bg_app_main_gradient.xml"))
        }
        val jpgFile = file("src/main/res/drawable/bg_app_main_image_file.jpg")
        if (jpgFile.exists()) {
            jpgFile.renameTo(file("src/main/res/drawable/bg_app_main_image.jpg"))
        }
    }
}

plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.personal_finance_manager"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.personal_finance_manager"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    val room_version = "2.6.1"

    implementation("androidx.room:room-runtime:$room_version")
    annotationProcessor("androidx.room:room-compiler:$room_version")
    
    // Hỗ trợ xử lý bất đồng bộ cho Java (ListenableFuture)
    implementation("androidx.room:room-guava:$room_version")
    // Hỗ trợ testing
    testImplementation("androidx.room:room-testing:$room_version")

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    
    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.gson)
    implementation(libs.okhttp.logging)
    
    // Charts
    implementation(libs.mpAndroidChart)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
