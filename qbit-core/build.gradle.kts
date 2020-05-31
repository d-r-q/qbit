val kotlin_serialization_version: String by rootProject.extra
val ktor_version: String by rootProject.extra

kotlin {

    jvm {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.6"
                freeCompilerArgs = listOf("-XXLanguage:+InlineClasses")
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
                api("io.ktor:ktor-io:$ktor_version")
                implementation("org.jetbrains.kotlinx:atomicfu-common:0.14.2-1.4-M1")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
                implementation(project(":qbit-test-fixtures")){
                    exclude("org.jetbrains.kotlinx", "kotlinx-coroutines-core:1.3.5-1.4-M1")
                }
            }
        }
        val jvmMain by getting {
            dependencies {
                api(kotlin("stdlib-jdk8"))
                api("org.jetbrains.kotlinx:kotlinx-serialization-runtime:$kotlin_serialization_version")
                api("io.ktor:ktor-io-jvm:$ktor_version")
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }
        val linuxMain by getting {
            dependencies {
                api(kotlin("stdlib"))
                api("org.jetbrains.kotlinx:kotlinx-serialization-runtime-native:$kotlin_serialization_version")
                api("io.ktor:ktor-io-native:$ktor_version") {
                    exclude("org.jetbrains.kotlinx", "kotlinx-coroutines-core")
                    exclude("org.jetbrains.kotlinx", "kotlinx-coroutines-core-native")
                }
                api("org.jetbrains.kotlinx:kotlinx-coroutines-core-native:1.3.5-native-mt-1.4-M1")
            }
        }
        val linuxTest by getting {
            dependencies {
            }
        }
        val nodeJsMain by getting {
            dependencies {
                api(kotlin("stdlib-js"))
                api("org.jetbrains.kotlinx:kotlinx-serialization-runtime-js:$kotlin_serialization_version")
                api("io.ktor:ktor-io-js:$ktor_version")
            }
        }
        val nodeJsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
    }
}

