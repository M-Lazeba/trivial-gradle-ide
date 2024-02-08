import java.net.URI

plugins {
    kotlin("jvm") version "1.9.21"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        url = URI("https://repo.gradle.org/artifactory/libs-releases")
    }
}

dependencies {
    implementation("org.gradle:gradle-tooling-api:8.4")
//    implementation("org.gradle:gradle-tooling-api:8.6-milestone-1")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}