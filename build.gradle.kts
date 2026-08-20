plugins {
    java
    application
    id("com.gradleup.shadow") version "8.3.5"
}

group = "com.zerog.stellarserverforge"
version = "0.1.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.2")
    implementation("org.bitlet:weupnp:0.1.4")
    implementation("com.electronwill.night-config:toml:3.8.1")
    implementation("net.java.dev.jna:jna-platform:5.15.0")

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

application {
    mainClass.set("com.zerog.stellarserverforge.Main")
}

tasks.test {
    useJUnitPlatform {
        excludeTags("live")
    }
}

val testLive by tasks.registering(Test::class) {
    description = "Runs live network smoke tests (real downloads against Mojang/etc) excluded from the default test task."
    group = "verification"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform {
        includeTags("live")
    }
}

tasks.shadowJar {
    archiveBaseName.set("StellarServerForge")
    archiveClassifier.set("all")
    archiveVersion.set(version.toString())
    manifest {
        attributes["Main-Class"] = "com.zerog.stellarserverforge.Main"
    }
}

val appImageDir = layout.buildDirectory.dir("jpackage")

val jpackage by tasks.registering(Exec::class) {
    group = "distribution"
    description = "Builds a native Windows executable (StellarServerForge.exe) via jpackage."
    dependsOn(tasks.shadowJar)

    doFirst {
        delete(appImageDir)
    }

    val javaHome = System.getProperty("java.home")
    val jpackageBin = if (System.getProperty("os.name").lowercase().contains("win")) "jpackage.exe" else "jpackage"

    commandLine(
        "$javaHome/bin/$jpackageBin",
        "--type", "app-image",
        "--name", "StellarServerForge",
        "--app-version", version.toString(),
        "--vendor", "ZeroG Network",
        "--input", tasks.shadowJar.get().destinationDirectory.get().asFile.absolutePath,
        "--main-jar", tasks.shadowJar.get().archiveFileName.get(),
        "--main-class", "com.zerog.stellarserverforge.Main",
        "--dest", appImageDir.get().asFile.absolutePath
    )
}
