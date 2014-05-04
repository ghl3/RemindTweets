package app

import scala.slick.driver.PostgresDriver
import com.github.tminglei.slickpg._
import models.Repeat
import models.Repeat.Frequency

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

/*

trait MyPostgresDriver extends PostgresDriver
with PgArraySupport
with PgDateSupportJoda
with PgRangeSupport
with PgJsonSupport
with PgHStoreSupport
with PgSearchSupport
with PgPostGISSupport {

  /// for json support
  type DOCType = text.Document
  override val jsonMethods = org.json4s.native.JsonMethods

  ///
  override val Implicit = new ImplicitsPlus {}
  override val simple = new SimpleQLPlus {}

  //////
  trait ImplicitsPlus extends Implicits
  with ArrayImplicits
  with DateTimeImplicits
  with RangeImplicits
  with HStoreImplicits
  with JsonImplicits
  with SearchImplicits
  with PostGISImplicits

  trait SimpleQLPlus extends SimpleQL
  with ImplicitsPlus
  with SearchAssistants
}

object MyPostgresDriver extends MyPostgresDriver
*/