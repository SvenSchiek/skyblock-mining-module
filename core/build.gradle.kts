plugins {
  `java-library`

  id("com.gradleup.shadow")

  kotlin("jvm")
}

val javaVersion: String = property("java.version").toString()
val minecraftVersion: String = property("minecraft.version").toString()

val serverMessageFrameworkVersion: String = property("server.message-framework.version").toString()

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(javaVersion))
  }
}

dependencies {
  compileOnly("com.unityrealms.server.messageframework:server-message-framework:${serverMessageFrameworkVersion}")
  compileOnly("org.purpurmc.purpur:purpur-api:${minecraftVersion}-R0.1-SNAPSHOT")

  implementation(
    project(":api")
  )
  implementation(
    project(":core:mine")
  )
  implementation(
    project(":core:tool")
  )
  implementation(
    project(":core:tool:enchantment")
  )
}

tasks {
  jar {
    enabled = false
  }

  shadowJar {
    archiveClassifier.set("")
    archiveBaseName.set(rootProject.name)
    archiveVersion.set("${rootProject.version}")

    dependencies {
      exclude(
        dependency("org.jetbrains.kotlin:kotlin-reflect")
      )

      exclude(
        dependency("org.jetbrains.kotlin:kotlin-stdlib")
      )

      exclude(
        dependency("org.jetbrains.kotlin:kotlin-stdlib-jdk7")
      )

      exclude(
        dependency("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
      )
    }

    minimize()
  }

  val kotlinBuildScriptModelTask = rootProject.tasks.findByName("prepareKotlinBuildScriptModel")

  if (kotlinBuildScriptModelTask == null) {
    register("prepareKotlinBuildScriptModel") {
      group = "build setup"
      description = "Prepares the Kotlin build script model."
    }
  }
}
