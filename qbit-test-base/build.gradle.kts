plugins {
    id("org.jetbrains.kotlin.plugin.serialization") version "1.3.50"
}

val serializationVersion by extra { "0.13.0" }

kotlin {

    jvm()
    js("nodeJs")
    linuxX64("linux")

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(kotlin("stdlib-common"))
                api("org.jetbrains.kotlinx:kotlinx-serialization-runtime-common:$serializationVersion")
            }
        }
        val jvmMain by getting {
            dependencies {
                api("org.jetbrains.kotlinx:kotlinx-serialization-runtime:$serializationVersion")
            }
        }
        val linuxMain by getting {
            dependencies {
                api("org.jetbrains.kotlinx:kotlinx-serialization-runtime-native:$serializationVersion")
            }
        }
        val nodeJsMain by getting {
            dependencies {
                api("org.jetbrains.kotlinx:kotlinx-serialization-runtime-js:$serializationVersion")
            }
        }
    }
}

