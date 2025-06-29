import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.21"
    id("org.jetbrains.intellij.platform") version "2.6.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "de.liebki"
version = "0.1.4"

dependencies {
    implementation("io.github.ollama4j:ollama4j:1.0.100")
    intellijPlatform {
        intellijIdeaCommunity("2025.1.3")
    }
}


repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

tasks.named<KotlinJvmCompile>("compileKotlin"){
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }

    patchPluginXml {
        sinceBuild.set("222.3345.118")
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

    shadowJar {
        mergeServiceFiles()
    }

    // Make the build task depend on the shadowJar task
    build {
        dependsOn(shadowJar)
    }

}
