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
    js("nodeJs")
    linuxX64("linux")

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(kotlin("stdlib-common"))
                api("org.jetbrains.kotlinx:kotlinx-serialization-runtime-common:$kotlin_serialization_version")
                api("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlin_coroutines_version")
                api("org.jetbrains.kotlinx:kotlinx-coroutines-core-common:$kotlin_coroutines_version")

                api(kotlin("test-common"))
                api(kotlin("test-annotations-common"))
            }
        }
        val jvmMain by getting {
            dependencies {
                api(kotlin("stdlib-jdk8"))
                api("org.jetbrains.kotlinx:kotlinx-serialization-runtime:$kotlin_serialization_version")

                implementation(kotlin("test-junit"))
            }
        }
        val linuxMain by getting {
            dependencies {
                api(kotlin("stdlib"))
                api("org.jetbrains.kotlinx:kotlinx-serialization-runtime-native:$kotlin_serialization_version")
                api("org.jetbrains.kotlinx:kotlinx-coroutines-core-native:$kotlin_coroutines_version")
            }
        }
        val nodeJsMain by getting {
            dependencies {
                api(kotlin("stdlib-js"))
                api("org.jetbrains.kotlinx:kotlinx-serialization-runtime-js:$kotlin_serialization_version")
                api("org.jetbrains.kotlinx:kotlinx-coroutines-core-js:$kotlin_coroutines_version")
                implementation(kotlin("test-js"))
            }
        }
    }
}

