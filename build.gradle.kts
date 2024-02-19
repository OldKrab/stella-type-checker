plugins {
    kotlin("jvm") version "1.9.22"
    java
    antlr
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
    arguments = arguments + listOf("-package", pkg)
    outputDirectory = outputDirectory.resolve(pkg.split(".").joinToString("/"))
   }

tasks.compileKotlin {
    dependsOn(tasks.generateGrammarSource)
}

// Fat JAR
tasks.jar {
    manifest {
        attributes["Main-Class"] = "org.old.MainKt"
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}


