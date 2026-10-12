package com.example.meditrack.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicamentoDao {

    @Query("SELECT * FROM medicamento WHERE idUsuario = :uid")
    fun getMedicamentosDeUsuario(uid: Int): Flow<List<Medicamento>>

    @Query("SELECT * FROM medicamento WHERE idMedicamento = :id")
    suspend fun getMedicamentoPorId(id: Int): Medicamento?

    @Query("SELECT * FROM medicamento WHERE idMedicamento = :id")
    fun getMedicamentoPorIdSync(id: Int): Medicamento?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(med: Medicamento): Long

    @Update
    suspend fun actualizar(med: Medicamento)

    @Delete
    suspend fun eliminar(med: Medicamento)

    @Query("DELETE FROM medicamento")
    suspend fun eliminarTodos()
}
