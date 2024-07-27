import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import java.util.regex.Pattern

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version "1.9.22"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("maven-publish")
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(project(":runtime"))
    compileOnly(project(":instrumentation"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    implementation("com.github.ajalt.clikt:clikt:4.2.2")
}

tasks.test {
    useJUnitPlatform()
}

tasks.named<ShadowJar>("shadowJar") {
    manifest {
        attributes(mapOf("Main-Class" to "cmu.pasta.fray.core.MainKt"))
    }
}


tasks.named("build") {
  dependsOn("shadowJar")
  finalizedBy("genRunner")
}

tasks.withType<JavaExec> {
  dependsOn(":jvmti:build")
  dependsOn(":jdk:build")
  val instrumentationTask = evaluationDependsOn(":instrumentation").tasks.named("shadowJar").get()
  val agentPath: String by rootProject.extra
  val jdk = project(":jdk")
  val instrumentation = instrumentationTask.outputs.files.first().absolutePath
  classpath = tasks.named("shadowJar").get().outputs.files
  executable("${jdk.layout.buildDirectory.get().asFile}/java-inst/bin/java")
  mainClass = "cmu.pasta.fray.core.MainKt"
  jvmArgs("-agentpath:$agentPath")
  jvmArgs("-javaagent:$instrumentation")
  jvmArgs("-ea")
  jvmArgs("--add-opens", "java.base/java.lang=ALL-UNNAMED")
  jvmArgs("--add-opens", "java.base/java.util.concurrent.atomic=ALL-UNNAMED")
  jvmArgs("--add-opens", "java.base/java.util=ALL-UNNAMED")
  jvmArgs("--add-opens", "java.base/java.io=ALL-UNNAMED")
  jvmArgs("--add-opens", "java.base/sun.nio.ch=ALL-UNNAMED")
  jvmArgs("--add-opens", "java.base/java.lang.reflect=ALL-UNNAMED")
  doFirst {
    // Printing the full command
    println("Executing command: ${executable} ${jvmArgs!!.joinToString(" ")} -cp ${classpath.asPath} ${mainClass.get()} ${args!!.joinToString(" ")}")
  }
}

tasks.register<JavaExec>("runFray") {
  var configPath = properties["configPath"] as String? ?: ""
  val extraArgs = when (val extraArgs = properties["extraArgs"]) {
    is String -> {
      val pattern = Pattern.compile("""("[^"]+"|\S+)""")
      val matcher = pattern.matcher(extraArgs)
      val result = mutableListOf<String>()
      while (matcher.find()) {
        result.add(matcher.group(1).replace("\"", ""))
      }
      result
    }
    else -> emptyList()
  }
  if (!File(configPath).isAbsolute) {
    configPath = System.getProperty("user.dir") + "/" + configPath
  }
  args = listOf("--run-config", "json", "--config-path", configPath) + extraArgs
}

tasks.create("genRunner") {
  doLast {
    val instrumentationTask = evaluationDependsOn(":instrumentation").tasks.named("shadowJar").get()
    val instrumentation = instrumentationTask.outputs.files.first().absolutePath
    val core = tasks.named("shadowJar").get().outputs.files.first().absolutePath
    val agentPath: String by rootProject.extra
    val binDir = "${rootProject.projectDir.absolutePath}/bin"
    var runner = file("${binDir}/fray.template").readText()
    runner = runner.replace("#JVM_TI_PATH#", agentPath)
    runner = runner.replace("#AGENT_PATH#", instrumentation)
    runner = runner.replace("#CORE_PATH#", core)
    val file = File("${binDir}/fray")
    file.writeText(runner)
    file.setExecutable(true)
  }
}
