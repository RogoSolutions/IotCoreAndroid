plugins {
    id("com.android.library")
    id("maven-publish")}

android {
    namespace = "com.example.rogocore_lib"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

// --- PHẦN XỬ LÝ ĐẶC BIỆT ĐỂ NHÚNG AAR GIỐNG NHƯ JAR ---
val extractedAarDir = layout.buildDirectory.dir("intermediates/extracted_aar_jar")
// Nhận cả bản -release lẫn -debug; báo lỗi rõ ràng thay vì âm thầm bỏ qua khi thiếu file
val baseAndroidAar = fileTree("libs") { include("rogobaseandroid-*.aar") }.files.singleOrNull()
    ?: throw GradleException("Cần đúng 1 file libs/rogobaseandroid-*.aar")
// rogobaseandroid-debug.aar -> rogobaseandroid-debug-internal.jar (tương tự với release)
val extractedJarName = "${baseAndroidAar.nameWithoutExtension}-internal.jar"
val extractAarJarTask = tasks.register<Copy>("extractAarJar") {
    from(zipTree(baseAndroidAar))
    include("classes.jar")
    into(extractedAarDir)
    rename("classes.jar", extractedJarName)
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)

    // 1. Nhúng các file JAR bình thường
    api(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    // 2. FIXED: Nhúng file JAR đã giải nén
    // We pass the task provider directly to api() or use builtBy separately
    api(files(extractedAarDir.get().file(extractedJarName)) {
        builtBy(extractAarJarTask)
    })

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                val releaseComponent = components.findByName("release")
                if (releaseComponent != null) {
                    from(releaseComponent)
                }

                groupId = "com.github.RogoSolutions"
                artifactId = "IotCoreAndroid"
                version = "1.0.3.46" // Tăng version để JitPack nhận bản mới nhất
            }
        }
    }
}