/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.database.schema

import javax.sql.DataSource

/** Ensures the mining database schema. */
object DatabaseMiningSchema {

  fun ensureSchema(dataSource: DataSource) {
    dataSource.connection.use { connection ->
      connection.createStatement().use { statement ->
        statement.execute(
          """
            CREATE TABLE IF NOT EXISTS mining_profile_data (
              identifier VARCHAR(36) PRIMARY KEY,
              level INTEGER NOT NULL DEFAULT 1,
              experience BIGINT NOT NULL DEFAULT 0,
              prestige INTEGER NOT NULL DEFAULT 0,
              total_blocks_mined BIGINT NOT NULL DEFAULT 0,
              total_experience_earned BIGINT NOT NULL DEFAULT 0,
              animation_enabled BOOLEAN NOT NULL DEFAULT TRUE,
              created_at BIGINT NOT NULL,
              updated_at BIGINT NOT NULL,
              last_mined_at BIGINT
            );
          """.trimIndent()
        )
        statement.execute(
          """
            CREATE TABLE IF NOT EXISTS mining_block_statistic_data (
              player_identifier VARCHAR(36) NOT NULL,
              block_identifier VARCHAR(64) NOT NULL,
              blocks_mined BIGINT NOT NULL DEFAULT 0,
              experience_earned BIGINT NOT NULL DEFAULT 0,
              last_mined_at BIGINT,
              PRIMARY KEY (player_identifier, block_identifier)
            );
          """.trimIndent()
        )
        statement.execute(
          """
            CREATE TABLE IF NOT EXISTS mining_prestige_history_data (
              identifier VARCHAR(36) PRIMARY KEY,
              player_identifier VARCHAR(36) NOT NULL,
              previous_prestige INTEGER NOT NULL,
              new_prestige INTEGER NOT NULL,
              timestamp BIGINT NOT NULL
            );
          """.trimIndent()
        )
      }
    }
  }
}
