package com.aggregateStructuredStream

/**
  * Created by ankur on 19.12.16.
  * Modified by ges
  */

import java.util.Date
import java.util.Calendar
import java.sql.Timestamp
import java.text.{DateFormat, SimpleDateFormat}

object Commons {

  case class UserEvent(device_id: String, category:String, window_time:String, m1_sum_downstream: String, m2_sum_downstream: String)
      extends Serializable

  def getTimeStamp(timeStr: String): String = {
    val timeL : Long = timeStr.toLong
    val ts = new Date(timeL)  
    val calendar = Calendar.getInstance
    calendar.setTime(ts) 
    val sec = calendar.get(Calendar.SECOND) % 5 
    calendar.add(Calendar.SECOND, - sec)
    val format1 : SimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    format1.format(calendar.getTime)
  }	

}
