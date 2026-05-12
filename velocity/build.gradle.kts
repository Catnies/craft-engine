plugins {
    id("xyz.jpenilla.run-velocity") version "3.0.2"
}

repositories {
    mavenCentral()
    maven("https://jitpack.io/")
    maven("https://libraries.minecraft.net/")
    maven("https://repo.momirealms.net/releases/")
    maven("https://repo.gtemc.net/releases/")
    maven("https://repo.codemc.io/repository/maven-releases/")
    maven("https://repo.codemc.io/repository/maven-snapshots/")
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    implementation(project(":core"))
    implementation(project(":core:adventure"))
    implementation(rootProject.files("${rootProject.rootDir}/libs/boosted-yaml-${rootProject.properties["boosted_yaml_version"]}.jar"))
    implementation("it.unimi.dsi:fastutil:${rootProject.properties["fastutil_version"]}")
    implementation("com.github.ben-manes.caffeine:caffeine:${rootProject.properties["caffeine_version"]}")

    compileOnly("com.velocitypowered:velocity-api:${rootProject.properties["velocity_version"]}-SNAPSHOT")
    annotationProcessor("com.velocitypowered:velocity-api:${rootProject.properties["velocity_version"]}-SNAPSHOT")
    compileOnly("com.github.retrooper:packetevents-velocity:${rootProject.properties["packet_events_version"]}")
}

tasks {
    shadowJar {
        relocation.applyCommon(this)
        archiveFileName = "${rootProject.name}-velocity-plugin-${rootProject.properties["project_version"]}.jar"
        destinationDirectory.set(file("$rootDir/target"))
    }

    runVelocity {
        velocityVersion("3.5.0-SNAPSHOT")
    }
}

