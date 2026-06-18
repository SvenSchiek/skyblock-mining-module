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
import com.unityrealms.skyblock.miningmodule.tool.ToolAbilityState
import com.unityrealms.skyblock.miningmodule.tool.mining.statistic.ToolMiningStatistic
import com.unityrealms.skyblock.miningmodule.tool.ToolPrestigeConfiguration
import com.unityrealms.skyblock.miningmodule.tool.profile.ToolProfile
import com.unityrealms.skyblock.miningmodule.tool.repository.ToolTransactionRepository

import java.sql.Connection
import java.util.UUID
import javax.sql.DataSource

/**
 * Represents a JDBC implementation of important atomic tool transactions.
 */
class JdbcToolTransactionRepository(private val dataSource: DataSource) : ToolTransactionRepository {

  override fun upgradeEnchantment(request: ToolTransactionRepository.EnchantmentUpgradeRequest): ToolTransactionRepository.EnchantmentUpgradeResult {
    return this.inTransaction { connection ->
      val profile = this.loadProfile(connection, request.toolIdentifier)
        ?: throw IllegalArgumentException("The tool profile '${request.toolIdentifier}' was not found.")

      if (profile.enchantmentTokenCount < request.enchantmentPointCost) {
        throw IllegalArgumentException("The tool does not have enough enchantment points.")
      }

      connection.prepareStatement(
        "SELECT unlocked, level FROM mining_tool_enchantment_state_data WHERE tool_identifier = ? AND enchantment_identifier = ?"
      ).use { statement ->
        statement.setString(1, request.toolIdentifier.toString())
        statement.setString(2, request.enchantmentIdentifier)

        statement.executeQuery().use { resultSet ->
          if (!(resultSet.next()) || !(resultSet.getBoolean("unlocked"))) {
            throw IllegalArgumentException("The enchantment '${request.enchantmentIdentifier}' is not unlocked.")
          }

          if (resultSet.getInt("level") != request.expectedLevel) {
            throw IllegalStateException("The enchantment level changed while the upgrade was processed.")
          }
        }
      }

      val updatedFragmentStateList = mutableListOf<FragmentState>()

      for ((rarity, cost) in request.fragmentCostMap) {
        val state = this.loadFragmentState(connection, request.toolIdentifier, rarity)
          ?: FragmentState(request.toolIdentifier, rarity)

        if (state.amount < cost) {
          throw IllegalArgumentException("The tool does not have enough ${rarity.name.lowercase()} fragments.")
        }

        updatedFragmentStateList.add(
          state.copy(
            amount = state.amount - cost,
            updatedAt = request.updatedAt
          )
        )
      }

      val updatedProfile = profile.copy(
        enchantmentTokenCount = profile.enchantmentTokenCount - request.enchantmentPointCost,
        updatedAt = request.updatedAt
      )
      this.updateProfile(connection, updatedProfile)
      updatedFragmentStateList.forEach { this.upsertFragmentState(connection, it) }

      connection.prepareStatement(
        """
          UPDATE mining_tool_enchantment_state_data
          SET level = ?, invested_enchantment_points = ?, invested_fragments = ?, updated_at = ?
          WHERE tool_identifier = ? AND enchantment_identifier = ?
        """.trimIndent()
      ).use { statement ->
        statement.setInt(1, request.nextLevel)
        statement.setInt(2, request.investedEnchantmentPoints)
        statement.setString(3, ToolMapCodec.encodeRarityIntMap(request.investedFragmentMap))
        statement.setLong(4, request.updatedAt)
        statement.setString(5, request.toolIdentifier.toString())
        statement.setString(6, request.enchantmentIdentifier)

        if (statement.executeUpdate() != 1) {
          throw IllegalStateException("The enchantment upgrade could not be persisted.")
        }
      }

      ToolTransactionRepository.EnchantmentUpgradeResult(updatedProfile, updatedFragmentStateList)
    }
  }

