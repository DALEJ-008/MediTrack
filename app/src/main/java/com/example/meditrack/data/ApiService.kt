package com.example.meditrack.data

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

// Objeto para recibir los datos exactos de la API del INVIMA
data class MedicamentoDTO(
    @com.google.gson.annotations.SerializedName("producto")
    val producto: String?,
    @com.google.gson.annotations.SerializedName("formafarmaceutica")
    val formafarmaceutica: String?,
    @com.google.gson.annotations.SerializedName("descripcionatc")
    val descripcionatc: String?
)

interface ApiService {
    @GET("resource/i7cb-raxc.json")
    fun getMedicamentos(
        @Query("\$where") where: String? = null,
        @Query("\$limit") limite: Int = 20
    ): Call<List<MedicamentoDTO>>
}
