package com.waterloo.wit.data

data class WorkItem(
    var UID: String,
    var Building_UID: String,
    var WorkTitle: String,
    var Date_Time: String,
    var Area: String,
    var Notes: String,
    var GPS_Latitude: String,
    var GPS_Longitude: String,
    var FloorLevel: Int,
    var UserId: String

)
