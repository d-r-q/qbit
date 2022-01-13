val kotlin_serialization_version: String by rootProject.extra
val kotlin_coroutines_version: String by rootProject.extra
val ktor_version: String by rootProject.extra

kotlin {

    jvm {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
                freeCompilerArgs += listOf("-Xopt-in=kotlin.RequiresOptIn")
            }
        }
    }
    js("nodeJs") {
        nodejs()
        compilations.all {
            kotlinOptions {
                freeCompilerArgs += listOf("-Xopt-in=kotlin.RequiresOptIn")
            }
        }
    }
    linuxX64("linux") {
        compilations.all {
            kotlinOptions {
                freeCompilerArgs += listOf("-Xopt-in=kotlin.RequiresOptIn")
            }
        }
    }

    sourceSets {
        all {
            languageSettings.optIn("kotlinx.serialization.ExperimentalSerializationApi")
            languageSettings.optIn("io.ktor.utils.io.core.ExperimentalIoApi")
        }

        val commonMain by getting {
            dependencies {
                api("org.jetbrains.kotlinx:kotlinx-serialization-core:$kotlin_serialization_version")
                api("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlin_coroutines_version")
                api("io.ktor:ktor-io:$ktor_version")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
                implementation(project(":qbit-test-fixtures")) {
                    exclude("org.jetbrains.kotlinx", "kotlinx-coroutines-core:$kotlin_coroutines_version")
                }
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }
        val nodeJsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
    }
}

