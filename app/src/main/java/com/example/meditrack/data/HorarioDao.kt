package com.example.meditrack.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface HorarioDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(horario: Horario): Long

    @Query("SELECT * FROM horario WHERE idMedicamento = :medId AND Activo = 1")
    fun getHorariosPorMedicamento(medId: Int): Flow<List<Horario>>

    @Query("SELECT * FROM horario WHERE idMedicamento = :medId AND Activo = 1")
    suspend fun getHorariosPorMedicamentoSync(medId: Int): List<Horario>

    @Query("SELECT * FROM horario")
    fun getTodosLosHorarios(): Flow<List<Horario>>

    @Query("UPDATE horario SET Activo = 0 WHERE idMedicamento = :medId")
    suspend fun desactivarHorariosDeMedicamento(medId: Int)

    @Update
    suspend fun actualizar(horario: Horario)

    @Query("DELETE FROM horario")
    suspend fun eliminarTodos()
}
