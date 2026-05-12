import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot")          version "3.3.0" apply false
    id("io.spring.dependency-management")   version "1.1.5" apply false
    kotlin("jvm")                           version "1.9.24" apply false
    kotlin("plugin.spring")                 version "1.9.24" apply false
    id("com.google.cloud.tools.jib")        version "3.4.2" apply false
    id("org.jlleitschuh.gradle.ktlint")     version "12.1.1" apply false
}

allprojects {
    group   = "com.creatorcrm"
    version = "0.1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "21"
            freeCompilerArgs = listOf("-Xjsr305=strict")
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        testLogging {
            events("passed", "failed", "skipped")
        }
    }

    // Shared dependency versions
    val springBootVersion     = "3.3.0"
    val testcontainersVersion = "1.19.8"

    dependencies {
        // Kotlin stdlib — every module needs this
        "implementation"("org.jetbrains.kotlin:kotlin-reflect")
        "implementation"("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

        // Test baseline
        "testImplementation"("org.springframework.boot:spring-boot-starter-test:$springBootVersion")
        "testImplementation"("org.testcontainers:testcontainers:$testcontainersVersion")
        "testImplementation"("org.testcontainers:postgresql:$testcontainersVersion")
        "testImplementation"("org.testcontainers:kafka:$testcontainersVersion")
        "testImplementation"("org.testcontainers:junit-jupiter:$testcontainersVersion")
    }
}
