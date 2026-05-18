import net.momirealms.netty

repositories {
    mavenCentral()
    maven("https://jitpack.io/")
    maven("https://libraries.minecraft.net/")
    maven("https://repo.momirealms.net/releases/")
    maven("https://repo.gtemc.net/releases/")
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    implementation(project(":core"))
    implementation(project(":proxy"))
    netty(project)
    // Platform
    compileOnly("net.md-5:bungeecord-api:${rootProject.properties["bungeecord_version"]}")
    compileOnly("org.jetbrains:annotations:${rootProject.properties["jetbrains_annotations_version"]}")
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
