/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.mining.statistic.repository.implementation

import com.unityrealms.skyblock.miningmodule.mining.statistic.MiningStatistic
import com.unityrealms.skyblock.miningmodule.mining.statistic.repository.MiningStatisticRepository

import java.util.UUID
import javax.sql.DataSource

/** JDBC implementation of the mining statistic repository. */
class JdbcMiningStatisticRepository(private val dataSource: DataSource) : MiningStatisticRepository {

  override fun save(miningStatistic: MiningStatistic) {
    this.dataSource.connection.use { connection ->
      connection.prepareStatement(
        """
          UPDATE mining_block_statistic_data
          SET blocks_mined = ?, experience_earned = ?, last_mined_at = ?
          WHERE player_identifier = ? AND block_identifier = ?
        """.trimIndent()
      ).use { preparedStatement ->
        preparedStatement.setLong(1, miningStatistic.blocksMined)
        preparedStatement.setLong(2, miningStatistic.experienceEarned)

        if (miningStatistic.lastMinedAt == null) {
          preparedStatement.setNull(3, java.sql.Types.BIGINT)
        } else {
          preparedStatement.setLong(3, miningStatistic.lastMinedAt)
        }

        preparedStatement.setString(4, miningStatistic.playerIdentifier.toString())
        preparedStatement.setString(5, miningStatistic.blockIdentifier)

        if (preparedStatement.executeUpdate() == 0) {
          connection.prepareStatement(
            """
              INSERT INTO mining_block_statistic_data(
                player_identifier, block_identifier, blocks_mined,
                experience_earned, last_mined_at
              ) VALUES (?, ?, ?, ?, ?)
            """.trimIndent()
          ).use { insertStatement ->
            insertStatement.setString(1, miningStatistic.playerIdentifier.toString())
            insertStatement.setString(2, miningStatistic.blockIdentifier)
            insertStatement.setLong(3, miningStatistic.blocksMined)
            insertStatement.setLong(4, miningStatistic.experienceEarned)

            if (miningStatistic.lastMinedAt == null) {
              insertStatement.setNull(5, java.sql.Types.BIGINT)
            } else {
              insertStatement.setLong(5, miningStatistic.lastMinedAt)
            }

            insertStatement.executeUpdate()
          }
        }
      }
    }
  }

  override fun load(playerIdentifier: UUID, blockIdentifier: String): MiningStatistic? {
    this.dataSource.connection.use { connection ->
      connection.prepareStatement(
        "SELECT * FROM mining_block_statistic_data WHERE player_identifier = ? AND block_identifier = ?"
      ).use { preparedStatement ->
        preparedStatement.setString(1, playerIdentifier.toString())
        preparedStatement.setString(2, blockIdentifier)

        preparedStatement.executeQuery().use { resultSet ->
          if (!(resultSet.next())) {
            return null
          }

          val lastMinedAt = resultSet.getLong("last_mined_at")

          return MiningStatistic(
            playerIdentifier = playerIdentifier,
            blockIdentifier = blockIdentifier,
            blocksMined = resultSet.getLong("blocks_mined"),
            experienceEarned = resultSet.getLong("experience_earned"),
            lastMinedAt = if (resultSet.wasNull()) null else lastMinedAt
          )
        }
      }
    }
  }

  override fun loadAll(playerIdentifier: UUID): List<MiningStatistic> {
    this.dataSource.connection.use { connection ->
      connection.prepareStatement(
        "SELECT * FROM mining_block_statistic_data WHERE player_identifier = ? ORDER BY blocks_mined DESC"
      ).use { preparedStatement ->
        preparedStatement.setString(1, playerIdentifier.toString())

        preparedStatement.executeQuery().use { resultSet ->
          val statisticList = mutableListOf<MiningStatistic>()

          while (resultSet.next()) {
            val lastMinedAt = resultSet.getLong("last_mined_at")

            statisticList.add(
              MiningStatistic(
                playerIdentifier = playerIdentifier,
                blockIdentifier = resultSet.getString("block_identifier"),
                blocksMined = resultSet.getLong("blocks_mined"),
                experienceEarned = resultSet.getLong("experience_earned"),
                lastMinedAt = if (resultSet.wasNull()) null else lastMinedAt
              )
            )
          }

          return statisticList
        }
      }
    }
  }

  override fun deleteAll(playerIdentifier: UUID) {
    this.dataSource.connection.use { connection ->
      connection.prepareStatement("DELETE FROM mining_block_statistic_data WHERE player_identifier = ?").use { preparedStatement ->
        preparedStatement.setString(1, playerIdentifier.toString())
        preparedStatement.executeUpdate()
      }
    }
  }
}
