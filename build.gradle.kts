plugins {
    kotlin("jvm") version "1.9.22"
    id("org.jetbrains.kotlin.plugin.spring") version "1.9.22"
    `maven-publish`
}

group = "io.github.denofbits"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // Kotlin
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    
    // Spring Data MongoDB
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb:3.2.1")
    
    // MongoDB Driver
    implementation("org.mongodb:mongodb-driver-sync:4.11.1")
    
    // Testing
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testImplementation("org.springframework.boot:spring-boot-starter-test:3.2.1")
    testImplementation("org.testcontainers:testcontainers:1.19.3")
    testImplementation("org.testcontainers:mongodb:1.19.3")
    testImplementation("org.testcontainers:junit-jupiter:1.19.3")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "io.github.denofbits"
            artifactId = "konduct"
            version = project.version.toString()
            
            from(components["java"])
            
            pom {
                name.set("Konduct")
                description.set("A Kotlin DSL for MongoDB aggregation pipelines")
                url.set("https://github.com/DenOfBits/konduct")
                
                licenses {
                    license {
                        name.set("Apache License 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0")
                    }
                }
                
                developers {
                    developer {
                        id.set("denofbits")
                        name.set("DenOfBits")
                        organization.set("DenOfBits")
                        organizationUrl.set("https://github.com/DenOfBits")
                    }
                }
                
                scm {
                    connection.set("scm:git:git://github.com/DenOfBits/konduct.git")
                    developerConnection.set("scm:git:ssh://github.com/DenOfBits/konduct.git")
                    url.set("https://github.com/DenOfBits/konduct")
                }
            }
        }
    }
}
