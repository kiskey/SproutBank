plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.sproutbank.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.sproutbank.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        ndk { abiFilters += listOf("arm64-v8a", "x86_64") }
    }

    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    sourceSets["main"].jniLibs.srcDirs("src/main/jniLibs")
    sourceSets["main"].java.srcDirs("build/generated/source/uniffi/main/java")
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation(platform("androidx.compose:compose-bom:2024.09.02"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")

  // inside dependencies { ... }  
    implementation("io.ktor:ktor-client-core:2.3.12")
    implementation("io.ktor:ktor-client-okhttp:2.3.12")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.12")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    implementation("net.java.dev.jna:jna:5.14.0@aar")
    debugImplementation("androidx.compose.ui:ui-tooling")
}

// ---- Rust integration -------------------------------------------------
val rustDir = rootProject.file("rust-core")
val jniDir  = file("src/main/jniLibs")
val bindGen = file("${buildDir}/generated/source/uniffi/main/java")

val buildRust by tasks.registering(Exec::class) {
    workingDir = rustDir
    commandLine("bash", "-c",
        "cargo ndk -t arm64-v8a -t x86_64 -o ../app/src/main/jniLibs build --release")
}

val generateBindings by tasks.registering(Exec::class) {
    workingDir = rustDir
    commandLine("bash", "-c",
        "cargo run --bin uniffi-bindgen -- generate --language kotlin " +
        "--library target/aarch64-linux-android/release/libsproutbank_core.so " +
        "--out-dir ${bindGen.absolutePath}")
}

tasks.named("preBuild") {
    dependsOn(buildRust, generateBindings)
}
