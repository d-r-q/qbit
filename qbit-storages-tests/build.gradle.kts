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
        val commonTest by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
                implementation(project(":qbit-core"))
                implementation(project(":qbit-test-fixtures"))

                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
                implementation(project(":qbit-fss"))
            }
        }
        val linuxTest by getting {
            dependencies {
                api(kotlin("stdlib"))
            }
        }
        val nodeJsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
    }
}
