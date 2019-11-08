kotlin {

    jvm()
    js("nodeJs")
    linuxX64("linux")

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":qbit-api"))
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val jvmMain by getting {
            dependencies {
                api(kotlin("stdlib-jdk8"))
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
        val nodeJsMain by getting {
            dependencies {
                api(kotlin("stdlib-js"))
            }
        }
        val nodeJsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
    }
}

