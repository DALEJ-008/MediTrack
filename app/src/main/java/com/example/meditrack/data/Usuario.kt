package com.example.meditrack.data

import androidx.room.Entity
import androidx.room.PrimaryKey

//Esta tabla guarda la informacion del usuario

@Entity (tableName = "usuario")
data class Usuario(
    @PrimaryKey (autoGenerate= true)
    val idUsuario: Int,

    val nombreUsuario: String,
    val correo: String,
    val contrasenaHash: String,
    val fechaCreacion: Long = System.currentTimeMillis()
)