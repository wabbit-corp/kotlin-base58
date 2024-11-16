import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val DEV: String by project

repositories {
    mavenCentral()

    maven("https://jitpack.io")
}

group   = "one.wabbit"
version = "1.1.0-SNAPSHOT"

plugins {
    kotlin("multiplatform") version "2.0.20"
    id("maven-publish")
}

kotlin {
    jvm {
        java {
            targetCompatibility = JavaVersion.toVersion(21)
            sourceCompatibility = JavaVersion.toVersion(21)
        }
    }
    js()
    linuxX64()
    mingwX64()

    sourceSets {
        val commonMain by getting {
            kotlin.srcDir("src/main/kotlin")
            resources.srcDir("src/main/resources")

            dependencies {

            }
        }

        val commonTest by getting {
            kotlin.srcDir("src/test/kotlin")
            resources.srcDir("src/test/resources")

            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "one.wabbit"
            artifactId = "kotlin-base58"
            version = "1.0.0"
            from(components["kotlin"])
        }
    }
}

tasks {
    withType<Test> {
        jvmArgs("-ea")
    }
    withType<JavaCompile> {
        options.encoding = Charsets.UTF_8.name()
    }
    withType<Javadoc> {
        options.encoding = Charsets.UTF_8.name()
    }

    withType<KotlinCompile> {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
            freeCompilerArgs.add("-Xcontext-receivers")
        }
    }

    withType<Jar> {
        setProperty("zip64", true)
    }
}
