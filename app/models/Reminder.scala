package models

case class Reminder(id: Long, user: User, content: Reminder#Content)  {


  def getText() {
    content.getText()
  }

  class Content {
    def getText() : String = {
      return "My Text Content"
    }

  }

}
