import com.moowork.gradle.node.NodeExtension
import com.moowork.gradle.node.task.NodeTask
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile
import java.net.URI

group = "org.qbit"
version = "0.3.0-SNAPSHOT"

plugins {
    kotlin("multiplatform") version "1.3.61" apply false
    id("com.moowork.node") version "1.3.1" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "1.3.61" apply false
}

subprojects {

    group = rootProject.group
    version = rootProject.version

    apply(plugin = ("kotlin-multiplatform"))
    apply(plugin = ("kotlinx-serialization"))
    apply(plugin = ("com.moowork.node"))

    repositories {
        mavenCentral()
        jcenter()
        maven { url = URI("https://kotlin.bintray.com/kotlinx") }
    }

    if (project.name in setOf("qbit-core")) {
        println("Enabling nodejs tests for ${project.name}")

        configure<NodeExtension> {
            yarnVersion = "1.12.3"
            workDir = file("${rootProject.buildDir}/nodejs")
            nodeModulesDir = file("${rootProject.projectDir}")
        }

        afterEvaluate {

            tasks {
                val yarn by getting
                val compileTestKotlinNodeJs by getting(Kotlin2JsCompile::class)
                val compileKotlinNodeJs by getting(AbstractCompile::class)
                val nodeJsTest by getting

                val populateNodeModulesForTests by creating {
                    dependsOn(yarn, compileKotlinNodeJs)
                    doLast {
                        copy {
                            duplicatesStrategy = DuplicatesStrategy.INCLUDE
                            from(compileKotlinNodeJs.destinationDir)
                            configurations["nodeJsRuntimeClasspath"].forEach {
                                from(zipTree(it.absolutePath).matching { include("*.js") })
                            }
                            configurations["nodeJsTestRuntimeClasspath"].forEach {
                                // quick fix
                                if (it.isFile) {
                                    from(zipTree(it.absolutePath).matching { include("*.js") })
                                }
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
        }
    }
}
