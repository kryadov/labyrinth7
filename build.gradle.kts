plugins {
    id("java")
    id("application")
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("org.beryx.jlink") version "3.1.3"
}

group = "com.kayar.yetanotherlabyrinth"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

// Select JavaFX platform classifier based on OS or -PjavafxPlatform=win|mac|linux
val osName = System.getProperty("os.name").lowercase()
val osArch = System.getProperty("os.arch").lowercase()
val defaultJavafxPlatform = when {
    osName.contains("win") -> "win"
    osName.contains("mac") || osName.contains("darwin") -> if (osArch == "aarch64") "mac-aarch64" else "mac"
    else -> "linux"
}
val javafxPlatform: String = (findProperty("javafxPlatform") as String?) ?: defaultJavafxPlatform
val javafxVersion = "21"
println("Using JavaFX platform classifier: $javafxPlatform")

dependencies {
    implementation("com.github.almasb:fxgl:21.1") {
        exclude(group = "org.openjfx")
    }

    // JavaFX 21 for selected platform (override with -PjavafxPlatform=win|mac|linux)
    implementation("org.openjfx:javafx-base:$javafxVersion:$javafxPlatform")
    implementation("org.openjfx:javafx-graphics:$javafxVersion:$javafxPlatform")
    implementation("org.openjfx:javafx-controls:$javafxVersion:$javafxPlatform")
    implementation("org.openjfx:javafx-media:$javafxVersion:$javafxPlatform")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

application {
    mainClass.set("com.kayar.yetanotherlabyrinth.LabyrinthApp")
}

// Configure Shadow JAR to produce a single executable file with all dependencies
tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    archiveBaseName.set("yetanotherlabyrinth")
    archiveClassifier.set("")
    mergeServiceFiles()
    manifest {
        attributes(mapOf(
            "Main-Class" to application.mainClass.get(),
            "Implementation-Title" to "YetAnotherLabyrinth",
            "Implementation-Version" to version
        ))
    }
}

val jdk21Launcher = javaToolchains.launcherFor {
    languageVersion.set(JavaLanguageVersion.of(21))
}

jlink {
    // Non-modular project using merged module; set explicit module name and align with application.mainModule
    moduleName.set("com.kayar.yetanotherlabyrinth.merged.module")
    imageName.set("yetanotherlabyrinth-$javafxPlatform")
    // Make sure jlink uses JDK 21 toolchain
    javaHome.set(jdk21Launcher.get().metadata.installationPath.asFile.absolutePath)
    options.set(listOf("--strip-debug", "--compress", "2", "--no-header-files", "--no-man-pages", "--bind-services"))
    launcher {
        name = "yetanotherlabyrinth"
    }
}

// Make `build` also produce the shadow jar and the jlink distribution zip
tasks.named("build") {
    dependsOn(tasks.named("shadowJar"))
    dependsOn(tasks.named("jlinkZip"))
}

tasks.test {
    useJUnitPlatform()
}
