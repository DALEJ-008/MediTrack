package com.example.meditrack.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RegistroDosisDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(registro: RegistroDosis)

    @Query("SELECT * FROM registroDosis WHERE idMedicamento = :medId AND fechaHoraProgramada >= :inicioDia AND fechaHoraProgramada <= :finDia")
    fun getRegistrosDelDia(medId: Int, inicioDia: Long, finDia: Long): Flow<List<RegistroDosis>>

    @Query("SELECT * FROM registroDosis")
    fun getTodosLosRegistros(): Flow<List<RegistroDosis>>

    @Query("""
        SELECT * FROM registroDosis 
        INNER JOIN medicamento ON registroDosis.idMedicamento = medicamento.idMedicamento
        WHERE registroDosis.fechaHoraProgramada >= :inicioDia AND registroDosis.fechaHoraProgramada <= :finDia
        AND medicamento.activo = 1
    """)
    fun getRegistrosConMedicamentoDelDia(inicioDia: Long, finDia: Long): Flow<Map<RegistroDosis, Medicamento>>

    @Query("""
        SELECT * FROM registroDosis 
        INNER JOIN medicamento ON registroDosis.idMedicamento = medicamento.idMedicamento
        WHERE registroDosis.fechaHoraProgramada >= :inicioMes AND registroDosis.fechaHoraProgramada <= :finMes
    """)
    fun getRegistrosConMedicamentoDelMes(inicioMes: Long, finMes: Long): Flow<Map<RegistroDosis, Medicamento>>

    @Query("""
        SELECT * FROM registroDosis 
        WHERE fechaHoraProgramada >= :inicioDia AND fechaHoraProgramada <= :finDia
    """)
    suspend fun getRegistrosDiaSync(inicioDia: Long, finDia: Long): List<RegistroDosis>

    @Query("""
        SELECT * FROM registroDosis 
        INNER JOIN medicamento ON registroDosis.idMedicamento = medicamento.idMedicamento
        WHERE registroDosis.fechaHoraProgramada >= :inicio AND registroDosis.fechaHoraProgramada <= :fin
    """)
    fun getTodosRegistrosConMedicamento(inicio: Long, fin: Long): Flow<Map<RegistroDosis, Medicamento>>

    @Query("UPDATE registroDosis SET estado = :nuevoEstado, accion = :nuevaAccion, fechaHoraTomada = :fechaTomada WHERE idRegistro = :id")
    suspend fun actualizarEstado(id: Int, nuevoEstado: String, nuevaAccion: String, fechaTomada: Long)

    @Query("""
        SELECT COUNT(*) FROM registroDosis 
        WHERE idMedicamento = :medId AND idHorario = :horarioId
        AND fechaHoraProgramada >= :inicioDia AND fechaHoraProgramada <= :finDia
    """)
    suspend fun contarRegistrosDia(medId: Int, horarioId: Int, inicioDia: Long, finDia: Long): Int

    @Query("DELETE FROM registroDosis WHERE idMedicamento = :medId AND estado = 'Pendiente'")
    suspend fun eliminarRegistrosPendientesDeMedicamento(medId: Int)

    @Query("DELETE FROM registroDosis")
    suspend fun eliminarTodos()
}
