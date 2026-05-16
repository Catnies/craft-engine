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
    // Platform
    compileOnly("net.md-5:bungeecord-api:${rootProject.properties["bungeecord_version"]}")
    implementation("com.github.retrooper:packetevents-bungeecord:${rootProject.properties["packet_events_version"]}")
}

tasks {
    processResources {
        filesMatching("plugin.yml") {
            expand(rootProject.properties)
        }
    }

    shadowJar {
        relocation.applyProxy(this)
        archiveFileName = "${rootProject.name}-bungeecord-plugin-${rootProject.properties["project_version"]}.jar"
        destinationDirectory.set(file("$rootDir/target"))
    }
}
