plugins {
  `java-library`

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
}
