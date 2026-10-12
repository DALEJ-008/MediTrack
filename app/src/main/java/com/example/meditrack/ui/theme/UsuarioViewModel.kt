package com.example.meditrack.ui.theme

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.meditrack.data.AppDatabase
import com.example.meditrack.data.Usuario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class UsuarioViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val usuarioDao = db.usuarioDao()
    private val medicamentoDao = db.medicamentoDao()
    private val horarioDao = db.horarioDao()
    private val registroDosisDao = db.registroDosisDao()

    private val prefs = application.getSharedPreferences("meditrack_prefs", Context.MODE_PRIVATE)

    private val _registroExitoso = MutableStateFlow(false)
    val registroExitoso: StateFlow<Boolean> = _registroExitoso

    private val _usuarioLogueado = MutableStateFlow<Usuario?>(null)
    val usuarioLogueado: StateFlow<Usuario?> = _usuarioLogueado

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        // Restaurar sesión guardada al arrancar la app
        val idGuardado = prefs.getInt("sesion_usuario_id", -1)
        if (idGuardado != -1) {
            viewModelScope.launch {
                try {
                    val usuario = usuarioDao.obtenerUsuarioPorId(idGuardado)
                    if (usuario != null) {
                        _usuarioLogueado.value = usuario
                    } else {
                        // Usuario ya no existe, limpiar sesión guardada
                        prefs.edit().remove("sesion_usuario_id").apply()
                    }
                } catch (e: Exception) {
                    prefs.edit().remove("sesion_usuario_id").apply()
                }
            }
        }
    }

    // Notification settings (persisted in SharedPreferences)
    private val _notificacionesActivas = MutableStateFlow(
        prefs.getBoolean("notificaciones_activas", false)
    )
    val notificacionesActivas: StateFlow<Boolean> = _notificacionesActivas

    private val _modoNoMolestar = MutableStateFlow(
        prefs.getBoolean("modo_no_molestar", false)
    )
    val modoNoMolestar: StateFlow<Boolean> = _modoNoMolestar

    private val _noMolestarDesde = MutableStateFlow(
        prefs.getString("no_molestar_desde", "22:00") ?: "22:00"
    )
    val noMolestarDesde: StateFlow<String> = _noMolestarDesde

    private val _noMolestarHasta = MutableStateFlow(
        prefs.getString("no_molestar_hasta", "08:00") ?: "08:00"
    )
    val noMolestarHasta: StateFlow<String> = _noMolestarHasta

    fun registrar(nombre: String, correo: String, contrasena: String) {
        viewModelScope.launch {
            try {
                val existe = usuarioDao.obtenerUsuarioPorCorreo(correo)
                if (existe != null) {
                    _error.value = "El correo ya está registrado"
                    return@launch
                }
                val nuevoUsuario = Usuario(
                    idUsuario = 0,
                    nombreUsuario = nombre,
                    correo = correo,
                    contrasenaHash = contrasena
                )
                usuarioDao.registrar(nuevoUsuario)
                _registroExitoso.value = true
                _error.value = null
            } catch (e: Exception) {
                _error.value = "Error al registrar: ${e.message}"
            }
        }
    }

    fun login(correo: String, contrasena: String) {
        viewModelScope.launch {
            try {
                val usuario = usuarioDao.login(correo, contrasena)
                if (usuario != null) {
                    _usuarioLogueado.value = usuario
                    _error.value = null
                    // Persistir sesión para que no se cierre al salir de la app
                    prefs.edit().putInt("sesion_usuario_id", usuario.idUsuario).apply()
                } else {
                    _error.value = "Correo o contraseña incorrectos"
                }
            } catch (e: Exception) {
                _error.value = "Error al iniciar sesión: ${e.message}"
            }
        }
    }

    fun cerrarSesion() {
        _usuarioLogueado.value = null
        _error.value = null
        _registroExitoso.value = false
        // Borrar sesión guardada para que la próxima apertura pida login
        prefs.edit().remove("sesion_usuario_id").apply()
    }

    fun eliminarTodosLosDatos() {
        viewModelScope.launch {
            try {
                registroDosisDao.eliminarTodos()
                horarioDao.eliminarTodos()
                medicamentoDao.eliminarTodos()
                usuarioDao.eliminarTodos()
                // Reset notification prefs
                prefs.edit().clear().apply()
                _notificacionesActivas.value = false
                _modoNoMolestar.value = false
                _noMolestarDesde.value = "22:00"
                _noMolestarHasta.value = "08:00"
                _usuarioLogueado.value = null
            } catch (e: Exception) {
                _error.value = "Error al eliminar datos: ${e.message}"
            }
        }
    }

    fun setNotificacionesActivas(activas: Boolean) {
        _notificacionesActivas.value = activas
        prefs.edit().putBoolean("notificaciones_activas", activas).apply()
    }

    fun setModoNoMolestar(activo: Boolean) {
        _modoNoMolestar.value = activo
        prefs.edit().putBoolean("modo_no_molestar", activo).apply()
    }

    fun setNoMolestarDesde(hora: String) {
        _noMolestarDesde.value = hora
        prefs.edit().putString("no_molestar_desde", hora).apply()
    }

    fun setNoMolestarHasta(hora: String) {
        _noMolestarHasta.value = hora
        prefs.edit().putString("no_molestar_hasta", hora).apply()
    }

    fun limpiarError() {
        _error.value = null
    }
}
