import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.22"
    java
    antlr
    application
    id("com.adarshr.test-logger") version "4.0.0"
}


group = "org.old"
version = "1.0-SNAPSHOT"

kotlin {
    jvmToolchain(17)
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.8.10")
    antlr("org.antlr:antlr4:4.13.1")

}

tasks.test {
    useJUnitPlatform()
}

tasks.generateGrammarSource {
    val pkg = "org.old.grammar"
    arguments = arguments + listOf("-package", pkg, "-visitor")
    outputDirectory = outputDirectory.resolve(pkg.split(".").joinToString("/"))
}

tasks.withType<KotlinCompile>().configureEach {
    dependsOn(tasks.withType<AntlrTask>())
}

val mainClassPath = "org.old.MainKt"

application {
    mainClass = mainClassPath
}



