package com.waterloo.wit.data

data class WitDestWorkItem(
    var name: String,
    var latitude: Double,
    var longitude: Double,
    var floorIdentifier: String,
    var floorLevel: Int,
    var buildingIdentifier: String
)