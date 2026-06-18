/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.tool.enchantment.repository.implementation

import com.unityrealms.skyblock.miningmodule.tool.enchantment.ToolEnchantmentState
import com.unityrealms.skyblock.miningmodule.tool.enchantment.repository.ToolEnchantmentStateRepository

import java.util.UUID
import javax.sql.DataSource

/**
 * Represents a JDBC implementation of the tool enchantment state repository.
 */
class JdbcToolEnchantmentStateRepository(private val dataSource: DataSource) : ToolEnchantmentStateRepository {

  override fun save(state: ToolEnchantmentState) {
    this.dataSource.connection.use { connection ->
      connection.prepareStatement(
        "UPDATE mining_tool_enchantment_state_data SET unlocked = ?, level = ?, invested_enchantment_points = ?, invested_fragments = ?, updated_at = ? WHERE tool_identifier = ? AND enchantment_identifier = ?"
      ).use { statement ->
        statement.setBoolean(1, state.unlocked)
        statement.setInt(2, state.level)
        statement.setInt(3, state.investedEnchantmentPoints)
        statement.setString(4, EnchantmentMapCodec.encode(state.investedFragments))
        statement.setLong(5, state.updatedAt)
        statement.setString(6, state.toolIdentifier.toString())
        statement.setString(7, state.enchantmentIdentifier.lowercase())

        if (statement.executeUpdate() == 0) {
          connection.prepareStatement(
            "INSERT INTO mining_tool_enchantment_state_data(tool_identifier, enchantment_identifier, unlocked, level, invested_enchantment_points, invested_fragments, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?)"
          ).use { insertStatement ->
            insertStatement.setString(1, state.toolIdentifier.toString())
            insertStatement.setString(2, state.enchantmentIdentifier.lowercase())
            insertStatement.setBoolean(3, state.unlocked)
            insertStatement.setInt(4, state.level)
            insertStatement.setInt(5, state.investedEnchantmentPoints)
            insertStatement.setString(6, EnchantmentMapCodec.encode(state.investedFragments))
            insertStatement.setLong(7, state.updatedAt)
            insertStatement.executeUpdate()
          }
        }
      }
    }
  }

  override fun load(toolIdentifier: UUID, enchantmentIdentifier: String): ToolEnchantmentState? {
    this.dataSource.connection.use { connection ->
      connection.prepareStatement(
        "SELECT unlocked, level, invested_enchantment_points, invested_fragments, updated_at FROM mining_tool_enchantment_state_data WHERE tool_identifier = ? AND enchantment_identifier = ?"
      ).use { statement ->
        statement.setString(1, toolIdentifier.toString())
        statement.setString(2, enchantmentIdentifier.lowercase())

        statement.executeQuery().use { resultSet ->
          if (!(resultSet.next())) return null
          return ToolEnchantmentState(
            toolIdentifier = toolIdentifier,
            enchantmentIdentifier = enchantmentIdentifier.lowercase(),
            unlocked = resultSet.getBoolean("unlocked"),
            level = resultSet.getInt("level"),
            investedEnchantmentPoints = resultSet.getInt("invested_enchantment_points"),
            investedFragments = EnchantmentMapCodec.decode(resultSet.getString("invested_fragments")),
            updatedAt = resultSet.getLong("updated_at")
          )
        }
      }
    }
  }

  override fun loadAll(toolIdentifier: UUID): List<ToolEnchantmentState> {
    this.dataSource.connection.use { connection ->
      connection.prepareStatement(
        "SELECT enchantment_identifier, unlocked, level, invested_enchantment_points, invested_fragments, updated_at FROM mining_tool_enchantment_state_data WHERE tool_identifier = ?"
      ).use { statement ->
        statement.setString(1, toolIdentifier.toString())

        statement.executeQuery().use { resultSet ->
          val stateList = mutableListOf<ToolEnchantmentState>()

          while (resultSet.next()) {
            stateList.add(
              ToolEnchantmentState(
                toolIdentifier = toolIdentifier,
                enchantmentIdentifier = resultSet.getString("enchantment_identifier"),
                unlocked = resultSet.getBoolean("unlocked"),
                level = resultSet.getInt("level"),
                investedEnchantmentPoints = resultSet.getInt("invested_enchantment_points"),
                investedFragments = EnchantmentMapCodec.decode(resultSet.getString("invested_fragments")),
                updatedAt = resultSet.getLong("updated_at")
              )
            )
          }

          return stateList
        }
      }
    }
  }

  override fun deleteAll(toolIdentifier: UUID) {
    this.dataSource.connection.use { connection ->
      connection.prepareStatement("DELETE FROM mining_tool_enchantment_state_data WHERE tool_identifier = ?").use { statement ->
        statement.setString(1, toolIdentifier.toString())
        statement.executeUpdate()
      }
    }
  }
}
