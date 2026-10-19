import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Properties

val ktorVersion = project.property("ktorVersion") as String
val logbackVersion = project.property("logbackVersion") as String
val exposedVersion = project.property("exposedVersion") as String
val sqliteDriverVersion = project.property("sqliteDriverVersion") as String

plugins {
    kotlin("jvm") version "2.3.0"
    kotlin("plugin.serialization") version "2.3.0"
    application
}

// Load version from properties file
val versionPropsFile = file("version.properties")
val versionProps = Properties()
if (versionPropsFile.exists()) {
    versionProps.load(versionPropsFile.inputStream())
}
val majorVersion = versionProps.getProperty("majorVersion", "1")
val minorVersion = versionProps.getProperty("minorVersion", "0")
val buildNumber = versionProps.getProperty("buildNumber", "1")

group = "com.retirement"
version = "$majorVersion.$minorVersion.$buildNumber"

// Task to increment build number (run manually before commit)
tasks.register("incrementBuildNumber") {
    doLast {
        val currentBuild = versionProps.getProperty("buildNumber", "1").toInt()
        versionProps.setProperty("buildNumber", (currentBuild + 1).toString())
        versionProps.store(versionPropsFile.outputStream(), "Auto-incremented build number")
        println("Build number incremented to ${currentBuild + 1}")
    }
}


// Generate version info file for runtime access
tasks.register("generateVersionInfo") {
    val outputDir = file("src/main/resources")
    val outputFile = file("$outputDir/version.properties")

    doLast {
        outputDir.mkdirs()
        val buildTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        outputFile.writeText("""
            version=$majorVersion.$minorVersion.$buildNumber
            buildTime=$buildTime
        """.trimIndent())
        println("Generated version info: $majorVersion.$minorVersion.$buildNumber at $buildTime")
    }
}

// Generate version info before compiling
tasks.named("processResources") {
    dependsOn("generateVersionInfo")
}

application {
    mainClass.set("com.retirement.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment", "--enable-native-access=ALL-UNNAMED")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-netty-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-cors-jvm:$ktorVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    
    // Exposed & SQLite
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion") // Check Maven for the latest version
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.xerial:sqlite-jdbc:$sqliteDriverVersion")
    
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation(kotlin("reflect")) // TsContractGenerator: sealed subclass names
}

kotlin {
     jvmToolchain(25)
}

// The SQLite driver loads a native library (as for ./gradlew run above)
tasks.test {
    // Timing tests (tag "perf", spec 006 SC006-005) run only with -Pperf, so CI timing noise can't fail a build
    useJUnitPlatform { if (!project.hasProperty("perf")) excludeTags("perf") }
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}

// Generate TypeScript API contract types for frontend/ and backend/ from the Kotlin models.
// ContractTypesTest fails if the generated files are out of date.
tasks.register<JavaExec>("generateTsContract") {
    group = "build"
    description = "Generates frontend/backend TypeScript contract types from Kotlin @Serializable models"
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("com.retirement.contract.TsContractGeneratorKt")
    workingDir = projectDir
}
