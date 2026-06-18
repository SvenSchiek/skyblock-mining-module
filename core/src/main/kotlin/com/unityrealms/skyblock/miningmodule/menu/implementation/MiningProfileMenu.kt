/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.menu.implementation

import com.unityrealms.server.messageframework.message.component.ComponentTransformer
import com.unityrealms.skyblock.miningmodule.mining.MiningCore
import com.unityrealms.skyblock.miningmodule.menu.Menu
import com.unityrealms.skyblock.miningmodule.menu.MenuItemFactory

import java.util.UUID
import java.util.function.Consumer

import net.kyori.adventure.text.Component

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta

/** Displays the complete mining profile overview. */
class MiningProfileMenu(private val profileIdentifier: UUID) : Menu(
  Inventory(ComponentTransformer.transform("#45B8FFMining Profile"), 45)
) {

  companion object {
    private const val ANIMATION_SLOT = 20
    private const val STATISTIC_SLOT = 22
    private const val PRESTIGE_SLOT = 24
    private const val MINE_SLOT = 31
    private const val CLOSE_SLOT = 40
  }

  override fun onOpen(player: Player) {
    this.inventory.clear()
    val profile = MiningCore.miningProfileManager.getOrCreate(this.profileIdentifier)
    val requiredExperience = MiningCore.miningLevelManager.requiredExperience(profile.level)
    val progress = MiningCore.miningLevelManager.progress(profile.level, profile.experience)
    val offlinePlayer = Bukkit.getOfflinePlayer(this.profileIdentifier)
    val profileItemStack = ItemStack(Material.PLAYER_HEAD)

    profileItemStack.editMeta(SkullMeta::class.java, Consumer { skullMeta ->
      skullMeta.owningPlayer = offlinePlayer
      skullMeta.displayName(ComponentTransformer.transform("#56D18F${offlinePlayer.name ?: "Mining Profile"}"))
      skullMeta.lore(
        listOf(
          ComponentTransformer.transform("#8f9baaMining Level: #ffffff${profile.level}"),
          ComponentTransformer.transform("#8f9baaPrestige: #ffffff${profile.prestige}"),
          ComponentTransformer.transform("#8f9baaExperience: #ffffff${profile.experience}/${if (requiredExperience == 0L) "MAX" else requiredExperience}"),
          Component.empty(),
          ComponentTransformer.transform("#8f9baaTotal Blocks: #ffffff${profile.totalBlocksMined}"),
          ComponentTransformer.transform("#8f9baaTotal XP: #ffffff${profile.totalExperienceEarned}")
        )
      )
    })
    this.inventory.setContentArray(profileItemStack, 13)

    val completedBars = (progress * 7.0).toInt().coerceIn(0, 7)

    for (index in 0 until 7) {
      val material = if (index < completedBars) Material.LIME_STAINED_GLASS_PANE else Material.GRAY_STAINED_GLASS_PANE
      this.inventory.setContentArray(
        MenuItemFactory.create(material, if (index < completedBars) "#56D18FProgress" else "#8f9baaProgress", listOf("#ffffff${(progress * 100.0).toInt()}%")),
        28 + index
      )
    }

    this.inventory.setContentArray(
      MenuItemFactory.create(
        if (profile.animationEnabled) Material.LIME_DYE else Material.GRAY_DYE,
        "#45B8FFMining Animations",
        listOf(
          "#8f9baaStatus: ${if (profile.animationEnabled) "#56D18FEnabled" else "#F12E44Disabled"}",
          "",
          "#ffffffClick to toggle."
        )
      ),
      ANIMATION_SLOT
    )
    this.inventory.setContentArray(
      MenuItemFactory.create(Material.WRITABLE_BOOK, "#45B8FFMining Statistics", listOf("#ffffffView mined blocks and earned experience.")),
      STATISTIC_SLOT
    )
    this.inventory.setContentArray(
      MenuItemFactory.create(
        Material.NETHER_STAR,
        "#c778ffMining Prestige",
        listOf(
          "#8f9baaCurrent Prestige: #ffffff${profile.prestige}",
          "#8f9baaRequired Level: #ffffff${MiningCore.miningLevelManager.maximumLevel}",
          "",
          "#ffffffClick to inspect prestige."
        )
      ),
      PRESTIGE_SLOT
    )
    val mine = MiningCore.mineRegistry.resolve(player.location)
    this.inventory.setContentArray(
      MenuItemFactory.create(
        Material.COMPASS,
        "#45B8FFCurrent Mine",
        listOf(
          "#8f9baaMine: #ffffff${mine?.displayName ?: "None"}",
          "#8f9baaXP Multiplier: #ffffff${mine?.experienceMultiplier ?: 1.0}x",
          "#8f9baaDamage Multiplier: #ffffff${mine?.damageMultiplier ?: 1.0}x"
        )
      ),
      MINE_SLOT
    )
    this.inventory.setContentArray(MenuItemFactory.create(Material.BARRIER, "#F12E44Close"), CLOSE_SLOT)
    player.playSound(player.location, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.65F, 1.3F)
  }

  override fun onClose(player: Player) {}

  override fun onClick(clickType: ClickType, slot: Int, player: Player) {
    when (slot) {
      ANIMATION_SLOT -> {
        val profile = MiningCore.miningProfileManager.getOrCreate(this.profileIdentifier)
        MiningCore.miningProfileManager.setAnimationEnabled(this.profileIdentifier, !(profile.animationEnabled))
        player.playSound(player.location, Sound.UI_BUTTON_CLICK, 0.7F, 1.2F)
        this.onOpen(player)
      }

      STATISTIC_SLOT -> MiningStatisticsMenu(this.profileIdentifier).open(player)
      PRESTIGE_SLOT -> MiningPrestigeMenu(this.profileIdentifier).open(player)
      CLOSE_SLOT -> player.closeInventory()
    }
  }
}
