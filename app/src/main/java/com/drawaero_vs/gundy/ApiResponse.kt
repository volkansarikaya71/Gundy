package com.drawaero_vs.gundy2

import com.google.gson.annotations.SerializedName

data class ApiResponse(
    val success: Boolean,
    val message: String
)

data class MessageRequest(
    @SerializedName("device_id") val device_id: String,
    @SerializedName("text") val text: String
)



