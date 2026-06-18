/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.database

import com.unityrealms.skyblock.miningmodule.database.configuration.DatabaseConfiguration
import com.unityrealms.skyblock.miningmodule.database.schema.DatabaseMiningSchema
import com.unityrealms.skyblock.miningmodule.tool.database.DatabaseToolSchema

import java.io.File
import java.io.PrintWriter
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.util.logging.Logger
import javax.sql.DataSource

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.Plugin

/** Represents a database connector that manages the connection to the database. */
object DatabaseConnector {

  /** Represents a reconnection policy for database connection attempts. */
  data class ReconnectPolicy(
    val maximumAttemptCount: Int = 3,
    val delay: Long = 500L
  ) {

    companion object {
      val DEFAULT = ReconnectPolicy()
    }

    init {
      require(this.maximumAttemptCount >= 1) {
        "The maximum attempt count must be at least 1."
      }

      require(this.delay >= 0L) {
        "The delay between attempts must not be negative."
      }
    }
  }

  private const val CONFIGURATION_PATH = "database/configuration.yml"

  private const val SQLITE_PATH = "database/mining_data.sqlite"

  private val reconnectPolicy = ReconnectPolicy.DEFAULT

  @Volatile
  var dataSource: DataSource? = null
    private set

  /** Initializes the database connection and schema. */
  fun initialize(plugin: Plugin): DataSource {
    val configurationFile = File(plugin.dataFolder, CONFIGURATION_PATH)

    if (!(configurationFile.exists())) {
      configurationFile.parentFile?.mkdirs()
      plugin.saveResource(CONFIGURATION_PATH, false)
      plugin.logger.info("Successfully created the default '$CONFIGURATION_PATH'.")
    }

    val databaseConfiguration = DatabaseConfiguration.Loader.fromYaml(
      YamlConfiguration.loadConfiguration(configurationFile)
    )
    val dataSource = if (databaseConfiguration.type.lowercase() == "sqlite") {
      plugin.logger.info("Using a configured SQLite database at '$SQLITE_PATH'...")
      this.createSqliteDataSource(plugin)
    } else {
      try {
        plugin.logger.info("Trying primary database (${databaseConfiguration.type}) connection...")
        val primaryDataSource = this.createJdbcDataSource(databaseConfiguration)

        primaryDataSource.connection.use { connection ->
          require(connection.isValid(5)) {
            "Connection validation failed."
          }
        }

        plugin.logger.info("Successfully connected to the primary database (${databaseConfiguration.type}).")
        primaryDataSource
      } catch (exception: Exception) {
        plugin.logger.warning("Failed to connect to the primary database (${databaseConfiguration.type}): ${exception.message}")
        plugin.logger.warning("Falling back to SQLite at '$SQLITE_PATH'.")
        this.createSqliteDataSource(plugin)
      }
    }

    this.dataSource = dataSource
    DatabaseMiningSchema.ensureSchema(dataSource)
    DatabaseToolSchema.ensureSchema(dataSource)
    return dataSource
  }

  /** Shuts down the database connector. */
  fun shutdown() {
    this.dataSource = null
  }

  private fun openConnection(jdbcUrl: String, user: String?, password: String?): Connection {
    var lastSqlException: SQLException? = null

    for (attempt in 1..this.reconnectPolicy.maximumAttemptCount) {
      try {
        return if (user == null && password == null) {
          DriverManager.getConnection(jdbcUrl)
        } else {
          DriverManager.getConnection(jdbcUrl, user, password)
        }
      } catch (sqlException: SQLException) {
        lastSqlException = sqlException
        val isRetryable = (sqlException.sqlState ?: continue).startsWith("08")

        if (attempt >= this.reconnectPolicy.maximumAttemptCount || !(isRetryable)) {
          throw sqlException
        }

        if (this.reconnectPolicy.delay > 0L) {
          Thread.sleep(this.reconnectPolicy.delay)
        }
      }
    }

    throw lastSqlException ?: SQLException("Failed to open the JDBC connection.")
  }

  private fun createRawDataSource(
    jdbcUrl: String,
    user: String? = null,
    password: String? = null
  ): DataSource = object : DataSource {

    override fun getConnection(): Connection = this@DatabaseConnector.openConnection(jdbcUrl, user, password)

    override fun getConnection(user: String?, password: String?): Connection =
      this@DatabaseConnector.openConnection(jdbcUrl, user, password)

    override fun <T> unwrap(interfaceClass: Class<T>?): T = throw UnsupportedOperationException()

    override fun isWrapperFor(interfaceClass: Class<*>?): Boolean = false

    override fun setLogWriter(printWriter: PrintWriter?) {}

    override fun getLogWriter(): PrintWriter? = null

    override fun setLoginTimeout(seconds: Int) {
      DriverManager.setLoginTimeout(seconds)
    }

    override fun getLoginTimeout(): Int = DriverManager.getLoginTimeout()

    override fun getParentLogger(): Logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME)
  }

  private fun createSqliteDataSource(plugin: Plugin): DataSource {
    val sqliteFile = File(plugin.dataFolder, SQLITE_PATH)

    if (!(sqliteFile.parentFile.exists())) {
      sqliteFile.parentFile.mkdirs()
    }

    if (!(sqliteFile.exists())) {
      sqliteFile.createNewFile()
    }

    try {
      Class.forName("org.sqlite.JDBC")
    } catch (classNotFoundException: ClassNotFoundException) {
      throw IllegalStateException("Failed to load SQLite JDBC driver.", classNotFoundException)
    }

    return this.createRawDataSource("jdbc:sqlite:${sqliteFile.absolutePath.replace('\\', '/')}")
  }

  private fun createJdbcDataSource(databaseConfiguration: DatabaseConfiguration): DataSource {
    val databaseType = databaseConfiguration.type.lowercase()

    if (databaseType !in listOf("mysql", "mariadb")) {
      throw IllegalArgumentException("Unsupported database type '${databaseConfiguration.type}'.")
    }

    try {
      when (databaseType) {
        "mysql" -> Class.forName("com.mysql.cj.jdbc.Driver")
        "mariadb" -> Class.forName("org.mariadb.jdbc.Driver")
      }
    } catch (classNotFoundException: ClassNotFoundException) {
      throw IllegalStateException("Failed to load JDBC driver for database type '$databaseType'.", classNotFoundException)
    }

    val host = databaseConfiguration.host ?: throw IllegalArgumentException("Database configuration requires a host.")
    val port = databaseConfiguration.port ?: throw IllegalArgumentException("Database configuration requires a port.")
    val name = databaseConfiguration.name ?: throw IllegalArgumentException("Database configuration requires a name.")
    val jdbcUrl = when (databaseType) {
      "mysql" -> "jdbc:mysql://$host:$port/$name"
      "mariadb" -> "jdbc:mariadb://$host:$port/$name"
      else -> throw IllegalArgumentException("Unsupported database type '${databaseConfiguration.type}' was provided.")
    }

    return this.createRawDataSource(
      jdbcUrl = jdbcUrl,
      user = databaseConfiguration.user,
      password = databaseConfiguration.password
    )
  }
}
