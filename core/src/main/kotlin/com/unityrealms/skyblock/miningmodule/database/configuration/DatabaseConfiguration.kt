/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.database.configuration

import org.bukkit.configuration.file.FileConfiguration

/** Represents database connection configuration. */
data class DatabaseConfiguration(
  val type: String,
  val host: String?,
  val port: Int?,
  val name: String?,
  val user: String?,
  val password: String?
) {

  /** Loads database configuration from YAML. */
  object Loader {

    fun fromYaml(fileConfiguration: FileConfiguration): DatabaseConfiguration {
      val configurationSection = fileConfiguration.getConfigurationSection("database")

      return DatabaseConfiguration(
        type = configurationSection?.getString("type") ?: "sqlite",
        host = configurationSection?.getString("host"),
        port = configurationSection?.getInt("port"),
        name = configurationSection?.getString("name"),
        user = configurationSection?.getString("user"),
        password = configurationSection?.getString("password")
      )
    }
  }
}
