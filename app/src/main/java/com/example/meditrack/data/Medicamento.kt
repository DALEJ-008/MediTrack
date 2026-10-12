package com.example.meditrack.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

//Tabla que guarda la informacion de los medicamentos agregados desde el formulario

@Entity(tableName = "medicamento")

data class Medicamento(
    @PrimaryKey(autoGenerate= true)
    val idMedicamento: Int = 0,

    val idUsuario: Int = 0,

    @SerializedName("producto")
    val nombreMedicamento: String,

    val dosisValor: Float = 0f,

    @SerializedName("formafarmaceutica")
    val formaFarmaceutica: String,

    val notas: String = "",
    val activo: Boolean = true,
    val StockActual: Int = 0,
    val stockMinimo: Int = 5,
    val creadoEn: Long = System.currentTimeMillis()
)