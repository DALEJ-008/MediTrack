package com.example.meditrack

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.meditrack.ui.theme.MediTrackTheme
import com.example.meditrack.ui.theme.pantallas.pantallaInicioSesion
import com.example.meditrack.ui.theme.pantallas.PantallaRegistro
import androidx.compose.material3.Text
import com.example.meditrack.ui.theme.pantallas.FormularioMedicamento
import com.example.meditrack.ui.theme.pantallas.PantallaPrincipal
import com.example.meditrack.ui.theme.pantallas.PantallaHistorial
import com.example.meditrack.ui.theme.pantallas.PantallaAjustes
import com.example.meditrack.ui.theme.pantallas.EditarMedicamento

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Permission result - no action needed */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Create notification channel on startup
        NotificationHelper.createNotificationChannel(this)

        // Request POST_NOTIFICATIONS permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            MediTrackTheme {
                MediTrackApp()
            }
        }
    }
}

@Composable
fun MediTrackApp() {
    val navController = rememberNavController()
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("meditrack_prefs", android.content.Context.MODE_PRIVATE) }
    val haySesionGuardada = remember { prefs.getInt("sesion_usuario_id", -1) != -1 }
    val destino = if (haySesionGuardada) "inicio" else "inicioSesión"

    NavHost(navController = navController, startDestination = destino) {
        composable("inicioSesión") {
            pantallaInicioSesion(navController)
        }
        composable("registro") {
            PantallaRegistro(navController)
        }
        composable("inicio") {
            PantallaPrincipal(navController)
        }
        composable("formularioMedicamento") {
            FormularioMedicamento(
                navController = navController,
                onBack = { navController.popBackStack() },
                onSave = {
                    navController.popBackStack()
                }
            )
        }
        composable("pantallaHistorial") {
            PantallaHistorial(navController)
        }
        composable("pantallaAjustes") {
            PantallaAjustes(navController)
        }
        composable("editarMedicamento/{idMedicamento}") { backStackEntry ->
            val idMedicamento = backStackEntry.arguments?.getString("idMedicamento")?.toIntOrNull() ?: return@composable
            EditarMedicamento(
                idMedicamento = idMedicamento,
                navController = navController,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
fun PantallaPrincipalTemporal() {
    Text(text = "¡Bienvenido a MediTrack!")
}
