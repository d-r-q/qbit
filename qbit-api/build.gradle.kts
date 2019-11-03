import com.moowork.gradle.node.NodeExtension
import com.moowork.gradle.node.task.NodeTask
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile

plugins {
    kotlin("multiplatform") version "1.3.50"
    id("com.moowork.node") version "1.3.1"
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

configure<NodeExtension> {
    yarnVersion = "1.12.3"
    workDir = file("${rootProject.buildDir}/nodejs")
    nodeModulesDir = file("${rootProject.projectDir}")
}

tasks {
    val yarn by getting
    val compileTestKotlinNodeJs by getting(Kotlin2JsCompile::class)
    val compileKotlinNodeJs by getting(AbstractCompile::class)
    val nodeJsTest by getting

    val populateNodeModulesForTests by creating {
        dependsOn(yarn, compileKotlinNodeJs)
        doLast {
            copy {
                from(compileKotlinNodeJs.destinationDir)
                configurations["nodeJsRuntimeClasspath"].forEach {
                    from(zipTree(it.absolutePath).matching { include("*.js") })
                }
                configurations["nodeJsTestRuntimeClasspath"].forEach {
                    from(zipTree(it.absolutePath).matching { include("*.js") })
                }

                into("$rootDir/node_modules")
            }
        }
    }

    val runTestsWithMocha by creating(NodeTask::class) {
        dependsOn(populateNodeModulesForTests)
        setScript(file("$rootDir/node_modules/mocha/bin/mocha"))
        setArgs(
            listOf(
                compileTestKotlinNodeJs.outputFile,
                "--reporter-options",
                "topLevelSuite=${project.name}-tests"
            )
        )
    }

    nodeJsTest.dependsOn(runTestsWithMocha, compileTestKotlinNodeJs)
}
