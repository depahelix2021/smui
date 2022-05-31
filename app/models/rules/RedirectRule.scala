package models.rules

import anorm.SqlParser.get
import anorm.{NamedParameter, RowParser, ~}
import models.input.SearchInputId
import models.{Id, IdObject, Status}
import play.api.libs.json.{JsArray, JsNumber, JsString, JsValue, Json, OFormat}

import java.time.LocalDateTime

class RedirectRuleId(id: String) extends Id(id)
object RedirectRuleId extends IdObject[RedirectRuleId](new RedirectRuleId(_))


case class RedirectRule(id: RedirectRuleId = RedirectRuleId(),
                        target: String,
                        isActive: Boolean,
                        searchInputId: SearchInputId = SearchInputId(),
                        lastUpdate: LocalDateTime = LocalDateTime.now()) extends Rule {

  override def toNamedParameters(searchInputId: SearchInputId): Seq[NamedParameter] = {
    super.toNamedParameters(searchInputId) ++ Seq[NamedParameter](
      RedirectRule.TARGET -> target
    )
  }

  def getTableName: JsString = JsString("delete_rule")

  def getColumns: JsValue = {
    JsArray(
      IndexedSeq (
        JsString("id"),
        JsString("target"),
        JsString("search_input_id"),
        JsString("last_update"),
        JsString("status")
      )
    )
  }

  def getRow: JsValue = {
    JsArray(
      IndexedSeq (
        JsString(id.toString),
        JsString(target),
        JsString(searchInputId.toString),
        JsString(lastUpdate.toString),
        JsNumber(status)
      )
    )
  }
}

object RedirectRule extends RuleObject[RedirectRule] {

  val TABLE_NAME = "redirect_rule"
  val TARGET = "target"

  implicit val jsonFormat: OFormat[RedirectRule] = Json.format[RedirectRule]

  override def fieldNames: Seq[String] = super.fieldNames :+ TARGET

  override def orderByField: String = TARGET

  override val sqlParser: RowParser[RedirectRule] = {
    get[RedirectRuleId](s"$TABLE_NAME.$ID") ~
      get[String](s"$TABLE_NAME.$TARGET") ~
      get[Int](s"$TABLE_NAME.$STATUS") map { case id ~ target ~ status =>
      RedirectRule(id, target, Status.isActiveFromStatus(status))
    }
  }

}