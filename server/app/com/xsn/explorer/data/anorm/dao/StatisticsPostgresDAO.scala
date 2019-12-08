package com.xsn.explorer.data.anorm.dao

import java.sql.Connection

import anorm._
import com.xsn.explorer.data.anorm.parsers.BlockRewardParsers.parseSummary
import com.xsn.explorer.data.anorm.parsers.StatisticsParsers
import com.xsn.explorer.models.{BlockRewardsSummary, Statistics}

class StatisticsPostgresDAO {

  import StatisticsPostgresDAO._

  def getStatistics(implicit conn: Connection): Statistics = {
    val result = SQL(
      s"""
        |SELECT (
        |    (SELECT value FROM aggregated_amounts WHERE name = 'available_coins') -
        |    (SELECT COALESCE(SUM(received - spent), 0) FROM balances WHERE address = '$BurnAddress')
        |  ) AS total_supply,
        |  (
        |    (SELECT value FROM aggregated_amounts WHERE name = 'available_coins') -
        |    (SELECT COALESCE(SUM(received - spent), 0) FROM balances WHERE address IN (SELECT address FROM hidden_addresses))
        |  ) AS circulating_supply,
        |  (SELECT n_live_tup FROM pg_stat_all_tables WHERE relname = 'transactions') AS transactions,
        |  (SELECT COALESCE(MAX(height), 0) FROM blocks) AS blocks
      """.stripMargin
    ).as(StatisticsParsers.parseStatistics.single)

    result
  }

  def getSummary(numberOfBlocks: Int)(implicit conn: Connection): BlockRewardsSummary = {
    SQL(
      """
        |SELECT
        |  COALESCE(AVG(reward), 0) AS average_reward,
        |  CASE WHEN COUNT(*) > 0 THEN SUM(pos_staked_amount + tpos_staked_amount) / COUNT(*) ELSE 0 END as average_input,
        |  CASE WHEN SUM(pos_reward) > 0 THEN SUM(pos_staked_amount) / SUM(pos_reward) ELSE 0 END AS pos_average_input,
        |  CASE WHEN SUM(tpos_reward) > 0 THEN SUM(tpos_staked_amount) / SUM(tpos_reward) ELSE 0 END AS tpos_average_input,
        |  COALESCE(PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY staked_time), 0) AS median_wait_time
        |FROM (
        |  SELECT
        |    SUM(r.value) AS reward,
        |    SUM(CASE WHEN b.extraction_method = 'PoS' THEN r.staked_amount ELSE 0 END) AS pos_staked_amount,
        |    SUM(CASE WHEN b.extraction_method = 'TPoS' THEN r.staked_amount ELSE 0 END) AS tpos_staked_amount,
        |    SUM(r.staked_time) AS staked_time,
        |    CASE WHEN b.extraction_method = 'PoS' THEN 1 ELSE 0 END AS pos_reward,
        |    CASE WHEN b.extraction_method = 'TPoS' THEN 1 ELSE 0 END AS tpos_reward
        |  FROM (
        |      SELECT blockhash, height, extraction_method FROM blocks ORDER BY height DESC LIMIT {number_of_blocks}
        |  ) b INNER JOIN block_rewards r USING(blockhash)
        |  WHERE r.type != 'MASTERNODE'
        |  GROUP BY b.height, b.extraction_method
        |) t
      """.stripMargin
    ).on(
        'number_of_blocks -> numberOfBlocks
      )
      .as(parseSummary.single)
  }
}

object StatisticsPostgresDAO {

  /**
   * We need to exclude the burn address from the total supply.
   */
  val BurnAddress = "XmPe9BHRsmZeThtYF34YYjdnrjmcAUn8bC"
}
