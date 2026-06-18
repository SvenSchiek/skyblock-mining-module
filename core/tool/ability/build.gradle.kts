plugins {
  `java-library`

  kotlin("jvm")
}

val javaVersion: String = property("java.version").toString()
val minecraftVersion: String = property("minecraft.version").toString()

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(javaVersion))
  }
}

dependencies {
  compileOnly("org.purpurmc.purpur:purpur-api:${minecraftVersion}-R0.1-SNAPSHOT")

  implementation(
    project(":core:tool")
  )
}
