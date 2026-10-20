import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Properties

val ktorVersion = project.property("ktorVersion") as String
val logbackVersion = project.property("logbackVersion") as String
val exposedVersion = project.property("exposedVersion") as String
val sqliteDriverVersion = project.property("sqliteDriverVersion") as String

plugins {
    kotlin("jvm") version "2.4.21"
    kotlin("plugin.serialization") version "2.4.21"
    application
}

// The released version, major.minor.patch, from version.properties. It changes only in a release
// (`./gradlew release`); ordinary builds never change it.
val versionPropsFile = file("version.properties")
val versionProps = Properties()
if (versionPropsFile.exists()) {
    versionPropsFile.inputStream().use { versionProps.load(it) }
}
val majorVersion = versionProps.getProperty("majorVersion", "1")
val minorVersion = versionProps.getProperty("minorVersion", "0")
val patchVersion = versionProps.getProperty("patchVersion", "0")

group = "com.retirement"
version = "$majorVersion.$minorVersion.$patchVersion"

// Bump the version for a release: patch by default (1.1.29 -> 1.1.30), or -Pbump=minor (1.2.0) or -Pbump=major (2.0.0)
val bumpPart = providers.gradleProperty("bump").orElse("patch")
tasks.register("release") {
    group = "versioning"
    description = "Bumps the version in version.properties for a release (-Pbump=patch|minor|major, default patch)."
    val propsFile = versionPropsFile
    val current = listOf(majorVersion, minorVersion, patchVersion).map { it.toInt() }
    doLast {
        val (major, minor, patch) = current
        val next = when (val part = bumpPart.get()) {
            "patch" -> listOf(major, minor, patch + 1)
            "minor" -> listOf(major, minor + 1, 0)
            "major" -> listOf(major + 1, 0, 0)
            else -> throw GradleException("-Pbump must be patch, minor or major, not '$part'")
        }
        propsFile.writeText(
            """
            |# The released version, major.minor.patch. Changed only by `./gradlew release` (see CLAUDE.md, Releases).
            |majorVersion=${next[0]}
            |minorVersion=${next[1]}
            |patchVersion=${next[2]}
            |""".trimMargin()
        )
        println("Version ${current.joinToString(".")} -> ${next.joinToString(".")}")
    }
}

// Version info for the running server (ServerInfo reads /version.properties from the classpath). Generated under
// build/, so builds leave no changes in the source tree. gitDescribe identifies the build: the commits since this
// version's release tag and the commit (ServerInfo turns it into SemVer build metadata, e.g. 1.1.29+16.gbff4a24).
val versionInfoDir = layout.buildDirectory.dir("generated/version-info")
val generateVersionInfo = tasks.register("generateVersionInfo") {
    val outputFile = versionInfoDir.map { it.file("version.properties") }
    val versionText = "$majorVersion.$minorVersion.$patchVersion"
    val repoDir = rootDir
    inputs.property("version", versionText)
    outputs.upToDateWhen { false } // the build time changes on every build
    outputs.dir(versionInfoDir)
    doLast {
        val buildTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        // Relative to this version's tag (v1.1.29); before that tag exists, --always gives the commit alone
        val gitDescribe = runCatching {
            val git = ProcessBuilder("git", "describe", "--tags", "--match", "v$versionText", "--dirty", "--always")
                .directory(repoDir).redirectError(ProcessBuilder.Redirect.DISCARD).start()
            val out = git.inputStream.bufferedReader().readText().trim()
            if (git.waitFor() == 0) out else ""
        }.getOrDefault("") // not a git checkout, or git not installed: the build is "unknown"
        outputFile.get().asFile.apply { parentFile.mkdirs() }
            .writeText("version=$versionText\nbuildTime=$buildTime\ngitDescribe=$gitDescribe\n")
        println("Generated version info: $versionText ($gitDescribe) at $buildTime")
    }
}
sourceSets.main {
    resources.srcDir(generateVersionInfo)
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
