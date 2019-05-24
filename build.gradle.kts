import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3.11"
}

group = "com.muandrew"
version = "0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    compile(kotlin("stdlib-jdk8"))
    compile("org.apache.pdfbox:pdfbox:2.0.15")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}