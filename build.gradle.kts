import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

repositories {
    mavenCentral()

    maven("https://jitpack.io")
}

group   = "one.wabbit"
version = "1.1.1"

plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka")
    id("org.jetbrains.kotlinx.kover")

    id("maven-publish")

    id("com.vanniktech.maven.publish")
}

mavenPublishing {
    coordinates("one.wabbit", "kotlin-base58", "1.1.1")
    publishToMavenCentral()
    signAllPublications()
    pom {
        name.set("kotlin-base58")
        description.set("kotlin-base58")
        url.set("https://github.com/wabbit-corp/kotlin-base58")
        licenses {
            license {
                name.set("GNU Affero General Public License v3.0 or later")
                url.set("https://spdx.org/licenses/AGPL-3.0-or-later.html")
            }
        }
        developers {
            developer {
                id.set("wabbit-corp")
                name.set("Wabbit Consulting Corporation")

                email.set("wabbit@wabbit.one")

            }
        }
        scm {
            url.set("https://github.com/wabbit-corp/kotlin-base58")
            connection.set("scm:git:git://github.com/wabbit-corp/kotlin-base58.git")
            developerConnection.set("scm:git:ssh://git@github.com/wabbit-corp/kotlin-base58.git")
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

val configuredVersionString = version.toString()

tasks.register("printVersion") {
    inputs.property("configuredVersion", configuredVersionString)
    doLast {
        println(inputs.properties["configuredVersion"])
    }
}

tasks.register("assertReleaseVersion") {
    inputs.property("configuredVersion", configuredVersionString)
    doLast {
        val versionString = inputs.properties["configuredVersion"].toString()
        require(!versionString.endsWith("+dev-SNAPSHOT")) {
            "Release publishing requires a non-snapshot version, got $versionString"
        }
        val refType = System.getenv("GITHUB_REF_TYPE") ?: ""
        val refName = System.getenv("GITHUB_REF_NAME") ?: ""
        if (refType == "tag" && refName.isNotBlank()) {
            val expectedTag = "v$versionString"
            require(refName == expectedTag) {
                "Git tag $refName does not match project version $versionString"
            }
        }
    }
}

tasks.register("assertSnapshotVersion") {
    inputs.property("configuredVersion", configuredVersionString)
    doLast {
        val versionString = inputs.properties["configuredVersion"].toString()
        require(versionString.endsWith("+dev-SNAPSHOT")) {
            "Snapshot publishing requires a +dev-SNAPSHOT version, got $versionString"
        }
        require((System.getenv("GITHUB_REF_TYPE") ?: "") != "tag") {
            "Snapshot publishing must not run from a tag ref"
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
