package controllers

import com.alexitc.playsonify.core.FutureOr.Implicits.FutureOps
import com.alexitc.playsonify.models.ordering.OrderingQuery
import com.alexitc.playsonify.models.pagination.{Limit, Offset, PaginatedQuery}
import com.xsn.explorer.services.BalanceService
import controllers.common.{Codecs, MyJsonController, MyJsonControllerComponents}
import javax.inject.Inject
import play.api.libs.json.{Json}

class BalancesController @Inject()(balanceService: BalanceService, cc: MyJsonControllerComponents)
    extends MyJsonController(cc) {

  import Codecs._

  def getHighest(limit: Int, lastSeenAddress: Option[String]) = public { _ =>
    balanceService
      .getHighest(Limit(limit), lastSeenAddress)
      .toFutureOr
      .map { value =>
        val response = Ok(Json.toJson(value))
        response.withHeaders("Cache-Control" -> "public, max-age=60")
      }
      .toFuture
  }
}
