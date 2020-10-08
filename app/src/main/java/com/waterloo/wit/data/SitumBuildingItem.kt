package com.waterloo.wit.data

import es.situm.sdk.model.cartography.Building
import es.situm.sdk.model.cartography.Floor

data class SitumBuildingItem(
    var building: Building?,
    var floor: Floor?,
    var itemType: Int

)