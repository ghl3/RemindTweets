package controllers

import play.api.mvc._
import models._

import play.api.db.slick._


object RestReminder extends Controller {


  def reminder(id: Long) = DBAction {
    implicit rs =>

      val reminder = Reminders.findById(id)

      if (reminder.isEmpty) {
        NotFound("Reminder with id %s was not found".format(id))
      } else {
        val scheduledReminders = reminder.get.getScheduledReminders
        Ok(views.html.reminder(reminder.get, scheduledReminders))
      }
  }
}
