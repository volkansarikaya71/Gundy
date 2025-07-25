package com.drawaero_vs.gundy2


import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {


    @Multipart
    @POST("gundy/parse_image")
    fun uploadImage(
        @Part("device_id") deviceId: RequestBody,
        @Part image: MultipartBody.Part
    ): Call<ApiResponse>

    @Headers("Content-Type: application/json")
    @POST("gundy/parse_text")
    suspend fun sendMessage(@Body request: MessageRequest): ApiResponse







}
