/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.tool.database

import javax.sql.DataSource

/**
 * Ensures all mining tool, fragment, ability and enchantment tables.
 */
object DatabaseToolSchema {

  fun ensureSchema(dataSource: DataSource) {
    dataSource.connection.use { connection ->
      connection.createStatement().use { statement ->
        statement.execute(
          """
            CREATE TABLE IF NOT EXISTS mining_tool_profile_data (
              tool_identifier VARCHAR(36) PRIMARY KEY,
              owner_identifier VARCHAR(36),
              pickaxe_level INTEGER NOT NULL DEFAULT 1,
              pickaxe_experience BIGINT NOT NULL DEFAULT 0,
              prestige INTEGER NOT NULL DEFAULT 0,
              enchantment_points INTEGER NOT NULL DEFAULT 0,
              total_enchantment_points_earned INTEGER NOT NULL DEFAULT 0,
              pickaxe_experience_multiplier DOUBLE NOT NULL DEFAULT 1.0,
              fragment_chance_multiplier DOUBLE NOT NULL DEFAULT 1.0,
              selected_ability_identifier VARCHAR(64),
              created_at BIGINT NOT NULL,
              updated_at BIGINT NOT NULL
            );
          """.trimIndent()
        )
        statement.execute(
          """
            CREATE TABLE IF NOT EXISTS mining_tool_enchantment_state_data (
              tool_identifier VARCHAR(36) NOT NULL,
              enchantment_identifier VARCHAR(64) NOT NULL,
              unlocked BOOLEAN NOT NULL DEFAULT FALSE,
              level INTEGER NOT NULL DEFAULT 0,
              invested_enchantment_points INTEGER NOT NULL DEFAULT 0,
              invested_fragments TEXT,
              updated_at BIGINT NOT NULL,
              PRIMARY KEY (tool_identifier, enchantment_identifier)
            );
          """.trimIndent()
        )
        statement.execute(
          """
            CREATE TABLE IF NOT EXISTS mining_tool_fragment_state_data (
              tool_identifier VARCHAR(36) NOT NULL,
              rarity VARCHAR(32) NOT NULL,
              amount INTEGER NOT NULL DEFAULT 0,
              failed_eligible_rolls BIGINT NOT NULL DEFAULT 0,
              updated_at BIGINT NOT NULL,
              PRIMARY KEY (tool_identifier, rarity)
            );
          """.trimIndent()
        )
        statement.execute(
          """
            CREATE TABLE IF NOT EXISTS mining_tool_ability_state_data (
              tool_identifier VARCHAR(36) NOT NULL,
              ability_identifier VARCHAR(64) NOT NULL,
              unlocked BOOLEAN NOT NULL DEFAULT FALSE,
              level INTEGER NOT NULL DEFAULT 0,
              cooldown_ends_at BIGINT,
              updated_at BIGINT NOT NULL,
              PRIMARY KEY (tool_identifier, ability_identifier)
            );
          """.trimIndent()
        )
        statement.execute(
          """
            CREATE TABLE IF NOT EXISTS mining_tool_statistic_data (
              tool_identifier VARCHAR(36) PRIMARY KEY,
              total_blocks_mined BIGINT NOT NULL DEFAULT 0,
              total_ores_mined BIGINT NOT NULL DEFAULT 0,
              blocks_by_category TEXT,
              ores_by_category TEXT,
              fragments_found TEXT,
              updated_at BIGINT NOT NULL
            );
          """.trimIndent()
        )
        statement.execute(
          """
            CREATE TABLE IF NOT EXISTS mining_tool_prestige_history_data (
              identifier VARCHAR(36) PRIMARY KEY,
              tool_identifier VARCHAR(36) NOT NULL,
              previous_prestige INTEGER NOT NULL,
              new_prestige INTEGER NOT NULL,
              prestiged_at BIGINT NOT NULL
            );
          """.trimIndent()
        )
        statement.execute("CREATE INDEX IF NOT EXISTS index_mining_tool_profile_owner ON mining_tool_profile_data(owner_identifier);")
        statement.execute("CREATE INDEX IF NOT EXISTS index_mining_tool_prestige_history_tool ON mining_tool_prestige_history_data(tool_identifier);")
      }
    }
  }
}
