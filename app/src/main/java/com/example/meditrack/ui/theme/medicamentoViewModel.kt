package com.example.meditrack.ui.theme

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.meditrack.data.ApiService
import com.example.meditrack.data.Medicamento
import com.example.meditrack.data.MedicamentoDTO
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

import androidx.lifecycle.viewModelScope
import com.example.meditrack.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.sql.Time
import java.text.SimpleDateFormat
import java.util.*

data class RegistroDosisConInfo(
    val registro: RegistroDosis,
    val medicamento: Medicamento
)

data class AdherenciaMedicamento(
    val medicamento: Medicamento,
    val dosisTomadas: Int,
    val dosisPerdidas: Int,
    val totalDosis: Int,
    val porcentaje: Int,
    val fechaInicio: Long,
    val fechaFin: Long?  // null = sigue en tratamiento
)

class MedicamentoViewModel(
    private val apiService: ApiService,
    private val medicamentoDao: MedicamentoDao,
    private val horarioDao: HorarioDao,
    private val registroDosisDao: RegistroDosisDao
) : ViewModel() {

    companion object {
        private fun getApiService(): ApiService {
            return Retrofit.Builder()
                .baseUrl("https://www.datos.gov.co/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService::class.java)
        }

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as android.app.Application)
                val database = com.example.meditrack.data.AppDatabase.getDatabase(application)
                MedicamentoViewModel(
                    apiService = getApiService(),
                    medicamentoDao = database.medicamentoDao(),
                    horarioDao = database.horarioDao(),
                    registroDosisDao = database.registroDosisDao()
                )
            }
        }

        fun provideFactory(
            apiService: ApiService,
            medicamentoDao: MedicamentoDao,
            horarioDao: HorarioDao,
            registroDosisDao: RegistroDosisDao
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                MedicamentoViewModel(apiService, medicamentoDao, horarioDao, registroDosisDao)
            }
        }
    }

    // Lista de medicamentos guardados
    val todosLosMedicamentos: Flow<List<Medicamento>> = medicamentoDao.getMedicamentosDeUsuario(0)

    // Registros de dosis para el día de hoy (con regeneración automática)
    val registrosHoy: Flow<List<RegistroDosisConInfo>> = flow {
        while (true) {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val inicioDia = calendar.timeInMillis

            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            val finDia = calendar.timeInMillis

            // Regenerar registros del día si hacen falta
            regenerarRegistrosDiarios(inicioDia, finDia)

            emitAll(registroDosisDao.getRegistrosConMedicamentoDelDia(inicioDia, finDia).map { map ->
                map.map { (registro, medicamento) ->
                    RegistroDosisConInfo(registro, medicamento)
                }.sortedBy { it.registro.fechaHoraProgramada }
            })
        }
    }

    /**
     * Retorna todos los registros del mes especificado (año + mes en base 0).
     */
    fun getRegistrosMes(year: Int, month: Int): Flow<List<RegistroDosisConInfo>> {
        val cal = Calendar.getInstance()
        cal.set(year, month, 1, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val inicioMes = cal.timeInMillis

        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val finMes = cal.timeInMillis

        return registroDosisDao.getTodosRegistrosConMedicamento(inicioMes, finMes).map { map ->
            map.map { (registro, medicamento) ->
                RegistroDosisConInfo(registro, medicamento)
            }
        }
    }

    /**
     * Calcula la adherencia por medicamento para el mes especificado.
     */
    fun getAdherenciaPorMedicamento(year: Int, month: Int): Flow<List<AdherenciaMedicamento>> {
        val cal = Calendar.getInstance()
        // Rango muy amplio para obtener TODOS los registros históricos de cada medicamento
        cal.set(2000, 0, 1, 0, 0, 0)
        val inicioTotal = cal.timeInMillis
        cal.set(2100, 11, 31, 23, 59, 59)
        val finTotal = cal.timeInMillis

        return combine(
            registroDosisDao.getTodosRegistrosConMedicamento(inicioTotal, finTotal),
            medicamentoDao.getMedicamentosDeUsuario(0)
        ) { registrosMap, medicamentos ->
            val registrosList = registrosMap.map { (r, m) -> RegistroDosisConInfo(r, m) }

            medicamentos.map { med ->
                val regsMed = registrosList.filter { it.registro.idMedicamento == med.idMedicamento }
                val tomadas = regsMed.count { it.registro.estado == "Tomado" }
                val perdidas = regsMed.count { it.registro.estado == "Perdido" }
                val total = regsMed.size
                val porcentaje = if (total > 0) (tomadas * 100 / total) else 0

                val fechaInicio = med.creadoEn
                val fechaFin = if (med.activo) null else {
                    regsMed.maxOfOrNull { it.registro.fechaHoraProgramada }
                }

                AdherenciaMedicamento(
                    medicamento = med,
                    dosisTomadas = tomadas,
                    dosisPerdidas = perdidas,
                    totalDosis = total,
                    porcentaje = porcentaje,
                    fechaInicio = fechaInicio,
                    fechaFin = fechaFin
                )
            }.filter { it.totalDosis > 0 }
        }
    }

    // Lista de sugerencias que se mostrarán en el formulario
    val sugerencias = mutableStateListOf<Medicamento>()

    // Para mostrar un circulito de carga si la API tarda
    val cargando = mutableStateOf(false)

    /**
     * Regenera los registros del día para todos los medicamentos activos que no tengan
     * registro creado para hoy todavía.
     */
    private suspend fun regenerarRegistrosDiarios(inicioDia: Long, finDia: Long) {
        try {
            val medicamentosActivos = medicamentoDao.getMedicamentosDeUsuario(0).first()
                .filter { it.activo }

            medicamentosActivos.forEach { med ->
                val horarios = horarioDao.getHorariosPorMedicamentoSync(med.idMedicamento)
                horarios.forEach { horario ->
                    val count = registroDosisDao.contarRegistrosDia(
                        med.idMedicamento, horario.idHorario, inicioDia, finDia
                    )
                    if (count == 0) {
                        val calHora = Calendar.getInstance().apply {
                            time = horario.hora
                        }
                        val programada = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, calHora.get(Calendar.HOUR_OF_DAY))
                            set(Calendar.MINUTE, calHora.get(Calendar.MINUTE))
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        registroDosisDao.insertar(
                            RegistroDosis(
                                idMedicamento = med.idMedicamento,
                                idHorario = horario.idHorario,
                                fechaHoraProgramada = programada.timeInMillis,
                                estado = "Pendiente",
                                accion = "Pendiente"
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MedicamentoViewModel", "Error al regenerar registros diarios", e)
        }
    }

    fun actualizarEstadoDosis(idRegistro: Int, nuevoEstado: String, nuevaAccion: String) {
        viewModelScope.launch {
            try {
                registroDosisDao.actualizarEstado(idRegistro, nuevoEstado, nuevaAccion, System.currentTimeMillis())
            } catch (e: Exception) {
                Log.e("MedicamentoViewModel", "Error al actualizar dosis", e)
            }
        }
    }

    fun terminarTratamiento(idMedicamento: Int) {
        viewModelScope.launch {
            try {
                val med = medicamentoDao.getMedicamentoPorId(idMedicamento)
                if (med != null) {
                    medicamentoDao.actualizar(med.copy(activo = false))
                    // Eliminar registros pendientes del día para que no sigan apareciendo
                    registroDosisDao.eliminarRegistrosPendientesDeMedicamento(idMedicamento)
                }
            } catch (e: Exception) {
                Log.e("MedicamentoViewModel", "Error al terminar tratamiento", e)
            }
        }
    }

    fun buscarMedicamentos(query: String) {
        val queryLimpia = query.trim()
        if (queryLimpia.length < 3) {
            sugerencias.clear()
            return
        }

        cargando.value = true
        val queryBusqueda = queryLimpia.uppercase()

        // Busca coincidencias en el nombre del producto Y en la descripción ATC (clase terapéutica)
        val whereClause = "upper(producto) like '%${queryBusqueda}%' OR upper(descripcionatc) like '%${queryBusqueda}%'"

        apiService.getMedicamentos(where = whereClause).enqueue(object : Callback<List<MedicamentoDTO>> {
            override fun onResponse(
                call: Call<List<MedicamentoDTO>>,
                response: Response<List<MedicamentoDTO>>
            ) {
                cargando.value = false
                if (response.isSuccessful) {
                    sugerencias.clear()
                    response.body()?.take(15)?.forEach { dto ->
                        if (sugerencias.none { it.nombreMedicamento == dto.producto }) {
                            sugerencias.add(
                                Medicamento(
                                    nombreMedicamento = dto.producto ?: "Sin nombre",
                                    formaFarmaceutica = dto.formafarmaceutica ?: "N/A",
                                    idUsuario = 0
                                )
                            )
                        }
                    }
                }
            }

            override fun onFailure(call: Call<List<MedicamentoDTO>>, t: Throwable) {
                cargando.value = false
            }
        })
    }

    fun guardarMedicamento(
        nombre: String,
        dosis: Float,
        formaFarmaceutica: String,
        notas: String,
        horariosSeleccionados: List<String>,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                val nuevoMed = Medicamento(
                    nombreMedicamento = nombre,
                    dosisValor = dosis,
                    formaFarmaceutica = formaFarmaceutica,
                    notas = notas,
                    idUsuario = 0
                )

                val medId = medicamentoDao.insertar(nuevoMed).toInt()

                // Determinar el inicio y fin del día actual
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val inicioDia = calendar.timeInMillis
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val finDia = calendar.timeInMillis

                val formatores = listOf(
                    SimpleDateFormat("hh:mm a", Locale.US),
                    SimpleDateFormat("HH:mm", Locale.getDefault()),
                    SimpleDateFormat("hh:mm a", Locale.getDefault())
                )

                horariosSeleccionados.forEach { horaStr ->
                    val normalizedHora = horaStr
                        .replace("a.m.", "AM", ignoreCase = true)
                        .replace("p.m.", "PM", ignoreCase = true)
                        .replace("a. m.", "AM", ignoreCase = true)
                        .replace("p. m.", "PM", ignoreCase = true)
                        .trim()

                    var date: Date? = null
                    for (sdf in formatores) {
                        try {
                            date = sdf.parse(normalizedHora)
                            if (date != null) break
                        } catch (_: Exception) {}
                    }

                    if (date != null) {
                        val calHora = Calendar.getInstance().apply { time = date!! }

                        val horario = Horario(
                            idMedicamento = medId,
                            hora = Time(calHora.get(Calendar.HOUR_OF_DAY).toLong() * 3600000L +
                                        calHora.get(Calendar.MINUTE).toLong() * 60000L)
                        )
                        val horarioId = horarioDao.insertar(horario).toInt()

                        // Crear registro de dosis para hoy
                        val programada = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, calHora.get(Calendar.HOUR_OF_DAY))
                            set(Calendar.MINUTE, calHora.get(Calendar.MINUTE))
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }

                        val yaExiste = registroDosisDao.contarRegistrosDia(medId, horarioId, inicioDia, finDia) > 0
                        if (!yaExiste) {
                            registroDosisDao.insertar(
                                RegistroDosis(
                                    idMedicamento = medId,
                                    idHorario = horarioId,
                                    fechaHoraProgramada = programada.timeInMillis,
                                    estado = "Pendiente",
                                    accion = "Pendiente"
                                )
                            )
                        }
                    } else {
                        Log.w("MedicamentoViewModel", "No se pudo parsear la hora: $horaStr (normalizada: $normalizedHora)")
                    }
                }
                onSuccess()
            } catch (e: Exception) {
                Log.e("MedicamentoViewModel", "Error al guardar", e)
            }
        }
    }

    fun getHorariosPorMedicamento(idMedicamento: Int): Flow<List<Horario>> {
        return horarioDao.getHorariosPorMedicamento(idMedicamento)
    }

    fun editarMedicamento(
        idMedicamento: Int,
        nuevaDosis: Float,
        nuevosHorarios: List<String>,
        nuevasNotas: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                val med = medicamentoDao.getMedicamentoPorId(idMedicamento) ?: return@launch

                medicamentoDao.actualizar(
                    med.copy(
                        dosisValor = nuevaDosis,
                        notas = nuevasNotas
                    )
                )

                horarioDao.desactivarHorariosDeMedicamento(idMedicamento)
                registroDosisDao.eliminarRegistrosPendientesDeMedicamento(idMedicamento)

                val calendar = Calendar.getInstance()
                calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
                val inicioDia = calendar.timeInMillis
                calendar.set(Calendar.HOUR_OF_DAY, 23); calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59); calendar.set(Calendar.MILLISECOND, 999)
                val finDia = calendar.timeInMillis

                val formatores = listOf(
                    SimpleDateFormat("hh:mm a", Locale.US),
                    SimpleDateFormat("HH:mm", Locale.getDefault()),
                    SimpleDateFormat("hh:mm a", Locale.getDefault())
                )

                nuevosHorarios.forEach { horaStr ->
                    val normalizedHora = horaStr
                        .replace("a.m.", "AM", ignoreCase = true)
                        .replace("p.m.", "PM", ignoreCase = true)
                        .replace("a. m.", "AM", ignoreCase = true)
                        .replace("p. m.", "PM", ignoreCase = true)
                        .trim()

                    var date: Date? = null
                    for (sdf in formatores) {
                        try {
                            date = sdf.parse(normalizedHora)
                            if (date != null) break
                        } catch (_: Exception) {}
                    }

                    if (date != null) {
                        val calHora = Calendar.getInstance().apply { time = date!! }

                        val horario = Horario(
                            idMedicamento = idMedicamento,
                            hora = Time(calHora.get(Calendar.HOUR_OF_DAY).toLong() * 3600000L +
                                        calHora.get(Calendar.MINUTE).toLong() * 60000L)
                        )
                        val horarioId = horarioDao.insertar(horario).toInt()

                        val programada = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, calHora.get(Calendar.HOUR_OF_DAY))
                            set(Calendar.MINUTE, calHora.get(Calendar.MINUTE))
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }

                        registroDosisDao.insertar(
                            RegistroDosis(
                                idMedicamento = idMedicamento,
                                idHorario = horarioId,
                                fechaHoraProgramada = programada.timeInMillis,
                                estado = "Pendiente",
                                accion = "Pendiente"
                            )
                        )
                    }
                }

                onSuccess()
            } catch (e: Exception) {
                Log.e("MedicamentoViewModel", "Error al editar medicamento", e)
            }
        }
    }
}
