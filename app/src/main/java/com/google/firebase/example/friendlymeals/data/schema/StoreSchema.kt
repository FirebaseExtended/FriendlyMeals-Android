package com.google.firebase.example.friendlymeals.data.schema

import kotlinx.serialization.Serializable

@Serializable
data class StoreSchema(
    val name: String = "",
    val address: String = "",
    val distance: String = "",
    val openNow: Boolean = false,
    val closingSoon: Boolean = false,
    val hasParking: Boolean = false,
    val parkingDetails: String = "",
    val mapUrl: String = ""
)

@Serializable
data class StoreLocalizerResult(
    val stores: List<StoreSchema> = emptyList()
)
