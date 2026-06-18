/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.api

import com.unityrealms.skyblock.miningmodule.mining.block.MiningBlockRegistry
import com.unityrealms.skyblock.miningmodule.mine.MineRegistry
import com.unityrealms.skyblock.miningmodule.mining.profile.MiningProfileManager
import com.unityrealms.skyblock.miningmodule.tool.runtime.ToolRuntimeService

import java.util.UUID

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * Represents the implementation of the public mining module API.
 */
class MiningModuleApiImpl(
  private val miningProfileManager: MiningProfileManager,
  private val miningBlockRegistry: MiningBlockRegistry,
  private val mineRegistry: MineRegistry,
  private val toolRuntimeService: ToolRuntimeService
) : MiningModuleApi {

  override fun getMiningProfile(playerIdentifier: UUID): MiningProfileView? {
    val profile = this.miningProfileManager.get(playerIdentifier) ?: return null

    return MiningProfileView(
      identifier = profile.identifier,
      level = profile.level,
      experience = profile.experience,
      prestige = profile.prestige,
      totalBlocksMined = profile.totalBlocksMined,
      totalExperienceEarned = profile.totalExperienceEarned
    )
  }

  override fun addExperience(playerIdentifier: UUID, experience: Long) {
    this.miningProfileManager.addExperience(playerIdentifier, experience)
  }

  override fun canPrestige(playerIdentifier: UUID): Boolean = this.miningProfileManager.canPrestige(playerIdentifier)

  override fun isMiningBlock(material: Material): Boolean = this.miningBlockRegistry.get(material) != null

  override fun hasMine(location: Location): Boolean = this.mineRegistry.resolve(location) != null

  override fun getToolProfile(itemStack: ItemStack): ToolProfileView? {
    val profile = this.toolRuntimeService.resolveProfile(itemStack) ?: return null

    return ToolProfileView(
      toolIdentifier = profile.toolIdentifier,
      ownerIdentifier = profile.ownerIdentifier,
      pickaxeLevel = profile.level,
      pickaxeExperience = profile.experience,
      prestige = profile.prestige,
      enchantmentPoints = profile.enchantmentTokenCount,
      totalEnchantmentPointsEarned = profile.totalEarnedEnchantmentTokenCount,
      pickaxeExperienceMultiplier = profile.experienceMultiplier,
      fragmentChanceMultiplier = profile.fragmentChanceMultiplier,
      selectedAbilityIdentifier = profile.selectedAbilityIdentifier,
      fragmentBalanceMap = this.toolRuntimeService.getFragmentStateList(profile.toolIdentifier).associate { state ->
        state.rarity.name to state.amount
      }
    )
  }

  override fun createTool(player: Player): ItemStack = this.toolRuntimeService.createTool(player)
}