  override fun upgradeAbility(request: ToolTransactionRepository.AbilityUpgradeRequest): ToolTransactionRepository.AbilityUpgradeResult {
    return this.inTransaction { connection ->
      val profile = this.loadProfile(connection, request.toolIdentifier)
        ?: throw IllegalArgumentException("The tool profile '${request.toolIdentifier}' was not found.")

      if (profile.enchantmentTokenCount < request.enchantmentPointCost) {
        throw IllegalArgumentException("The tool does not have enough enchantment points.")
      }

      val state = this.loadAbilityState(connection, request.toolIdentifier, request.abilityIdentifier)
        ?: throw IllegalArgumentException("The ability '${request.abilityIdentifier}' is not unlocked.")

      if (!(state.unlocked)) {
        throw IllegalArgumentException("The ability '${request.abilityIdentifier}' is not unlocked.")
      }

      if (state.level != request.expectedLevel) {
        throw IllegalStateException("The ability level changed while the upgrade was processed.")
      }

      val updatedFragmentStateList = mutableListOf<FragmentState>()

      for ((rarity, cost) in request.fragmentCostMap) {
        val fragmentState = this.loadFragmentState(connection, request.toolIdentifier, rarity)
          ?: FragmentState(request.toolIdentifier, rarity)

        if (fragmentState.amount < cost) {
          throw IllegalArgumentException("The tool does not have enough ${rarity.name.lowercase()} fragments.")
        }

        updatedFragmentStateList.add(
          fragmentState.copy(
            amount = fragmentState.amount - cost,
            updatedAt = request.updatedAt
          )
        )
      }

      val updatedProfile = profile.copy(
        enchantmentTokenCount = profile.enchantmentTokenCount - request.enchantmentPointCost,
        updatedAt = request.updatedAt
      )
      val updatedState = state.copy(
        level = request.nextLevel,
        updatedAt = request.updatedAt
      )

      this.updateProfile(connection, updatedProfile)
      updatedFragmentStateList.forEach { this.upsertFragmentState(connection, it) }

      connection.prepareStatement(
        "UPDATE mining_tool_ability_state_data SET level = ?, updated_at = ? WHERE tool_identifier = ? AND ability_identifier = ?"
      ).use { statement ->
        statement.setInt(1, request.nextLevel)
        statement.setLong(2, request.updatedAt)
        statement.setString(3, request.toolIdentifier.toString())
        statement.setString(4, request.abilityIdentifier.lowercase())

        if (statement.executeUpdate() != 1) {
          throw IllegalStateException("The ability upgrade could not be persisted.")
        }
      }

      ToolTransactionRepository.AbilityUpgradeResult(updatedProfile, updatedState, updatedFragmentStateList)
    }
  }

  override fun prestige(
    toolProfile: ToolProfile,
    statistics: ToolMiningStatistic,
    maximumPickaxeLevel: Int,
    maximumPrestige: Int,
    requirement: ToolPrestigeConfiguration.Requirement,
    reward: ToolPrestigeConfiguration.Reward
  ): ToolProfile {
    return this.inTransaction { connection ->
      val currentProfile = this.loadProfile(connection, toolProfile.toolIdentifier)
        ?: throw IllegalArgumentException("The tool profile '${toolProfile.toolIdentifier}' was not found.")

      if (currentProfile.prestige >= maximumPrestige) {
        throw IllegalStateException("The maximum tool prestige has already been reached.")
      }

      if (requirement.maximumPickaxeLevelRequired && currentProfile.level < maximumPickaxeLevel) {
        throw IllegalStateException("The maximum pickaxe level must be reached before prestiging.")
      }

      if (statistics.totalMinedBlockCount < requirement.totalBlocksMined) {
        throw IllegalStateException("The required total block count has not been reached.")
      }

      if (statistics.totalMinedOreCount < requirement.totalOresMined) {
        throw IllegalStateException("The required total ore count has not been reached.")
      }

      val now = System.currentTimeMillis()
      val updatedProfile = currentProfile.copy(
        level = 1,
        experience = 0L,
        prestige = currentProfile.prestige + 1,
        enchantmentTokenCount = currentProfile.enchantmentTokenCount + reward.enchantmentPoints,
        totalEarnedEnchantmentTokenCount = currentProfile.totalEarnedEnchantmentTokenCount + reward.enchantmentPoints,
        experienceMultiplier = currentProfile.experienceMultiplier + reward.pickaxeExperienceMultiplier,
        fragmentChanceMultiplier = currentProfile.fragmentChanceMultiplier + reward.fragmentChanceMultiplier,
        updatedAt = now
      )
      this.updateProfile(connection, updatedProfile)

      connection.prepareStatement(
        "INSERT INTO mining_tool_prestige_history_data(identifier, tool_identifier, previous_prestige, new_prestige, prestiged_at) VALUES (?, ?, ?, ?, ?)"
      ).use { statement ->
        statement.setString(1, UUID.randomUUID().toString())
        statement.setString(2, currentProfile.toolIdentifier.toString())
        statement.setInt(3, currentProfile.prestige)
        statement.setInt(4, updatedProfile.prestige)
        statement.setLong(5, now)
        statement.executeUpdate()
      }

      updatedProfile
    }
  }

