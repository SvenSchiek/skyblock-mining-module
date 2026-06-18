/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.mining.prestige.repository.implementation

import com.unityrealms.skyblock.miningmodule.mining.prestige.MiningPrestigeHistory
import com.unityrealms.skyblock.miningmodule.mining.prestige.repository.MiningPrestigeHistoryRepository

import java.util.UUID
import javax.sql.DataSource

/** JDBC implementation of mining prestige history persistence. */
class JdbcMiningPrestigeHistoryRepository(private val dataSource: DataSource) : MiningPrestigeHistoryRepository {

  override fun save(history: MiningPrestigeHistory) {
    this.dataSource.connection.use { connection ->
      connection.prepareStatement(
        """
          INSERT INTO mining_prestige_history_data(
            identifier, player_identifier, previous_prestige, new_prestige, timestamp
          ) VALUES (?, ?, ?, ?, ?)
        """.trimIndent()
      ).use { preparedStatement ->
        preparedStatement.setString(1, history.identifier.toString())
        preparedStatement.setString(2, history.playerIdentifier.toString())
        preparedStatement.setInt(3, history.previousPrestige)
        preparedStatement.setInt(4, history.newPrestige)
        preparedStatement.setLong(5, history.timestamp)
        preparedStatement.executeUpdate()
      }
    }
  }

  override fun loadLatest(playerIdentifier: UUID, limit: Int): List<MiningPrestigeHistory> {
    require(limit in 1..100) {
      "The prestige history limit must be between 1 and 100."
    }

    this.dataSource.connection.use { connection ->
      connection.prepareStatement(
        "SELECT * FROM mining_prestige_history_data WHERE player_identifier = ? ORDER BY timestamp DESC LIMIT ?"
      ).use { preparedStatement ->
        preparedStatement.setString(1, playerIdentifier.toString())
        preparedStatement.setInt(2, limit)

        preparedStatement.executeQuery().use { resultSet ->
          val historyList = mutableListOf<MiningPrestigeHistory>()

          while (resultSet.next()) {
            historyList.add(
              MiningPrestigeHistory(
                identifier = UUID.fromString(resultSet.getString("identifier")),
                playerIdentifier = playerIdentifier,
                previousPrestige = resultSet.getInt("previous_prestige"),
                newPrestige = resultSet.getInt("new_prestige"),
                timestamp = resultSet.getLong("timestamp")
              )
            )
          }

          return historyList
        }
      }
    }
  }
}
