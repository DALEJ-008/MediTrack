package com.example.meditrack.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "registroDosis")
data class RegistroDosis(
    @PrimaryKey(autoGenerate = true)
    val idRegistro: Int = 0,

    val idMedicamento: Int,
    val idHorario: Int,
    val fechaHoraProgramada: Long = System.currentTimeMillis(),
    val fechaHoraTomada: Long = System.currentTimeMillis(),
    val estado: String,
    val accion: String
)