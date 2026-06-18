plugins {
  `java-library`
  `maven-publish`

  id("com.diffplug.spotless")
  id("com.gradleup.shadow")
}

group = "com.unityrealms.skyblock.miningmodule"
version = property("project.version").toString()

spotless {
  java {
    licenseHeaderFile(rootProject.file("LICENSE_HEADER"))

    target(
      "api/src/main/java/**/*.java",
      "api/src/test/java/**/*.java",
      "core/src/main/java/**/*.java",
      "core/src/test/java/**/*.java",
      "core/mine/src/main/java/**/*.java",
      "core/mine/src/test/java/**/*.java",
      "core/tool/src/main/java/**/*.java",
      "core/tool/src/test/java/**/*.java",
      "core/tool/enchantment/src/main/java/**/*.java",
      "core/tool/enchantment/src/test/java/**/*.java"
    )
  }

  kotlin {
    licenseHeaderFile(rootProject.file("LICENSE_HEADER"))

    target(
      "api/src/main/kotlin/**/*.kt",
      "api/src/test/kotlin/**/*.kt",
      "core/src/main/kotlin/**/*.kt",
      "core/src/test/kotlin/**/*.kt",
      "core/mine/src/main/kotlin/**/*.kt",
      "core/mine/src/test/kotlin/**/*.kt",
      "core/tool/src/main/kotlin/**/*.kt",
      "core/tool/src/test/kotlin/**/*.kt",
      "core/tool/enchantment/src/main/kotlin/**/*.kt",
      "core/tool/enchantment/src/test/kotlin/**/*.kt"
    )
  }
}

publishing {
  publications {
    create<MavenPublication>("mavenJava") {
      artifact(tasks.named("shadowJar").get())

      val javadocJar by tasks.registering(Jar::class) {
        description = "Assembles a JAR archive containing the Javadoc API documentation for the main source code."
        archiveClassifier.set("javadoc")
        from(tasks.named("javadoc"))
      }

      val sourcesJar by tasks.registering(Jar::class) {
        description = "Assembles a JAR archive containing the source code for the main source set."
        archiveClassifier.set("sources")
        from(sourceSets.main.get().allSource)
      }

      artifact(javadocJar)
      artifact(sourcesJar)
    }
  }
}
