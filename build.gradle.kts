import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import java.io.File
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.api.Task

buildscript {
    repositories {
        mavenCentral()
        google()
    }
}

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.3.21"
    id("org.jetbrains.intellij.platform") version "2.16.0"
}

group = "de.liebki"
version = "2026.5.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    implementation("org.duckdb:duckdb_jdbc:1.5.2.1")
    implementation("io.github.ollama4j:ollama4j:1.1.7")
    implementation("org.json:json:20251224")
    implementation("com.google.code.gson:gson:2.14.0")

    intellijPlatform {
        intellijIdea("2026.1.1")
    }
}

tasks.named<KotlinJvmCompile>("compileKotlin") {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }

    patchPluginXml {
        sinceBuild.set("251.3345.118")
        untilBuild = provider { null }
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }

    build {
        dependsOn("prepareSandbox")
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}
