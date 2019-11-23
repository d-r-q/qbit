val coroutinesVersion by extra { "1.3.2-1.3.60" }

kotlin {

    jvm()
    js("nodeJs")
    linuxX64("linux")

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(kotlin("stdlib-common"))
                api("org.jetbrains.kotlinx:kotlinx-coroutines-core-common:$coroutinesVersion")
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
                api("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
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
                api("org.jetbrains.kotlinx:kotlinx-coroutines-core-native:$coroutinesVersion")
            }
        }
        val linuxTest by getting {
            dependencies {
            }
        }
        val nodeJsMain by getting {
            dependencies {
                api(kotlin("stdlib-js"))
                api("org.jetbrains.kotlinx:kotlinx-coroutines-core-js:$coroutinesVersion")
            }
        }
        val nodeJsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
    }
}

