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
                api("org.jetbrains.kotlinx:kotlinx-serialization-core:1.0.1")
                api("io.ktor:ktor-io:$ktor_version")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
                implementation(project(":qbit-test-fixtures")){
                    exclude("org.jetbrains.kotlinx", "kotlinx-coroutines-core:1.4.1")
                }
            }
        }
        val jvmMain by getting {
            dependencies {
                api(kotlin("stdlib-jdk8"))
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
                api("io.ktor:ktor-io-native:1.3.1") {
                    exclude("org.jetbrains.kotlinx", "kotlinx-coroutines-core")
                    exclude("org.jetbrains.kotlinx", "kotlinx-coroutines-core-native")
                }
            }
        }
        val linuxTest by getting {
            dependencies {
            }
        }
        val nodeJsMain by getting {
            dependencies {
                api(kotlin("stdlib-js"))
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

