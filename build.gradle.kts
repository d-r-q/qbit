import com.moowork.gradle.node.NodeExtension
import com.moowork.gradle.node.task.NodeTask
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile
import java.net.URI

buildscript {
    dependencies {
        classpath("org.jetbrains.kotlinx:atomicfu-gradle-plugin:0.14.4")
    }
}

group = "org.qbit"
version = "0.3.0-SNAPSHOT"

plugins {
    val kotlin_version: String by System.getProperties()
    kotlin("multiplatform") version kotlin_version apply false
    id("com.moowork.node") version "1.3.1" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version kotlin_version apply false
}

subprojects {

    group = rootProject.group
    version = rootProject.version

    apply(plugin = ("kotlin-multiplatform"))
    apply(plugin = ("kotlinx-serialization"))
    apply(plugin = ("kotlinx-atomicfu"))
    apply(plugin = ("com.moowork.node"))

    repositories {
        mavenCentral()
        jcenter()
        maven { url = URI("https://kotlin.bintray.com/kotlinx") }
    }

}