  override fun selectAbility(toolIdentifier: UUID, abilityIdentifier: String): ToolProfile {
    return this.inTransaction { connection ->
      connection.prepareStatement(
        "SELECT unlocked FROM mining_tool_ability_state_data WHERE tool_identifier = ? AND ability_identifier = ?"
      ).use { statement ->
        statement.setString(1, toolIdentifier.toString())
        statement.setString(2, abilityIdentifier.lowercase())

        statement.executeQuery().use { resultSet ->
          if (!(resultSet.next()) || !(resultSet.getBoolean("unlocked"))) {
            throw IllegalArgumentException("The ability '$abilityIdentifier' is not unlocked.")
          }
        }
      }

      val profile = this.loadProfile(connection, toolIdentifier)
        ?: throw IllegalArgumentException("The tool profile '$toolIdentifier' was not found.")
      val updatedProfile = profile.copy(
        selectedAbilityIdentifier = abilityIdentifier.lowercase(),
        updatedAt = System.currentTimeMillis()
      )
      this.updateProfile(connection, updatedProfile)
      updatedProfile
    }
  }

  override fun activateAbility(toolIdentifier: UUID, abilityIdentifier: String, cooldownEndsAt: Long): ToolAbilityState {
    return this.inTransaction { connection ->
      val state = this.loadAbilityState(connection, toolIdentifier, abilityIdentifier)
        ?: throw IllegalArgumentException("The ability '$abilityIdentifier' is not unlocked.")

      if (!(state.unlocked)) {
        throw IllegalArgumentException("The ability '$abilityIdentifier' is not unlocked.")
      }

      val now = System.currentTimeMillis()

      if (state.cooldownEndsAt != null && state.cooldownEndsAt > now) {
        throw IllegalStateException("The ability is still on cooldown for ${state.cooldownEndsAt - now} milliseconds.")
      }

      val updatedState = state.copy(
        cooldownEndsAt = cooldownEndsAt,
        updatedAt = now
      )

      connection.prepareStatement(
        "UPDATE mining_tool_ability_state_data SET cooldown_ends_at = ?, updated_at = ? WHERE tool_identifier = ? AND ability_identifier = ?"
      ).use { statement ->
        statement.setLong(1, cooldownEndsAt)
        statement.setLong(2, now)
        statement.setString(3, toolIdentifier.toString())
        statement.setString(4, abilityIdentifier.lowercase())

        if (statement.executeUpdate() != 1) {
          throw IllegalStateException("The ability cooldown could not be persisted.")
        }
      }

      updatedState
    }
  }

  private fun loadProfile(connection: Connection, toolIdentifier: UUID): ToolProfile? {
    connection.prepareStatement("SELECT * FROM mining_tool_profile_data WHERE tool_identifier = ?").use { statement ->
      statement.setString(1, toolIdentifier.toString())

      statement.executeQuery().use { resultSet ->
        if (!(resultSet.next())) {
          return null
        }

        return ToolProfile(
          toolIdentifier = toolIdentifier,
          ownerIdentifier = resultSet.getString("owner_identifier")?.let(UUID::fromString),
          experience = resultSet.getLong("pickaxe_experience"),
          level = resultSet.getInt("pickaxe_level"),
          prestige = resultSet.getInt("prestige"),
          enchantmentTokenCount = resultSet.getInt("enchantment_points"),
          totalEarnedEnchantmentTokenCount = resultSet.getInt("total_enchantment_points_earned"),
          experienceMultiplier = resultSet.getDouble("pickaxe_experience_multiplier"),
          fragmentChanceMultiplier = resultSet.getDouble("fragment_chance_multiplier"),
          selectedAbilityIdentifier = resultSet.getString("selected_ability_identifier"),
          createdAt = resultSet.getLong("created_at"),
          updatedAt = resultSet.getLong("updated_at")
        )
      }
    }
  }

