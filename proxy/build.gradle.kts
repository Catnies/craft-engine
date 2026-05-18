import net.momirealms.adventure
import net.momirealms.nbt
import net.momirealms.netty

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
    adventure(project, JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME)
    // Gson
    implementation("com.google.code.gson:gson:${rootProject.properties["gson_version"]}")
    // Reflection
    implementation(files("${rootProject.rootDir}/libs/jni-internal-lookup-1.9.jar"))
    implementation("net.momirealms:sparrow-reflection:${rootProject.properties["sparrow_reflection_version"]}")
    implementation("it.unimi.dsi:fastutil:${rootProject.properties["fastutil_version"]}")
    implementation("com.github.ben-manes.caffeine:caffeine:${rootProject.properties["caffeine_version"]}")
}