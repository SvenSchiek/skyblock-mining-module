package com.unityrealms.skyblock.miningmodule.tool.mining

import com.unityrealms.skyblock.miningmodule.tool.ActivationSource
import com.unityrealms.skyblock.miningmodule.tool.ToolMiningChainState
import com.unityrealms.skyblock.miningmodule.tool.profile.ToolProfile

import org.bukkit.block.Block
import org.bukkit.entity.Player

/**
 * Represents a validated mining context shared by normal mining, enchantments and abilities.
 *
 * @property player The player performing the mining action.
 * @property toolProfile The profile of the tool being used for mining.
 * @property originBlock The original block being mined that initiated the mining context.
 * @property miningBlockIdentifier The identifier of the block being mined.
 * @property blockCategory The category of the block being mined.
 * @property oreCategory The ore category of the block being mined, if applicable.
 * @property activationSource The source of the mining activation.
 * @property recursionDepth The current recursion depth for secondary block processing.
 * @property toolMiningChainState The shared state for the current mining activation chain, used for safety checks.
 * @property allowAnySecondaryEnchantment Whether to allow any enchantment to process secondary blocks.
 * @property allowFragmentRolling Whether to allow fragment rolls for secondary blocks.
 * @property secondaryBlockProcessor The function to process secondary blocks through the mining pipeline.
 */
data class ToolMiningContext(
  val player: Player,
  val toolProfile: ToolProfile,

  val originBlock: Block,
  val miningBlockIdentifier: String,
  val blockCategory: String,
  val oreCategory: String?,

  val activationSource: ActivationSource,

  val recursionDepth: Int,

  val toolMiningChainState: ToolMiningChainState,

  val allowAnySecondaryEnchantment: Boolean,
  val allowFragmentRolling: Boolean,

  val secondaryBlockProcessor: (Block, ToolMiningContext) -> Boolean
) {

  /**
   * Processes a secondary block through the controlled mining pipeline.
   *
   * @param block The block to process.
   * @param allowFragmentRolling Whether to allow fragment rolls for this secondary block.
   *
   * @return True if the block was successfully processed, false otherwise.
   */
  fun processSecondaryBlock(block: Block, allowFragmentRolling: Boolean): Boolean {
    if (this.recursionDepth >= toolMiningChainState.maximumRecursionDepth) {
      return false
    }

    if (!(toolMiningChainState.tryReserveBlock())) {
      return false
    }

    return this.secondaryBlockProcessor(
      block,
      this.copy(
        activationSource = if (this.activationSource == ActivationSource.ABILITY) {
          ActivationSource.ABILITY
        } else {
          ActivationSource.ENCHANTMENT
        },

        recursionDepth = this.recursionDepth + 1,

        allowAnySecondaryEnchantment = false,
        allowFragmentRolling = allowFragmentRolling
      )
    )
  }
}
