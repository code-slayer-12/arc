// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.System.getenv
import java.net.URI

group = "org.eclipse.lmos"
version = project.findProperty("version") as String

plugins {
    kotlin("jvm") version "2.1.21" apply false
    kotlin("plugin.serialization") version "2.1.21" apply false
    id("org.jetbrains.dokka") version "2.0.0"
    id("org.cyclonedx.bom") version "2.3.1"
    id("org.jlleitschuh.gradle.ktlint") version "12.2.0"
    id("org.jetbrains.kotlinx.kover") version "0.9.1"
    id("net.researchgate.release") version "3.1.0"
    id("com.vanniktech.maven.publish") version "0.31.0"
}

subprojects {
    group = "org.eclipse.lmos"

    apply(plugin = "org.jetbrains.dokka")
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "kotlinx-serialization")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "org.jetbrains.kotlinx.kover")
    apply(plugin = "com.vanniktech.maven.publish")

    java {
        sourceCompatibility = JavaVersion.VERSION_21
    }

    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        debug.set(true)
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs += "-Xjsr305=strict"
            freeCompilerArgs += "-Xcontext-receivers"
            jvmTarget = "21"
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    val javadocJar: TaskProvider<Jar> by tasks.registering(Jar::class) {
        dependsOn(tasks.dokkaJavadoc)
        from(tasks.dokkaJavadoc.flatMap { it.outputDirectory })
        archiveClassifier.set("javadoc")
    }

    if (project.name != "arc-gradle-plugin") {
        mavenPublishing {
            publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, automaticRelease = true)
            signAllPublications()

            pom {
                name = "ARC"
                description = "ARC is an AI framework."
                url = "https://github.com/eclipse-lmos/arc"
                licenses {
                    license {
                        name = "Apache-2.0"
                        distribution = "repo"
                        url = "https://github.com/eclipse-lmos/arc/blob/main/LICENSES/Apache-2.0.txt"
                    }
                }
                developers {
                    developer {
                        id = "pat"
                        name = "Patrick Whelan"
                        email = "opensource@telekom.de"
                    }
                    developer {
                        id = "bharat_bhushan"
                        name = "Bharat Bhushan"
                        email = "opensource@telekom.de"
                    }
                    developer {
                        id = "merrenfx"
                        name = "Max Erren"
                        email = "opensource@telekom.de"
                    }
                    developer {
                        id = "jas34"
                        name = "Jasbir Singh"
                        email = "jasbirsinghkamboj@gmail.com"
                    }
                    developer {
                        id = "harishsainik"
                        name = "Harish Kumar Saini"
                        email = "harishsaini.lajak@gmail.com"
                    }
                }
                scm {
                    url = "https://github.com/eclipse-lmos/arc.git"
                }
            }

            repositories {
                maven {
                    name = "GitHubPackages"
                    url = URI("https://maven.pkg.github.com/eclipse-lmos/arc")
                    credentials {
                        username = findProperty("GITHUB_USER")?.toString() ?: getenv("GITHUB_USER")
                        password = findProperty("GITHUB_TOKEN")?.toString() ?: getenv("GITHUB_TOKEN")
                    }
                }
            }
        }
    }

    if (!project.name.endsWith("-bom")) {
        dependencies {
            "implementation"(rootProject.libs.kotlinx.coroutines.slf4j)
            "implementation"(rootProject.libs.kotlinx.coroutines.jdk8)
            "implementation"(rootProject.libs.kotlinx.coroutines.reactor)
            "implementation"(rootProject.libs.kotlinx.serialization.json)

            // Testing
            "testImplementation"("org.junit.jupiter:junit-jupiter:5.13.0")
            "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
            "testImplementation"("org.assertj:assertj-core:3.27.3")
            "testImplementation"("io.mockk:mockk:1.14.2")
        }
    }

    repositories {
        mavenLocal()
        mavenCentral()
        google()
    }

    tasks.named("dokkaJavadoc") {
        mustRunAfter("checksum")
    }
}

dependencies {
    kover(project("arc-scripting"))
    kover(project("arc-azure-client"))
    kover(project("arc-result"))
    kover(project("arc-readers"))
    kover(project("arc-agents"))
    kover(project("arc-spring-boot-starter"))
    kover(project("arc-memory-mongo-spring-boot-starter"))
    kover(project("arc-api"))
    kover(project("arc-graphql-spring-boot-starter"))
    kover(project(("arc-openai-api-spring-boot-starter")))
    kover(project("arc-agent-client"))
    kover(project("arc-assistants"))
    kover(project("arc-langchain4j-client"))
    kover(project("arc-memory-redis"))
    kover(project("arc-mcp"))
}

repositories {
    mavenLocal()
    mavenCentral()
}

fun Project.java(configure: Action<JavaPluginExtension>): Unit =
    (this as ExtensionAware).extensions.configure("java", configure)

fun String.execWithCode(workingDir: File? = null): Pair<CommandResult, Sequence<String>> {
    ProcessBuilder().apply {
        workingDir?.let { directory(it) }
        command(split(" "))
        redirectErrorStream(true)
        val process = start()
        val result = process.readStream()
        val code = process.waitFor()
        return CommandResult(code) to result
    }
}

class CommandResult(val code: Int) {

    val isFailed = code != 0
    val isSuccess = !isFailed

    fun ifFailed(block: () -> Unit) {
        if (isFailed) block()
    }
}

/**
 * Executes a string as a command.
 */
fun String.exec(workingDir: File? = null) = execWithCode(workingDir).second

fun Project.isBOM() = name.endsWith("-bom")

private fun Process.readStream() = sequence<String> {
    val reader = BufferedReader(InputStreamReader(inputStream))
    try {
        var line: String?
        while (true) {
            line = reader.readLine()
            if (line == null) {
                break
            }
            yield(line)
        }
    } finally {
        reader.close()
    }
}

release {
    ignoredSnapshotDependencies = listOf("org.springframework.ai:spring-ai-bom")
    newVersionCommitMessage = "New Snapshot-Version:"
    preTagCommitMessage = "Release:"
}
