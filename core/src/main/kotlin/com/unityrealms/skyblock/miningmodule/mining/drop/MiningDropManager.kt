/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.mining.drop

import com.unityrealms.skyblock.miningmodule.mining.block.MiningBlock

import java.util.concurrent.ThreadLocalRandom

import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/** Creates and delivers configured mining drops. */
class MiningDropManager {

  /** Creates drops for a mining block and player tool. */
  fun createDrops(player: Player, miningBlock: MiningBlock, additionalFortuneBonus: Int = 0): MutableList<ItemStack> {
    val random = ThreadLocalRandom.current()
    val fortuneLevel = player.inventory.itemInMainHand.getEnchantmentLevel(Enchantment.FORTUNE) + additionalFortuneBonus
    val dropList = mutableListOf<ItemStack>()

    for (drop in miningBlock.dropList) {
      if (random.nextDouble() > drop.chance) {
        continue
      }

      val baseAmount = random.nextInt(drop.minimumAmount, drop.maximumAmount + 1)
      val fortuneBonus = if (fortuneLevel <= 0) 0 else random.nextInt(0, fortuneLevel + 1)
      val amount = (baseAmount + fortuneBonus).coerceAtMost(drop.material.maxStackSize)

      dropList.add(ItemStack(drop.material, amount))
    }

    return dropList
  }

  /** Delivers drops to the inventory and drops overflow naturally. */
  fun deliver(player: Player, dropList: List<ItemStack>) {
    for (itemStack in dropList) {
      val overflowMap = player.inventory.addItem(itemStack)

      for (overflow in overflowMap.values) {
        player.world.dropItemNaturally(player.location, overflow)
      }
    }
  }
}
