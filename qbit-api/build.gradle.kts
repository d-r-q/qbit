plugins {
    kotlin("multiplatform") version "1.3.50"
}

group = "org.qbit"
version = "2.0-SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin {

    jvm()
    js("nodeJs")
    linuxX64("linux")

    println(sourceSets.names)

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(kotlin("stdlib-common"))

            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
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
            }
        }
        val linuxTest by getting {
            dependencies {
            }
        }
    }
}