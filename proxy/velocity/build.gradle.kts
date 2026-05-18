import net.momirealms.netty

plugins {
    id("xyz.jpenilla.run-velocity") version "3.0.2"
}

repositories {
    mavenCentral()
    maven("https://jitpack.io/")
    maven("https://libraries.minecraft.net/")
    maven("https://repo.momirealms.net/releases/")
    maven("https://repo.gtemc.net/releases/")
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.codemc.io/repository/maven-releases/")
    maven("https://repo.codemc.io/repository/maven-snapshots/")
}

dependencies {
    implementation(project(":core"))
    implementation(project(":proxy"))
    netty(project)
    // Platform
    compileOnly("com.velocitypowered:velocity-api:${rootProject.properties["velocity_version"]}-SNAPSHOT")
    annotationProcessor("com.velocitypowered:velocity-api:${rootProject.properties["velocity_version"]}-SNAPSHOT")
}

tasks {
    runVelocity {
        velocityVersion("3.5.0-SNAPSHOT")
    }

    shadowJar {
        relocation.applyProxy(this)
        archiveFileName = "${rootProject.name}-velocity-plugin-${rootProject.properties["project_version"]}.jar"
        destinationDirectory.set(file("$rootDir/target"))
    }
}
