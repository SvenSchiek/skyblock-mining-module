package com.unityrealms.skyblock.miningmodule.tool.configuration

import com.unityrealms.skyblock.miningmodule.tool.AbilityActivationType
import com.unityrealms.skyblock.miningmodule.tool.AbilityDefinition
import com.unityrealms.skyblock.miningmodule.tool.AbilityLevelDefinition
import com.unityrealms.skyblock.miningmodule.tool.ActivationSource
import com.unityrealms.skyblock.miningmodule.tool.FragmentDefinition
import com.unityrealms.skyblock.miningmodule.tool.FragmentRarity
import com.unityrealms.skyblock.miningmodule.tool.ToolPrestigeConfiguration
import com.unityrealms.skyblock.miningmodule.tool.ToolSafetyConfiguration
import com.unityrealms.skyblock.miningmodule.tool.ToolUnlockRequirement

import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.FileConfiguration

/**
 * Represents the complete mining tool configuration.
 *
 * @property requireProgressionTool Whether the progression tool is required for mining.
 * @property toolMaterial The material of the mining tool.
 * @property toolType The type identifier of the mining tool.
 * @property toolVersion The version of the mining tool configuration.
 * @property maximumLevel The maximum level the mining tool can reach.
 * @property baseExperience The base experience required for the first level.
 * @property experienceMultiplier The multiplier for experience required for each subsequent level.
 * @property toolPrestigeConfiguration The configuration for tool prestige levels and rewards.
 * @property abilityDefinitionList The list of ability definitions available for the tool.
 * @property fragmentDefinitionList The list of fragment definitions available for the tool.
 * @property toolSafetyConfiguration The configuration for tool safety mechanics.
 */
