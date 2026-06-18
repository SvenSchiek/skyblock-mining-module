package com.unityrealms.skyblock.miningmodule.tool.profile.repository.implementation

import com.unityrealms.skyblock.miningmodule.tool.profile.ToolProfile
import com.unityrealms.skyblock.miningmodule.tool.profile.repository.ToolProfileRepository

import java.sql.PreparedStatement
import java.util.UUID
import javax.sql.DataSource

/**
 * Represents a JDBC implementation of the tool profile repository.
 *
 * @property dataSource The data source to use for database connections.
 */
class JdbcToolProfileRepository(private val dataSource: DataSource) : ToolProfileRepository {

  /**
   * Saves the given tool profile.
   *
   * @param toolProfile The tool profile to save.
   */
  override fun save(toolProfile: ToolProfile) {
    this.dataSource.connection.use { connection ->
      connection.prepareStatement("""
        UPDATE mining_tool_profile_data
          SET owner_identifier = ?, experience = ?, level = ?, prestige = ?, enchantment_token_count = ?,
            total_earned_enchantment_token_count = ?, experience_multiplier = ?, fragment_chance_multiplier = ?,
            selected_ability_identifier = ?, created_at = ?, updated_at = ?
          WHERE tool_identifier = ?
      """.trimIndent()).use { preparedStatement ->
        this.bind(preparedStatement, toolProfile, false)

        if (preparedStatement.executeUpdate() == 0) {
          connection.prepareStatement("""
            INSERT INTO mining_tool_profile_data(
              tool_identifier, owner_identifier, experience, level, prestige, enchantment_token_count,
              total_earned_enchantment_token_count, experience_multiplier, fragment_chance_multiplier,
              selected_ability_identifier, created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
          """.trimIndent()).use { insertPreparedStatement ->
            this.bind(insertPreparedStatement, toolProfile, true)

            insertPreparedStatement.executeUpdate()
          }
        }
      }
    }
  }

  /**
   * Loads the tool profile with the specified identifier.
   *
   * @param toolIdentifier The unique identifier of the tool profile to load.
   *
   * @return The tool profile with the specified identifier, or null if no profile is found with that identifier.
   */
  override fun load(toolIdentifier: UUID): ToolProfile? {
    this.dataSource.connection.use { connection ->
      connection.prepareStatement("SELECT * FROM mining_tool_profile_data WHERE tool_identifier = ?").use { preparedStatement ->
        preparedStatement.setString(1, toolIdentifier.toString())

        preparedStatement.executeQuery().use { resultSet ->
          if (!(resultSet.next())) {
            return null
          }

          return ToolProfile(
            toolIdentifier = toolIdentifier,
            ownerIdentifier = resultSet.getString("owner_identifier")?.let(UUID::fromString),

            experience = resultSet.getLong("experience"),
            level = resultSet.getInt("level"),
            prestige = resultSet.getInt("prestige"),

            enchantmentTokenCount = resultSet.getInt("enchantment_token_count"),
            totalEarnedEnchantmentTokenCount = resultSet.getInt("total_earned_enchantment_token_count"),

            experienceMultiplier = resultSet.getDouble("pickaxe_experience_multiplier"),
            fragmentChanceMultiplier = resultSet.getDouble("fragment_chance_multiplier"),

            selectedAbilityIdentifier = resultSet.getString("selected_ability_identifier"),

            createdAt = resultSet.getLong("created_at"),
            updatedAt = resultSet.getLong("updated_at")
          )
        }
      }
    }
  }


  /**
   * Deletes the tool profile with the specified identifier.
   *
   * @param toolIdentifier The unique identifier of the tool profile to delete.
   */
  override fun delete(toolIdentifier: UUID) {
    this.dataSource.connection.use { connection ->
      connection.prepareStatement("DELETE FROM mining_tool_profile_data WHERE tool_identifier = ?").use { preparedStatement ->
        preparedStatement.setString(1, toolIdentifier.toString())

        preparedStatement.executeUpdate()
      }
    }
  }


  /**
   * Binds the values of the given tool profile to the specified prepared statement for either an insert or update operation.
   *
   * @param preparedStatement The prepared statement to which to bind the values of the tool profile.
   * @param toolProfile The tool profile whose values to bind to the prepared statement.
   * @param insert Whether the prepared statement is for an insert operation or an update operation, which determines the order of the bound values.
   */
  private fun bind(preparedStatement: PreparedStatement, toolProfile: ToolProfile, insert: Boolean) {
    if (insert) {
      preparedStatement.setString(1, toolProfile.toolIdentifier.toString())
      preparedStatement.setString(2, toolProfile.ownerIdentifier?.toString())
      preparedStatement.setLong(3, toolProfile.experience)
      preparedStatement.setInt(4, toolProfile.level)
      preparedStatement.setInt(5, toolProfile.prestige)
      preparedStatement.setInt(6, toolProfile.enchantmentTokenCount)
      preparedStatement.setInt(7, toolProfile.totalEarnedEnchantmentTokenCount)
      preparedStatement.setDouble(8, toolProfile.experienceMultiplier)
      preparedStatement.setDouble(9, toolProfile.fragmentChanceMultiplier)
      preparedStatement.setString(10, toolProfile.selectedAbilityIdentifier)
      preparedStatement.setLong(11, toolProfile.createdAt)
      preparedStatement.setLong(12, toolProfile.updatedAt)

      return
    }

    preparedStatement.setString(1, toolProfile.ownerIdentifier?.toString())
    preparedStatement.setLong(2, toolProfile.experience)
    preparedStatement.setInt(3, toolProfile.level)
    preparedStatement.setInt(4, toolProfile.prestige)
    preparedStatement.setInt(5, toolProfile.enchantmentTokenCount)
    preparedStatement.setInt(6, toolProfile.totalEarnedEnchantmentTokenCount)
    preparedStatement.setDouble(7, toolProfile.experienceMultiplier)
    preparedStatement.setDouble(8, toolProfile.fragmentChanceMultiplier)
    preparedStatement.setString(9, toolProfile.selectedAbilityIdentifier)
    preparedStatement.setLong(10, toolProfile.createdAt)
    preparedStatement.setLong(11, toolProfile.updatedAt)
    preparedStatement.setString(12, toolProfile.toolIdentifier.toString())
  }
}
