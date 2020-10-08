package com.waterloo.wit.data

import es.situm.sdk.model.cartography.Building
import es.situm.sdk.model.cartography.Floor
import es.situm.sdk.model.cartography.Geofence

data class RoomInfoItem(
    var building: String,
    var room: String,
    var description1: String,
    var description2: String,
    var uom: String,
    var amount: String
)