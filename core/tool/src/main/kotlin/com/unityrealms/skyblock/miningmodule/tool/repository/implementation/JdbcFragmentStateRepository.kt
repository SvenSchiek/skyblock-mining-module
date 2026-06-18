/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.tool.repository.implementation

import com.unityrealms.skyblock.miningmodule.tool.FragmentRarity
import com.unityrealms.skyblock.miningmodule.tool.FragmentState
import com.unityrealms.skyblock.miningmodule.tool.repository.FragmentStateRepository

import java.util.UUID
import javax.sql.DataSource

/**
 * Represents a JDBC implementation of the fragment state repository.
 */
class JdbcFragmentStateRepository(private val dataSource: DataSource) : FragmentStateRepository {

  override fun save(fragmentState: FragmentState) {
    this.dataSource.connection.use { connection ->
      connection.prepareStatement(
        "UPDATE mining_tool_fragment_state_data SET amount = ?, failed_eligible_rolls = ?, updated_at = ? WHERE tool_identifier = ? AND rarity = ?"
      ).use { statement ->
        statement.setInt(1, fragmentState.amount)
        statement.setLong(2, fragmentState.failedEligibleRolls)
        statement.setLong(3, fragmentState.updatedAt)
        statement.setString(4, fragmentState.toolIdentifier.toString())
        statement.setString(5, fragmentState.rarity.name)

        if (statement.executeUpdate() == 0) {
          connection.prepareStatement(
            "INSERT INTO mining_tool_fragment_state_data(tool_identifier, rarity, amount, failed_eligible_rolls, updated_at) VALUES (?, ?, ?, ?, ?)"
          ).use { insertStatement ->
            insertStatement.setString(1, fragmentState.toolIdentifier.toString())
            insertStatement.setString(2, fragmentState.rarity.name)
            insertStatement.setInt(3, fragmentState.amount)
            insertStatement.setLong(4, fragmentState.failedEligibleRolls)
            insertStatement.setLong(5, fragmentState.updatedAt)
            insertStatement.executeUpdate()
          }
        }
      }
    }
  }

  override fun load(toolIdentifier: UUID, rarity: FragmentRarity): FragmentState? {
    this.dataSource.connection.use { connection ->
      connection.prepareStatement("SELECT amount, failed_eligible_rolls, updated_at FROM mining_tool_fragment_state_data WHERE tool_identifier = ? AND rarity = ?").use { statement ->
        statement.setString(1, toolIdentifier.toString())
        statement.setString(2, rarity.name)

        statement.executeQuery().use { resultSet ->
          if (!(resultSet.next())) {
            return null
          }

          return FragmentState(
            toolIdentifier = toolIdentifier,
            rarity = rarity,
            amount = resultSet.getInt("amount"),
            failedEligibleRolls = resultSet.getLong("failed_eligible_rolls"),
            updatedAt = resultSet.getLong("updated_at")
          )
        }
      }
    }
  }

  override fun loadAll(toolIdentifier: UUID): List<FragmentState> {
    this.dataSource.connection.use { connection ->
      connection.prepareStatement("SELECT rarity, amount, failed_eligible_rolls, updated_at FROM mining_tool_fragment_state_data WHERE tool_identifier = ?").use { statement ->
        statement.setString(1, toolIdentifier.toString())

        statement.executeQuery().use { resultSet ->
          val stateList = mutableListOf<FragmentState>()

          while (resultSet.next()) {
            val rarity = runCatching { FragmentRarity.valueOf(resultSet.getString("rarity")) }.getOrNull() ?: continue
            stateList.add(FragmentState(toolIdentifier, rarity, resultSet.getInt("amount"), resultSet.getLong("failed_eligible_rolls"), resultSet.getLong("updated_at")))
          }

          return stateList
        }
      }
    }
  }

  override fun deleteAll(toolIdentifier: UUID) {
    this.dataSource.connection.use { connection ->
      connection.prepareStatement("DELETE FROM mining_tool_fragment_state_data WHERE tool_identifier = ?").use { statement ->
        statement.setString(1, toolIdentifier.toString())
        statement.executeUpdate()
      }
    }
  }
}
