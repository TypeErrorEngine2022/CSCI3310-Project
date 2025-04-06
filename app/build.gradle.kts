plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.csci3310project"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.csci3310project"
        minSdk = 34
        targetSdk = 35
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

        // Idk if this
        isCoreLibraryDesugaringEnabled = true
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.room.runtime)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)
    implementation(project(":OpenCV"))
//    implementation(libs.litert.support.api)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    annotationProcessor(libs.room.compiler)

    // CameraX
    implementation("androidx.camera:camera-core:1.3.3")
    implementation("androidx.camera:camera-camera2:1.3.3")
    implementation("androidx.camera:camera-lifecycle:1.3.3")
    implementation("androidx.camera:camera-view:1.3.3")

    // ML Kit Face Detection
    //implementation("com.google.android.gms:play-services-mlkit-face-detection:17.1.0")
//    implementation ("com.google.mlkit:face-detection:16.1.7")

    // -
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-common-java8:2.7.0")

    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
    implementation ("commons-io:commons-io:2.4")

    // ai shit
//    implementation("org.pytorch:pytorch_android_lite:2.3.0")
//    implementation("org.pytorch:pytorch_android_torchvision_lite:2.3.0")
//    implementation("com.quickbirdstudios:opencv:4.8.0-contrib")
//    implementation("com.github.quickbirdstudios:retinaface-android:1.0.0")

    // more ai shit
    implementation ("org.tensorflow:tensorflow-lite:2.12.0")
//    implementation ("org.tensorflow:tensorflow-lite-gpu:2.12.0") // GPU accel
    implementation ("org.tensorflow:tensorflow-lite-support:0.4.4")

}