/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.mining.profile.repository.implementation

import com.unityrealms.skyblock.miningmodule.mining.profile.MiningProfile
import com.unityrealms.skyblock.miningmodule.mining.profile.repository.MiningProfileRepository

import java.sql.ResultSet
import java.util.UUID
import javax.sql.DataSource

/**
 * Represents a JDBC implementation of the mining profile repository.
 *
 * @param dataSource The data source used for database operations.
 */
class JdbcMiningProfileRepository(private val dataSource: DataSource) : MiningProfileRepository {

  override fun save(miningProfile: MiningProfile) {
    this.dataSource.connection.use { connection ->
      connection.prepareStatement(
        """
          UPDATE mining_profile_data
          SET level = ?, experience = ?, prestige = ?, total_blocks_mined = ?,
              total_experience_earned = ?, animation_enabled = ?, created_at = ?,
              updated_at = ?, last_mined_at = ?
          WHERE identifier = ?
        """.trimIndent()
      ).use { preparedStatement ->
        preparedStatement.setInt(1, miningProfile.level)
        preparedStatement.setLong(2, miningProfile.experience)
        preparedStatement.setInt(3, miningProfile.prestige)
        preparedStatement.setLong(4, miningProfile.totalBlocksMined)
        preparedStatement.setLong(5, miningProfile.totalExperienceEarned)
        preparedStatement.setBoolean(6, miningProfile.animationEnabled)
        preparedStatement.setLong(7, miningProfile.createdAt)
        preparedStatement.setLong(8, miningProfile.updatedAt)

        if (miningProfile.lastMinedAt == null) {
          preparedStatement.setNull(9, java.sql.Types.BIGINT)
        } else {
          preparedStatement.setLong(9, miningProfile.lastMinedAt)
        }

        preparedStatement.setString(10, miningProfile.identifier.toString())

        if (preparedStatement.executeUpdate() == 0) {
          connection.prepareStatement(
            """
              INSERT INTO mining_profile_data(
                identifier, level, experience, prestige, total_blocks_mined,
                total_experience_earned, animation_enabled, created_at,
                updated_at, last_mined_at
              ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
          ).use { insertStatement ->
            insertStatement.setString(1, miningProfile.identifier.toString())
            insertStatement.setInt(2, miningProfile.level)
            insertStatement.setLong(3, miningProfile.experience)
            insertStatement.setInt(4, miningProfile.prestige)
            insertStatement.setLong(5, miningProfile.totalBlocksMined)
            insertStatement.setLong(6, miningProfile.totalExperienceEarned)
            insertStatement.setBoolean(7, miningProfile.animationEnabled)
            insertStatement.setLong(8, miningProfile.createdAt)
            insertStatement.setLong(9, miningProfile.updatedAt)

            if (miningProfile.lastMinedAt == null) {
              insertStatement.setNull(10, java.sql.Types.BIGINT)
            } else {
              insertStatement.setLong(10, miningProfile.lastMinedAt)
            }

            insertStatement.executeUpdate()
          }
        }
      }
    }
  }

  override fun load(identifier: UUID): MiningProfile? {
    this.dataSource.connection.use { connection ->
      connection.prepareStatement("SELECT * FROM mining_profile_data WHERE identifier = ?").use { preparedStatement ->
        preparedStatement.setString(1, identifier.toString())

        preparedStatement.executeQuery().use { resultSet ->
          return if (resultSet.next()) {
            this.mapProfile(resultSet)
          } else {
            null
          }
        }
      }
    }
  }

  override fun delete(identifier: UUID) {
    this.dataSource.connection.use { connection ->
      connection.prepareStatement("DELETE FROM mining_profile_data WHERE identifier = ?").use { preparedStatement ->
        preparedStatement.setString(1, identifier.toString())
        preparedStatement.executeUpdate()
      }
    }
  }

  override fun loadTopByPrestige(limit: Int): List<MiningProfile> {
    return this.loadTop(
      "SELECT * FROM mining_profile_data ORDER BY prestige DESC, level DESC, experience DESC, total_blocks_mined DESC LIMIT ?",
      limit
    )
  }

  override fun loadTopByBlocks(limit: Int): List<MiningProfile> {
    return this.loadTop(
      "SELECT * FROM mining_profile_data ORDER BY total_blocks_mined DESC, prestige DESC, level DESC LIMIT ?",
      limit
    )
  }

  private fun loadTop(query: String, limit: Int): List<MiningProfile> {
    require(limit in 1..100) {
      "The profile query limit must be between 1 and 100."
    }

    this.dataSource.connection.use { connection ->
      connection.prepareStatement(query).use { preparedStatement ->
        preparedStatement.setInt(1, limit)

        preparedStatement.executeQuery().use { resultSet ->
          val profileList = mutableListOf<MiningProfile>()

          while (resultSet.next()) {
            profileList.add(this.mapProfile(resultSet))
          }

          return profileList
        }
      }
    }
  }

  private fun mapProfile(resultSet: ResultSet): MiningProfile {
    val lastMinedAt = resultSet.getLong("last_mined_at")

    return MiningProfile(
      identifier = UUID.fromString(resultSet.getString("identifier")),
      level = resultSet.getInt("level"),
      experience = resultSet.getLong("experience"),
      prestige = resultSet.getInt("prestige"),
      totalBlocksMined = resultSet.getLong("total_blocks_mined"),
      totalExperienceEarned = resultSet.getLong("total_experience_earned"),
      animationEnabled = resultSet.getBoolean("animation_enabled"),
      createdAt = resultSet.getLong("created_at"),
      updatedAt = resultSet.getLong("updated_at"),
      lastMinedAt = if (resultSet.wasNull()) null else lastMinedAt
    )
  }
}
