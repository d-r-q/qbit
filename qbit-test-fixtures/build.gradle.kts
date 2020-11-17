val kotlin_serialization_version: String by rootProject.extra
val kotlin_coroutines_version: String by rootProject.extra

kotlin {

    jvm {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.6"
            }
        }
    }
    js("nodeJs") {
        nodejs()
    }
    linuxX64("linux")

    sourceSets {
        val commonMain by getting {
            dependencies {
                api("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlin_coroutines_version")
                api("org.jetbrains.kotlinx:kotlinx-serialization-core:$kotlin_serialization_version")

                api(kotlin("test-common"))
                api(kotlin("test-annotations-common"))
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }
        val nodeJsMain by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
    }
}

