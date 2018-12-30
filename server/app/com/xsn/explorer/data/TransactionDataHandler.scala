package com.xsn.explorer.data

import com.alexitc.playsonify.core.ApplicationResult
import com.alexitc.playsonify.models.ordering.{FieldOrdering, OrderingCondition}
import com.alexitc.playsonify.models.pagination.{Limit, PaginatedQuery, PaginatedResult}
import com.xsn.explorer.models._
import com.xsn.explorer.models.fields.TransactionField
import org.scalactic.Every

import scala.language.higherKinds

trait TransactionDataHandler[F[_]] {

  def getBy(
      address: Address,
      paginatedQuery: PaginatedQuery,
      ordering: FieldOrdering[TransactionField]): F[PaginatedResult[TransactionWithValues]]

  def getBy(address: Address, limit: Limit, lastSeenTxid: Option[TransactionId], orderingCondition: OrderingCondition): F[List[Transaction]]

  def getUnspentOutputs(address: Address): F[List[Transaction.Output]]

  def getByBlockhash(
      blockhash: Blockhash,
      paginatedQuery: PaginatedQuery,
      ordering: FieldOrdering[TransactionField]): F[PaginatedResult[TransactionWithValues]]

  def getByBlockhash(
      blockhash: Blockhash,
      limit: Limit,
      lastSeenTxid: Option[TransactionId]): F[List[TransactionWithValues]]

  def getLatestTransactionBy(addresses: Every[Address]): F[Map[String, String]]
}

trait TransactionBlockingDataHandler extends TransactionDataHandler[ApplicationResult]
