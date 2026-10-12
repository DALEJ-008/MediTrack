package com.example.meditrack.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.sql.Time

@Entity(tableName = "horario")

data class Horario(
    @PrimaryKey(autoGenerate = true)
    val idHorario: Int= 0,

    val idMedicamento: Int,
    val hora: Time,
    val Activo: Boolean = true,
    val creadoEn: Long = System.currentTimeMillis()

)