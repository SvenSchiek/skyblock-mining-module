/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.mining.animation

import com.unityrealms.server.messageframework.message.component.ComponentTransformer
import com.unityrealms.skyblock.miningmodule.mining.MiningCore
import com.unityrealms.skyblock.miningmodule.mining.configuration.MiningConfiguration
import com.unityrealms.skyblock.miningmodule.mining.profile.MiningExperienceResult
import com.unityrealms.skyblock.miningmodule.mining.profile.MiningProfile

import java.time.Duration
import kotlin.math.cos
import kotlin.math.sin

import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title

import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.block.Block
import org.bukkit.block.data.BlockData
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin

/** Handles mining crack, particle, sound and progression animations. */
class MiningAnimationManager(
  private val plugin: Plugin,
  animationConfiguration: MiningConfiguration.Animation,
  sessionConfiguration: MiningConfiguration.Session
) {

  @Volatile
  private var animationConfiguration: MiningConfiguration.Animation = animationConfiguration

  @Volatile
  private var sessionConfiguration: MiningConfiguration.Session = sessionConfiguration

  fun updateConfiguration(
    animationConfiguration: MiningConfiguration.Animation,
    sessionConfiguration: MiningConfiguration.Session
  ) {
    this.animationConfiguration = animationConfiguration
    this.sessionConfiguration = sessionConfiguration
  }

  /** Updates the client-side block crack stage. */
  fun updateDamage(block: Block, progress: Float) {
    for (viewer in this.viewers(block.location)) {
      viewer.sendBlockDamage(block.location, progress.coerceIn(0.0F, 0.99F))
    }
  }

  /** Clears client-side block crack state. */
  fun resetDamage(block: Block) {
    for (viewer in this.viewers(block.location)) {
      viewer.sendBlockDamage(block.location, 0.0F)
    }
  }

  /** Plays progressive hit feedback. */
  fun playHit(block: Block, progress: Double) {
    if (!(this.animationConfiguration.enabled)) {
      return
    }

    val location = block.location.add(0.5, 0.5, 0.5)
    block.world.spawnParticle(
      Particle.BLOCK,
      location,
      this.animationConfiguration.hitParticleAmount,
      0.24,
      0.24,
      0.24,
      0.02,
      block.blockData
    )
    val pitch = (0.75 + progress.coerceIn(0.0, 1.0) * 0.65).toFloat()
    block.world.playSound(location, Sound.BLOCK_STONE_HIT, 0.45F, pitch)
  }

  /** Plays the block completion animation. */
  fun playBreak(location: Location, originalBlockData: BlockData) {
    if (!(this.animationConfiguration.enabled)) {
      return
    }

    val center = location.clone().add(0.5, 0.5, 0.5)
    location.world?.spawnParticle(
      Particle.BLOCK,
      center,
      this.animationConfiguration.breakParticleAmount,
      0.38,
      0.38,
      0.38,
      0.08,
      originalBlockData
    )
    location.world?.spawnParticle(Particle.CRIT, center, 8, 0.35, 0.35, 0.35, 0.12)
    location.world?.playSound(center, Sound.BLOCK_STONE_BREAK, 0.9F, 1.1F)
    location.world?.playSound(center, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.55F, 1.55F)
  }

  /** Plays a block respawn animation. */
  fun playRespawn(location: Location, blockData: BlockData) {
    if (!(this.animationConfiguration.enabled)) {
      return
    }

    val center = location.clone().add(0.5, 0.5, 0.5)
    location.world?.spawnParticle(
      Particle.BLOCK,
      center,
      this.animationConfiguration.respawnParticleAmount,
      0.32,
      0.32,
      0.32,
      0.02,
      blockData
    )
    location.world?.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_PLACE, 0.65F, 1.35F)
  }

  /** Displays mining experience progress to the player. */
  fun playExperience(player: Player, result: MiningExperienceResult) {
    if (!(result.profile.animationEnabled)) {
      return
    }

    if (result.profile.level >= MiningCore.miningLevelManager.maximumLevel) {
      player.sendActionBar(
        ComponentTransformer.transform(
          MiningCore.resolveMessage("message.mining.maximum_level_action_bar")
        )
      )
      return
    }

    player.sendActionBar(
      ComponentTransformer.transform(
        MiningCore.resolveMessage(
          "message.mining.experience_action_bar",
          result.awardedExperience,
          result.profile.experience,
          result.requiredExperience
        )
      )
    )
  }

  /** Plays a level-up animation. */
  fun playLevelUp(player: Player, profile: MiningProfile, level: Int) {
    if (!(profile.animationEnabled)) {
      return
    }

    val milestone = level % 10 == 0 || level == MiningCore.miningLevelManager.maximumLevel
    val title = Title.title(
      ComponentTransformer.transform(if (milestone) "#f7c948<bold>MINING MILESTONE</bold>" else "#56D18F<bold>MINING LEVEL UP</bold>"),
      ComponentTransformer.transform("#ffffffLevel $level"),
      Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(if (milestone) 1800 else 1200), Duration.ofMillis(400))
    )
    player.showTitle(title)
    player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 0.9F, if (milestone) 0.85F else 1.25F)
    this.spawnRing(player.location.clone().add(0.0, 0.2, 0.0), if (milestone) Particle.END_ROD else Particle.HAPPY_VILLAGER, if (milestone) 2.0 else 1.35)
  }

  /** Plays a prestige animation. */
  fun playPrestige(player: Player, profile: MiningProfile) {
    if (!(profile.animationEnabled)) {
      return
    }

    player.showTitle(
      Title.title(
        ComponentTransformer.transform("#c778ff<bold>MINING PRESTIGE</bold>"),
        ComponentTransformer.transform("#ffffffPrestige ${profile.prestige}"),
        Title.Times.times(Duration.ofMillis(300), Duration.ofMillis(2400), Duration.ofMillis(600))
      )
    )
    player.playSound(player.location, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 0.9F)

    for (wave in 0..2) {
      this.plugin.server.scheduler.runTaskLater(this.plugin, Runnable {
        if (player.isOnline) {
          this.spawnRing(player.location.clone().add(0.0, 0.2 + wave * 0.45, 0.0), Particle.END_ROD, 1.3 + wave * 0.35)
          player.world.spawnParticle(Particle.TOTEM_OF_UNDYING, player.location.clone().add(0.0, 1.0, 0.0), 18, 0.5, 0.8, 0.5, 0.08)
        }
      }, wave * 8L)
    }
  }

  private fun viewers(location: Location): List<Player> {
    val world = location.world ?: return emptyList()
    val maximumDistanceSquared = this.sessionConfiguration.particleViewDistance * this.sessionConfiguration.particleViewDistance

    return world.players.filter { player ->
      player.location.distanceSquared(location) <= maximumDistanceSquared
    }
  }

  private fun spawnRing(location: Location, particle: Particle, radius: Double) {
    val world = location.world ?: return

    for (index in 0 until 32) {
      val angle = Math.PI * 2.0 * index / 32.0
      world.spawnParticle(
        particle,
        location.clone().add(cos(angle) * radius, 0.15, sin(angle) * radius),
        1,
        0.0,
        0.0,
        0.0,
        0.0
      )
    }
  }
}
