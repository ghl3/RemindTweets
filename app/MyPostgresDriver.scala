package app

import scala.slick.driver.PostgresDriver
import com.github.tminglei.slickpg._
import models.Repeat

trait WithMyDriver {
  val driver: MyPostgresDriver
}


////////////////////////////////////////////////////////////
trait MyPostgresDriver extends PostgresDriver
with PgArraySupport
with PgDateSupportJoda
with PgRangeSupport
with PgHStoreSupport
with PgPlayJsonSupport
with PgSearchSupport
with PgEnumSupport {

  override val Implicit = new ImplicitsPlus {}//with MyEnumImplicits  {}
  override val simple = new SimpleQLPlus {} //with MyEnumImplicits {}

  trait MyEnumImplicits {
    implicit val repeatMapper = createEnumJdbcType("repeat", Repeat)
    implicit val weekDayListTypeMapper = createEnumListJdbcType("repeats", Repeat)

    implicit val repeatColumnExtensionMethodsBuilder = createEnumColumnExtensionMethodsBuilder(Repeat)
    implicit val weekDayOptionColumnExtensionMethodsBuilder = createEnumOptionColumnExtensionMethodsBuilder(Repeat)
  }


  //////
  trait ImplicitsPlus extends Implicits
  with ArrayImplicits
  with DateTimeImplicits
  with RangeImplicits
  with HStoreImplicits
  with JsonImplicits
  with SearchImplicits
  with MyEnumImplicits

  trait SimpleQLPlus extends SimpleQL
  with ImplicitsPlus
  with SearchAssistants
  with MyEnumImplicits
}

object MyPostgresDriver extends MyPostgresDriver
