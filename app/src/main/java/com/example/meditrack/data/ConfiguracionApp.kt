package com.example.meditrack.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.sql.Time

//Esta tabla guarda la informacion de la configuracion de la app del usuario

@Entity(tableName =  "configuracionApp")

data class ConfiguracionApp(
    @PrimaryKey(autoGenerate= true)
    val idConfig: Int = 0,

    val idUsuario: Int,
    val notificacionesActivas: Boolean= true,
    val volumenAlarma: String,
    val modoNoMolestar: Boolean = false,
    val noMolestarDesde: Time,
    val noMolestarHasta: Time

)