data class ToolConfiguration(
  val requireProgressionTool: Boolean,

  val toolMaterial: Material,
  val toolType: String,
  val toolVersion: Int,

  val maximumLevel: Int,
  val baseExperience: Long,
  val experienceMultiplier: Double,

  val toolPrestigeConfiguration: ToolPrestigeConfiguration,

  val abilityDefinitionList: List<AbilityDefinition>,
  val fragmentDefinitionList: List<FragmentDefinition>,

  val toolSafetyConfiguration: ToolSafetyConfiguration
) {

  /**
   * Represents the loader for the tool configuration from a YAML file.
   */
  object Loader {

    /**
     * Loads the tool configuration from the given YAML file configuration.
     *
     * @param fileConfiguration The YAML file configuration to load from.
     *
     * @return The loaded tool configuration.
     */
    fun fromYaml(fileConfiguration: FileConfiguration): ToolConfiguration {
      return ToolConfiguration(
        requireProgressionTool = fileConfiguration.getBoolean("tool.require_progression_tool", true),

        toolMaterial = Material.matchMaterial(fileConfiguration.getString("tool.identity.material") ?: "DIAMOND_PICKAXE") ?: Material.DIAMOND_PICKAXE,
        toolType = fileConfiguration.getString("tool.identity.type", "mining_pickaxe") ?: "mining_pickaxe",
        toolVersion = fileConfiguration.getInt("tool.identity.version", 1).coerceAtLeast(1),

        maximumLevel = fileConfiguration.getInt("tool.progression.maximum_level", 100).coerceAtLeast(1),
        baseExperience = fileConfiguration.getLong("tool.progression.base_experience", 100L).coerceAtLeast(1L),
        experienceMultiplier = fileConfiguration.getDouble("tool.progression.experience_multiplier", 1.2).coerceAtLeast(1.0),

        toolPrestigeConfiguration = this.loadToolPrestigeConfiguration(fileConfiguration.getConfigurationSection("tool.prestige")),

        abilityDefinitionList = this.loadAbilities(fileConfiguration.getConfigurationSection("tool.abilities")),
        fragmentDefinitionList = this.loadFragments(fileConfiguration.getConfigurationSection("tool.fragments")),
        safety = ToolSafetyConfiguration(
          maximumAffectedBlocksPerActivation = fileConfiguration.getInt(
            "tool.safety.maximum_affected_blocks_per_activation",
            32
          ).coerceAtLeast(1),
          maximumRecursionDepth = fileConfiguration.getInt("tool.safety.maximum_recursion_depth", 1).coerceAtLeast(0),
          secondaryExperienceMultiplier = fileConfiguration.getDouble(
            "tool.safety.secondary_experience_multiplier",
            0.5
          ).coerceAtLeast(0.0),
          secondaryDropMultiplier = fileConfiguration.getDouble("tool.safety.secondary_drop_multiplier", 1.0)
            .coerceAtLeast(0.0),
          allowSecondaryFragmentRolls = fileConfiguration.getBoolean(
            "tool.safety.allow_secondary_fragment_rolls",
            false
          )
        )
      )
    }

    fun loadRequirementList(section: ConfigurationSection?): List<ToolUnlockRequirement> {
      if (section == null) {
        return emptyList()
      }

      val groupSection = section.getConfigurationSection("groups")

      if (groupSection != null) {
        return groupSection.getKeys(false).mapNotNull { key ->
          groupSection.getConfigurationSection(key)?.let(this::loadRequirement)
        }
      }

      return listOf(this.loadRequirement(section))
    }

    private fun loadToolPrestigeConfiguration(configurationSection: ConfigurationSection?): ToolPrestigeConfiguration {
      val maximumPrestige = configurationSection?.getInt("maximum_prestige", 2)?.coerceIn(0, 2) ?: 2

      val requirementMap = mutableMapOf<Int, ToolPrestigeConfiguration.Requirement>()
      val rewardMap = mutableMapOf<Int, ToolPrestigeConfiguration.Reward>()

      for (prestige in 1..maximumPrestige) {
        val prestigeConfigurationSection = configurationSection?.getConfigurationSection("levels.$prestige")

        requirementMap[prestige] = ToolPrestigeConfiguration.Requirement(
          maximumPickaxeLevelRequired = prestigeConfigurationSection?.getBoolean("requirements.maximum_pickaxe_level", true) ?: true,
          totalBlocksMined = prestigeConfigurationSection?.getLong("requirements.total_blocks_mined", 0L)?.coerceAtLeast(0L) ?: 0L,
          totalOresMined = prestigeConfigurationSection?.getLong("requirements.total_ores_mined", 0L)?.coerceAtLeast(0L) ?: 0L
        )

        rewardMap[prestige] = ToolPrestigeConfiguration.Reward(
          pickaxeExperienceMultiplier = prestigeConfigurationSection?.getDouble("rewards.pickaxe_experience_multiplier", 0.1)?.coerceAtLeast(0.0) ?: 0.1,
          fragmentChanceMultiplier = prestigeConfigurationSection?.getDouble("rewards.fragment_chance_multiplier", 0.1)?.coerceAtLeast(0.0) ?: 0.1,
          enchantmentPoints = prestigeConfigurationSection?.getInt("rewards.enchantment_points", 0)?.coerceAtLeast(0) ?: 0
        )
      }

      return ToolPrestigeConfiguration(maximumPrestige, requirementMap, rewardMap)
    }

    private fun loadFragments(section: ConfigurationSection?): List<FragmentDefinition> {
      return FragmentRarity.entries.map { rarity ->
        val raritySection = section?.getConfigurationSection(rarity.name.lowercase())
        val sourceSet = raritySection?.getStringList("eligible_activation_sources")?.mapNotNull { rawSource ->
          runCatching { ActivationSource.valueOf(rawSource.uppercase()) }.getOrNull()
        }?.toSet().orEmpty().ifEmpty { setOf(ActivationSource.NORMAL) }

        FragmentDefinition(
          rarity = rarity,
          baseChance = raritySection?.getDouble("base_chance", 0.001)?.coerceIn(0.0, 1.0) ?: 0.001,
          eligibleActivationSourceSet = sourceSet,
          allowSecondaryBlocks = raritySection?.getBoolean("allow_secondary_blocks", false) ?: false,
          softPityStart = raritySection?.getLong("soft_pity_start", 100L)?.coerceAtLeast(0L) ?: 100L,
          pityGrowth = raritySection?.getDouble("pity_growth", 0.0001)?.coerceAtLeast(0.0) ?: 0.0001,
          hardPityThreshold = raritySection?.getLong("hard_pity_threshold")?.takeIf { it > 0L },
          prestigeBaseChanceMultiplier = raritySection?.getDouble("prestige_base_chance_multiplier", 0.1)
            ?.coerceAtLeast(0.0) ?: 0.1,
          prestigeSoftPityReduction = raritySection?.getLong("prestige_soft_pity_reduction", 0L)?.coerceAtLeast(0L)
            ?: 0L,
          prestigePityGrowthMultiplier = raritySection?.getDouble("prestige_pity_growth_multiplier", 0.0)
            ?.coerceAtLeast(0.0) ?: 0.0
        )
      }
    }

    private fun loadAbilities(configurationSection: ConfigurationSection?): List<AbilityDefinition> {
      if (configurationSection == null) {
        return emptyList()
      }

      return configurationSection.getKeys(false).mapNotNull abilityMap@ { toolAbilityIdentifier ->
        val toolAbilityConfigurationSection = configurationSection.getConfigurationSection(toolAbilityIdentifier) ?: return@abilityMap null

        val toolAbilityActivationType = this.runCatching {
          AbilityActivationType.valueOf((toolAbilityConfigurationSection.getString("activation_type") ?: "RIGHT_CLICK").uppercase())
        }.getOrDefault(AbilityActivationType.RIGHT_CLICK)

        val toolAbilityLevelDefinitionMap = toolAbilityConfigurationSection.getConfigurationSection("levels")?.getKeys(false)?.mapNotNull levelMap@ { rawLevel ->
          val level = rawLevel.toIntOrNull() ?: return@levelMap null
          val levelSection = toolAbilityConfigurationSection.getConfigurationSection("levels.$rawLevel") ?: return@levelMap null
          level to AbilityLevelDefinition(
            enchantmentPoints = levelSection.getInt("enchantment_points", 0).coerceAtLeast(0),
            fragmentCostMap = this.loadFragmentIntMap(levelSection.getConfigurationSection("fragments")),
            effectConfiguration = this.loadDoubleMap(levelSection.getConfigurationSection("effect_configuration"))
          )
        }?.toMap().orEmpty()

        AbilityDefinition(
          identifier = toolAbilityIdentifier.lowercase(),
          displayName = toolAbilityConfigurationSection.getString("display_name") ?: toolAbilityIdentifier,
          descriptionList = toolAbilityConfigurationSection.getStringList("description"),
          unlockRequirementList = this.loadRequirementList(toolAbilityConfigurationSection.getConfigurationSection("unlock_requirements")),
          activationType = toolAbilityActivationType,
          cooldownMillis = toolAbilityConfigurationSection.getLong(
            "cooldown_millis",
            toolAbilityConfigurationSection.getLong("cooldown_seconds", 60L) * 1000L
          ).coerceAtLeast(0L),
          effectIdentifier = toolAbilityConfigurationSection.getString("effect") ?: toolAbilityIdentifier.lowercase(),
          effectConfiguration = this.loadDoubleMap(toolAbilityConfigurationSection.getConfigurationSection("effect_configuration")),
          levelDefinitionMap = toolAbilityLevelDefinitionMap,
          enabled = toolAbilityConfigurationSection.getBoolean("enabled", true),
          maximumLevel = toolAbilityConfigurationSection.getInt("maximum_level", toolAbilityLevelDefinitionMap.keys.maxOrNull() ?: 1)
            .coerceAtLeast(1)
        )
      }
    }

    private fun loadRequirement(section: ConfigurationSection): ToolUnlockRequirement {
      return ToolUnlockRequirement(
        pickaxeLevel = section.getInt("level", 1).coerceAtLeast(1),
        prestige = section.getInt("prestige", 0).coerceAtLeast(0),
        totalBlocksMined = section.getLong("total_blocks_mined", 0L).coerceAtLeast(0L),
        totalOresMined = section.getLong("total_ores_mined", 0L).coerceAtLeast(0L),
        blockCategory = section.getString("block_category")?.lowercase(),
        blockCategoryAmount = section.getLong("block_category_amount", 0L).coerceAtLeast(0L),
        oreCategory = section.getString("ore_category")?.lowercase(),
        oreCategoryAmount = section.getLong("ore_category_amount", 0L).coerceAtLeast(0L),
        fragmentsObtained = this.loadFragmentLongMap(section.getConfigurationSection("fragments_obtained"))
      )
    }

    private fun loadFragmentIntMap(section: ConfigurationSection?): Map<FragmentRarity, Int> {
      if (section == null) return emptyMap()
      return FragmentRarity.entries.mapNotNull { rarity ->
        section.getInt(rarity.name.lowercase(), 0).takeIf { it > 0 }?.let { rarity to it }
      }.toMap()
    }

    private fun loadFragmentLongMap(section: ConfigurationSection?): Map<FragmentRarity, Long> {
      if (section == null) return emptyMap()
      return FragmentRarity.entries.mapNotNull { rarity ->
        section.getLong(rarity.name.lowercase(), 0L).takeIf { it > 0L }?.let { rarity to it }
      }.toMap()
    }

    private fun loadDoubleMap(section: ConfigurationSection?): Map<String, Double> {
      if (section == null) return emptyMap()
      return section.getKeys(false).associateWith { section.getDouble(it) }
    }
  }
}