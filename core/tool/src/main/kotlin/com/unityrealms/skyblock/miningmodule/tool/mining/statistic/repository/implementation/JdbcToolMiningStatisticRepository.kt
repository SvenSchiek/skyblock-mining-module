package com.unityrealms.skyblock.miningmodule.tool.mining.statistic.repository.implementation

import com.unityrealms.skyblock.miningmodule.tool.mining.statistic.ToolMiningStatistic
import com.unityrealms.skyblock.miningmodule.tool.mining.statistic.repository.ToolMiningStatisticRepository
import com.unityrealms.skyblock.miningmodule.tool.repository.implementation.ToolMapCodec

import java.util.UUID
import javax.sql.DataSource

/**
 * Represents a JDBC implementation of the tool mining statistics repository.
 *
 * @property dataSource The data source to use for database connections.
 */
class JdbcToolMiningStatisticRepository(private val dataSource: DataSource) : ToolMiningStatisticRepository {

  /**
   * Saves the given tool mining statistic.
   *
   * @param toolMiningStatistic The tool mining statistic to save.
   */
  override fun save(toolMiningStatistic: ToolMiningStatistic) {
    this.dataSource.connection.use { connection ->
      connection.prepareStatement("""
        UPDATE mining_tool_statistic_data SET total_mined_block_count = ?, total_mined_ore_count = ?, block_by_category_map = ?, ore_by_category_map = ?, found_fragment_map = ?, updated_at = ? WHERE tool_identifier = ?
      """.trimIndent()).use { preparedStatement ->
        preparedStatement.setLong(1, toolMiningStatistic.totalMinedBlockCount)
        preparedStatement.setLong(2, toolMiningStatistic.totalMinedOreCount)
        preparedStatement.setString(3, ToolMapCodec.encodeStringLongMap(toolMiningStatistic.blockByCategoryMap))
        preparedStatement.setString(4, ToolMapCodec.encodeStringLongMap(toolMiningStatistic.oreByCategoryMap))
        preparedStatement.setString(5, ToolMapCodec.encodeRarityLongMap(toolMiningStatistic.foundFragmentMap))
        preparedStatement.setLong(6, toolMiningStatistic.updatedAt)
        preparedStatement.setString(7, toolMiningStatistic.toolIdentifier.toString())

        if (preparedStatement.executeUpdate() == 0) {
          connection.prepareStatement("""
            INSERT INTO mining_tool_statistic_data(
              tool_identifier, total_mined_block_count, total_mined_ore_count, block_by_category_map, ore_by_category_map, found_fragment_map, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?)
          """.trimIndent()).use { insertPreparedStatement ->
            insertPreparedStatement.setString(1, toolMiningStatistic.toolIdentifier.toString())
            insertPreparedStatement.setLong(2, toolMiningStatistic.totalMinedBlockCount)
            insertPreparedStatement.setLong(3, toolMiningStatistic.totalMinedOreCount)
            insertPreparedStatement.setString(4, ToolMapCodec.encodeStringLongMap(toolMiningStatistic.blockByCategoryMap))
            insertPreparedStatement.setString(5, ToolMapCodec.encodeStringLongMap(toolMiningStatistic.oreByCategoryMap))
            insertPreparedStatement.setString(6, ToolMapCodec.encodeRarityLongMap(toolMiningStatistic.foundFragmentMap))
            insertPreparedStatement.setLong(7, toolMiningStatistic.updatedAt)

            insertPreparedStatement.executeUpdate()
          }
        }
      }
    }
  }

  /**
   * Loads the tool mining statistic with the specified identifier.
   *
   * @param toolIdentifier The unique identifier of the tool mining statistic to load.
   *
   * @return The tool mining statistic with the specified identifier, or null if no statistic is found with that identifier.
   */
  override fun load(toolIdentifier: UUID): ToolMiningStatistic? {
    this.dataSource.connection.use { connection ->
      connection.prepareStatement("SELECT * FROM mining_tool_statistic_data WHERE tool_identifier = ?").use { preparedStatement ->
        preparedStatement.setString(1, toolIdentifier.toString())

        preparedStatement.executeQuery().use { resultSet ->
          if (!(resultSet.next())) {
            return null
          }

          return ToolMiningStatistic(
            toolIdentifier = toolIdentifier,

            totalMinedBlockCount = resultSet.getLong("total_mined_block_count"),
            totalMinedOreCount = resultSet.getLong("total_mined_ore_count"),

            blockByCategoryMap = ToolMapCodec.decodeStringLongMap(resultSet.getString("block_by_category_map")),
            oreByCategoryMap = ToolMapCodec.decodeStringLongMap(resultSet.getString("ore_by_category_map")),

            foundFragmentMap = ToolMapCodec.decodeRarityLongMap(resultSet.getString("found_fragment_map")),

            updatedAt = resultSet.getLong("updated_at")
          )
        }
      }
    }
  }


  /**
   * Deletes the tool mining statistic with the specified identifier.
   *
   * @param toolIdentifier The unique identifier of the tool mining statistic to delete.
   */
  override fun delete(toolIdentifier: UUID) {
    this.dataSource.connection.use { connection ->
      connection.prepareStatement("DELETE FROM mining_tool_statistic_data WHERE tool_identifier = ?").use { preparedStatement ->
        preparedStatement.setString(1, toolIdentifier.toString())

        preparedStatement.executeUpdate()
      }
    }
  }
}
