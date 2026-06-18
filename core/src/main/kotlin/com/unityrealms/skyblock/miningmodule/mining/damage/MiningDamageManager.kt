/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.mining.damage

import com.unityrealms.skyblock.miningmodule.mine.Mine
import com.unityrealms.skyblock.miningmodule.mining.profile.MiningProfile
import com.unityrealms.skyblock.miningmodule.mining.profile.MiningProfileManager

import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/** Calculates server-controlled mining damage. */
class MiningDamageManager(private val miningProfileManager: MiningProfileManager) {

  /** Calculates mining damage for the current tick. */
  fun calculate(player: Player, profile: MiningProfile, mine: Mine?): Double {
    val tool = player.inventory.itemInMainHand
    val baseDamage = this.toolPower(tool)
    val efficiencyLevel = tool.getEnchantmentLevel(Enchantment.EFFICIENCY)
    val efficiencyMultiplier = 1.0 + efficiencyLevel * 0.18
    val levelMultiplier = 1.0 + (profile.level - 1) * 0.005
    val prestigeMultiplier = this.miningProfileManager.damageMultiplier(profile)
    val mineMultiplier = mine?.damageMultiplier ?: 1.0

    return baseDamage * efficiencyMultiplier * levelMultiplier * prestigeMultiplier * mineMultiplier
  }

  private fun toolPower(itemStack: ItemStack): Double {
    val materialName = itemStack.type.name

    return when {
      materialName.startsWith("GOLDEN_") -> 4.2
      materialName.startsWith("NETHERITE_") -> 4.0
      materialName.startsWith("DIAMOND_") -> 3.4
      materialName.startsWith("IRON_") -> 2.8
      materialName.startsWith("STONE_") -> 2.2
      materialName.startsWith("WOODEN_") -> 1.5
      else -> 0.8
    }
  }
}