  private fun updateProfile(connection: Connection, toolProfile: ToolProfile) {
    connection.prepareStatement(
      """
        UPDATE mining_tool_profile_data
        SET owner_identifier = ?, pickaxe_level = ?, pickaxe_experience = ?, prestige = ?, enchantment_points = ?,
            total_enchantment_points_earned = ?, pickaxe_experience_multiplier = ?, fragment_chance_multiplier = ?,
            selected_ability_identifier = ?, updated_at = ?
        WHERE tool_identifier = ?
      """.trimIndent()
    ).use { statement ->
      statement.setString(1, toolProfile.ownerIdentifier?.toString())
      statement.setInt(2, toolProfile.level)
      statement.setLong(3, toolProfile.experience)
      statement.setInt(4, toolProfile.prestige)
      statement.setInt(5, toolProfile.enchantmentTokenCount)
      statement.setInt(6, toolProfile.totalEarnedEnchantmentTokenCount)
      statement.setDouble(7, toolProfile.experienceMultiplier)
      statement.setDouble(8, toolProfile.fragmentChanceMultiplier)
      statement.setString(9, toolProfile.selectedAbilityIdentifier)
      statement.setLong(10, toolProfile.updatedAt)
      statement.setString(11, toolProfile.toolIdentifier.toString())

      if (statement.executeUpdate() != 1) {
        throw IllegalStateException("The tool profile transaction could not be persisted.")
      }
    }
  }

  private fun loadFragmentState(connection: Connection, toolIdentifier: UUID, rarity: FragmentRarity): FragmentState? {
    connection.prepareStatement(
      "SELECT amount, failed_eligible_rolls, updated_at FROM mining_tool_fragment_state_data WHERE tool_identifier = ? AND rarity = ?"
    ).use { statement ->
      statement.setString(1, toolIdentifier.toString())
      statement.setString(2, rarity.name)

      statement.executeQuery().use { resultSet ->
        if (!(resultSet.next())) {
          return null
        }

        return FragmentState(toolIdentifier, rarity, resultSet.getInt("amount"), resultSet.getLong("failed_eligible_rolls"), resultSet.getLong("updated_at"))
      }
    }
  }

  private fun upsertFragmentState(connection: Connection, state: FragmentState) {
    connection.prepareStatement(
      "UPDATE mining_tool_fragment_state_data SET amount = ?, failed_eligible_rolls = ?, updated_at = ? WHERE tool_identifier = ? AND rarity = ?"
    ).use { statement ->
      statement.setInt(1, state.amount)
      statement.setLong(2, state.failedEligibleRolls)
      statement.setLong(3, state.updatedAt)
      statement.setString(4, state.toolIdentifier.toString())
      statement.setString(5, state.rarity.name)

      if (statement.executeUpdate() == 0) {
        connection.prepareStatement(
          "INSERT INTO mining_tool_fragment_state_data(tool_identifier, rarity, amount, failed_eligible_rolls, updated_at) VALUES (?, ?, ?, ?, ?)"
        ).use { insertStatement ->
          insertStatement.setString(1, state.toolIdentifier.toString())
          insertStatement.setString(2, state.rarity.name)
          insertStatement.setInt(3, state.amount)
          insertStatement.setLong(4, state.failedEligibleRolls)
          insertStatement.setLong(5, state.updatedAt)
          insertStatement.executeUpdate()
        }
      }
    }
  }

  private fun loadAbilityState(connection: Connection, toolIdentifier: UUID, abilityIdentifier: String): ToolAbilityState? {
    connection.prepareStatement(
      "SELECT unlocked, level, cooldown_ends_at, updated_at FROM mining_tool_ability_state_data WHERE tool_identifier = ? AND ability_identifier = ?"
    ).use { statement ->
      statement.setString(1, toolIdentifier.toString())
      statement.setString(2, abilityIdentifier.lowercase())

      statement.executeQuery().use { resultSet ->
        if (!(resultSet.next())) {
          return null
        }

        val cooldownEndsAt = resultSet.getLong("cooldown_ends_at").let { if (resultSet.wasNull()) null else it }
        return ToolAbilityState(toolIdentifier, abilityIdentifier.lowercase(), resultSet.getBoolean("unlocked"), resultSet.getInt("level"), cooldownEndsAt, resultSet.getLong("updated_at"))
      }
    }
  }

  private fun <T> inTransaction(block: (Connection) -> T): T {
    this.dataSource.connection.use { connection ->
      val previousAutoCommit = connection.autoCommit
      connection.autoCommit = false

      try {
        val result = block(connection)
        connection.commit()
        return result
      } catch (exception: Exception) {
        connection.rollback()
        throw exception
      } finally {
        connection.autoCommit = previousAutoCommit
      }
    }
  }
}
