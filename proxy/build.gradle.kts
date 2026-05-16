import net.momirealms.adventure
import net.momirealms.nbt
import net.momirealms.netty
import org.gradle.kotlin.dsl.exclude

repositories {
    mavenCentral()
    maven("https://jitpack.io/")
    maven("https://libraries.minecraft.net/")
    maven("https://repo.momirealms.net/releases/")
    maven("https://repo.gtemc.net/releases/")
    maven("https://repo.codemc.io/repository/maven-releases/")
    maven("https://repo.codemc.io/repository/maven-snapshots/")
}

dependencies {
    implementation(project(":core"))
    nbt(project, JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME)
    netty(project, JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME)
    // Yaml
    implementation(files("${rootProject.rootDir}/libs/boosted-yaml-${rootProject.properties["boosted_yaml_version"]}.jar"))
    // Reflection
    implementation(files("${rootProject.rootDir}/libs/jni-internal-lookup-1.9.jar"))
    implementation("net.momirealms:sparrow-reflection:${rootProject.properties["sparrow_reflection_version"]}")
    implementation("it.unimi.dsi:fastutil:${rootProject.properties["fastutil_version"]}")
    implementation("com.github.ben-manes.caffeine:caffeine:${rootProject.properties["caffeine_version"]}")
    // Adventure
    implementation("net.kyori:adventure-api:4.26.1")
    implementation("net.kyori:adventure-text-minimessage:4.26.1")
    implementation("net.kyori:adventure-text-serializer-json-legacy-impl:4.26.1")
    implementation("net.kyori:adventure-text-serializer-legacy:4.26.1")
    implementation("net.kyori:adventure-text-serializer-gson:4.26.1").apply {
        (this as? ExternalModuleDependency)?.exclude("com.google.code.gson", "gson")
    }
    // Packet
    compileOnly("com.github.retrooper:packetevents-velocity:${rootProject.properties["packet_events_version"]}")
    compileOnly("com.github.retrooper:packetevents-bungeecord:${rootProject.properties["packet_events_version"]}")
}