package com.xsn.explorer.data

import com.alexitc.playsonify.models.ordering.{FieldOrdering, OrderingCondition}
import com.alexitc.playsonify.models.pagination.{Limit, Offset, PaginatedQuery}
import com.alexitc.playsonify.sql.FieldOrderingSQLInterpreter
import com.xsn.explorer.data.anorm.BalancePostgresDataHandler
import com.xsn.explorer.data.anorm.dao.BalancePostgresDAO
import com.xsn.explorer.data.common.PostgresDataHandlerSpec
import com.xsn.explorer.helpers.DataHelper
import com.xsn.explorer.models.Balance
import com.xsn.explorer.models.fields.BalanceField
import org.scalactic.Good

class BalancePostgresDataHandlerSpec extends PostgresDataHandlerSpec {

  lazy val dataHandler = new BalancePostgresDataHandler(database, new BalancePostgresDAO(new FieldOrderingSQLInterpreter))

  val defaultOrdering = FieldOrdering(BalanceField.Available, OrderingCondition.DescendingOrder)

  "upsert" should {
    "create an empty balance" in {
      val address = DataHelper.createAddress("XxQ7j37LfuXgsLd5DZAwFKhT3s2ZMkW85F")
      val balance = Balance(address)

      val result = dataHandler.upsert(balance)
      result mustEqual Good(balance)
    }

    "update an existing balance" in {
      val address = DataHelper.createAddress("XfAATXtkRgCdMTrj2fxHvLsKLLmqAjhEAt")
      val initialBalance = Balance(address, received = BigDecimal(10), spent = BigDecimal(5))
      val patch = Balance(address, received = BigDecimal(10), spent = BigDecimal(10))
      val expected = combine(initialBalance, patch)

      dataHandler.upsert(initialBalance)

      val result = dataHandler.upsert(patch)
      result mustEqual Good(expected)
    }
  }

  "get" should {

    val balances = List(
      Balance(
        address = DataHelper.createAddress("XxQ7j37LfuXgsLd5DZAwFKhT3s2ZMkW85F"),
        received = BigDecimal("1000"),
        spent = BigDecimal("0")),

      Balance(
        address = DataHelper.createAddress("Xbh5pJdBNm8J9PxnEmwVcuQKRmZZ7DkpcF"),
        received = BigDecimal("1000"),
        spent = BigDecimal("100")),

      Balance(
        address = DataHelper.createAddress("XfAATXtkRgCdMTrj2fxHvLsKLLmqAjhEAt"),
        received = BigDecimal("10000"),
        spent = BigDecimal("1000")),

      Balance(
        address = DataHelper.createAddress("XiHW7SR56UPHeXKwcpeVsE4nUfkHv5RqE3"),
        received = BigDecimal("1000"),
        spent = BigDecimal("500"))
    ).sortBy(_.available).reverse

    def prepare() = {
      clearDatabase()

      balances
          .map(dataHandler.upsert)
          .foreach(_.isGood mustEqual true)
    }

    "return the first 3 richest addresses" in {
      prepare()
      val query = PaginatedQuery(Offset(0), Limit(3))
      val expected = balances.take(3)

      val result = dataHandler.get(query, defaultOrdering)
      result.map(_.data) mustEqual Good(expected)
    }

    "skip the first richest address" in {
      prepare()
      val query = PaginatedQuery(Offset(1), Limit(3))
      val expected = balances.drop(1).take(3)

      val result = dataHandler.get(query, defaultOrdering)
      result.map(_.data) mustEqual Good(expected)
    }
  }

  "getBy address" should {
    "return empty values for unknown address" in {
      val address = DataHelper.createAddress("XxQ7j37LfuXGSld5DZAwFKhT3s2ZMkW85F")
      val expected = Balance(address)

      val result = dataHandler.getBy(address)
      result mustEqual Good(expected)
    }

    "return the balance" in {
      val address = DataHelper.createAddress("XxQ7j37LfuXgsLd5DZAWfKhT3s2ZMkW85F")
      val balance = Balance(address, 1000, 500)

      dataHandler.upsert(balance).isGood mustEqual true
      val result = dataHandler.getBy(address)
      result mustEqual Good(balance)
    }
  }

  "getNonZeroBalances" should {
    val balances = List(
      Balance(
        address = DataHelper.createAddress("XiHW7SR56uPHeXKwcpeVsE4nUfkHv5RqE3"),
        received = BigDecimal("0"),
        spent = BigDecimal("0"))
    ).sortBy(_.available).reverse

    def prepare() = {
      clearDatabase()

      balances
          .map(dataHandler.upsert)
          .foreach(_.isGood mustEqual true)
    }
    "ignore addresses with balance = 0" in {
      prepare()
      val query = PaginatedQuery(Offset(0), Limit(1))
      val expected = List.empty[Balance]

      val result = dataHandler.getNonZeroBalances(query, defaultOrdering)
      result.map(_.data) mustEqual Good(expected)
      result.get.total.int mustEqual balances.size - 1
    }
  }

  private def combine(balances: Balance*): Balance = {
    balances.reduce { (a, b) =>
      Balance(a.address, a.received + b.received, a.spent + b.spent)
    }
  }
}
