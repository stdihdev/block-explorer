package com.xsn.explorer.helpers

import java.io.File

import com.xsn.explorer.models._
import com.xsn.explorer.models.values.Blockhash
import play.api.libs.json.{JsValue, Json}

import scala.util.Try

object BlockLoader {

  private val BasePath = "blocks"
  private val FullBlocksBasePath = "full-blocks"

  def get(blockhash: String): persisted.Block = {
    val rpcBlock = getRPC(blockhash)
    Converters.toPersistedBlock(rpcBlock)
  }

  def getWithTransactions(blockhash: String): persisted.Block.HasTransactions = {
    val rpcBlock = getRPC(blockhash)
    val block = Converters.toPersistedBlock(rpcBlock)
    val transactions = rpcBlock.transactions
      .map(_.string)
      .map(TransactionLoader.getWithValues)
      .map(persisted.Transaction.fromRPC)
      .map(_._1)

    persisted.Block.HasTransactions(block, transactions)
  }

  def getWithTransactions(rpcBlock: rpc.Block.Canonical): persisted.Block.HasTransactions = {
    val block = Converters.toPersistedBlock(rpcBlock)
    val transactions = rpcBlock.transactions
      .map(_.string)
      .map(TransactionLoader.getWithValues)
      .map(_.copy(blockhash = rpcBlock.hash))
      .map(persisted.Transaction.fromRPC)
      .map(_._1)

    persisted.Block.HasTransactions(block, transactions)
  }

  def getRPC(blockhash: String): rpc.Block.Canonical = {
    val partial = json(blockhash).as[rpc.Block.Canonical]
    cleanGenesisBlock(partial)
  }

  def getFullRPC(blockhash: String, coin: String = "xsn"): rpc.Block.HasTransactions[rpc.TransactionVIN] = {
    val resource = s"$FullBlocksBasePath/$coin/$blockhash"
    jsonFromResource(resource).as[rpc.Block.HasTransactions[rpc.TransactionVIN]]
  }

  def getFullRPCOpt(blockhash: String, coin: String = "xsn"): Option[rpc.Block.HasTransactions[rpc.TransactionVIN]] = {
    Try(getFullRPC(blockhash, coin)).toOption
  }

  def json(blockhash: String): JsValue = {
    val resource = s"$BasePath/$blockhash"
    jsonFromResource(resource)
  }

  def all(): List[persisted.Block] = {
    allRPC()
      .map(Converters.toPersistedBlock)
  }

  def allRPC(): List[rpc.Block.Canonical] = {
    val uri = getClass.getResource(s"/$BasePath")
    new File(uri.getPath)
      .listFiles()
      .toList
      .map(_.getName)
      .map(getRPC)
  }

  def cleanGenesisBlock(block: rpc.Block.Canonical): rpc.Block.Canonical = {
    val genesisBlockhash: Blockhash =
      Blockhash.from("00000c822abdbb23e28f79a49d29b41429737c6c7e15df40d1b1f1b35907ae34").get

    Option(block)
      .filter(_.hash == genesisBlockhash)
      .map(_.copy(transactions = List.empty))
      .getOrElse(block)
  }

  private def jsonFromResource(resource: String): JsValue = {
    try {
      val json = scala.io.Source.fromResource(resource).getLines().mkString("\n")
      Json.parse(json)
    } catch {
      case _: Throwable => throw new RuntimeException(s"Resource $resource not found")
    }
  }
}
