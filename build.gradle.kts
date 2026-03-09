import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

repositories {
    mavenCentral()
}

group   = "one.wabbit"
version = "1.1.1"

plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka")
    id("org.jetbrains.kotlinx.kover")

    id("maven-publish")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "one.wabbit"
            artifactId = "kotlin-base58"
            version = "1.1.1"
            from(components["java"])
        }
    }
}

dependencies {
    testImplementation(kotlin("test"))
}

java {
    targetCompatibility = JavaVersion.toVersion(21)
    sourceCompatibility = JavaVersion.toVersion(21)
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

            freeCompilerArgs.add("-Xcontext-parameters")

        }
    }

    jar {
        setProperty("zip64", true)

    }
}

// Kover Configuration
kover {
    // useJacoco() // This is the default, can be specified if you want to be explicit
    // reports {
    //     // Configure reports for the default test task.
    //     // Kover tries to infer the variant for simple JVM projects.
    //     // If you have specific build types/flavors, you'd configure them here as variants.
    //     variant() { // Or remove "debug" for a default JVM setup unless you have variants
    //         html {
    //             // reportDir.set(layout.buildDirectory.dir("reports/kover/html")) // Uncomment to customize output
    //             // title.set("kotlin-base58 Code Coverage") // Uncomment to customize title
    //         }
    //         xml {
    //             // reportFile.set(layout.buildDirectory.file("reports/kover/coverage.xml")) // Uncomment to customize output
    //         }
    //     }
    // }
}

dokka {
    moduleName.set("kotlin-base58")
    dokkaPublications.html {
        suppressInheritedMembers.set(true)
        failOnWarning.set(true)
    }
    dokkaSourceSets.main {
        // includes.from("README.md")

        sourceLink {
            localDirectory.set(file("src/main/kotlin"))
            remoteUrl("https://github.com/wabbit-corp/kotlin-base58/tree/master/src/main/kotlin")
            remoteLineSuffix.set("#L")
        }

    }
    pluginsConfiguration.html {
        // customStyleSheets.from("styles.css")
        // customAssets.from("logo.png")
        footerMessage.set("(c) Wabbit Consulting Corporation")
    }
}
