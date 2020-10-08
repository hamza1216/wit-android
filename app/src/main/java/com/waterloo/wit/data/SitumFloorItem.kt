package com.waterloo.wit.data

import es.situm.sdk.model.cartography.Building
import es.situm.sdk.model.cartography.Floor

data class SitumFloorItem(
    var level: Int,
    var floor: Floor?

)