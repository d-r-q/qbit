val kotlin_serialization_version: String by rootProject.extra
val kotlin_io_version: String by rootProject.extra

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
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
                implementation(project(":qbit-test-fixtures"))
            }
        }
        val jvmMain by getting {
            dependencies {
                api(kotlin("stdlib-jdk8"))
                api("org.jetbrains.kotlinx:kotlinx-io-jvm:$kotlin_io_version")
                api("org.jetbrains.kotlinx:kotlinx-serialization-runtime:$kotlin_serialization_version")
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
            }
        }
        val nodeJsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
    }
}

