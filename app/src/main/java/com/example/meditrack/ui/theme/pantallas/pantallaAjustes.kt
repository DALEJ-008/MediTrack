package com.example.meditrack.ui.theme.pantallas

import android.app.TimePickerDialog
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.meditrack.ui.theme.MedicamentoViewModel
import com.example.meditrack.ui.theme.colorFondoNaranja
import com.example.meditrack.ui.theme.colorNaranjaPendiente
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.meditrack.NotificationHelper
import com.example.meditrack.R
import com.example.meditrack.ui.theme.MediTrackTheme
import com.example.meditrack.ui.theme.UsuarioViewModel
import com.example.meditrack.ui.theme.colorAzulSecundario
import com.example.meditrack.ui.theme.colorFondoApp
import com.example.meditrack.ui.theme.colorVerdeOscuro
import java.util.Calendar

@Composable
fun PantallaAjustes(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: MedicamentoViewModel = viewModel(factory = MedicamentoViewModel.Factory),
    usuarioViewModel: UsuarioViewModel = viewModel()
) {
    val context = LocalContext.current
    val medicamentos by viewModel.todosLosMedicamentos.collectAsState(initial = emptyList())

    val notificacionesActivas by usuarioViewModel.notificacionesActivas.collectAsState()
    val modoNoMolestar by usuarioViewModel.modoNoMolestar.collectAsState()
    val noMolestarDesde by usuarioViewModel.noMolestarDesde.collectAsState()
    val noMolestarHasta by usuarioViewModel.noMolestarHasta.collectAsState()

    var mostrarDialogoEliminar by remember { mutableStateOf(false) }
    var mostrarDialogoCerrarSesion by remember { mutableStateOf(false) }

    // Confirmation dialog: Delete all data
    if (mostrarDialogoEliminar) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoEliminar = false },
            title = { Text("Eliminar todos los datos", fontWeight = FontWeight.Bold) },
            text = { Text("¿Estás seguro de que deseas eliminar todos los datos? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        mostrarDialogoEliminar = false
                        usuarioViewModel.eliminarTodosLosDatos()
                        NotificationHelper.cancelAllAlarms(context)
                        navController.navigate("inicioSesión") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                ) {
                    Text("Aceptar", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoEliminar = false }) {
                    Text("Cancelar")
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Confirmation dialog: Sign out
    if (mostrarDialogoCerrarSesion) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoCerrarSesion = false },
            title = { Text("Cerrar sesión", fontWeight = FontWeight.Bold) },
            text = { Text("¿Deseas cerrar tu sesión?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        mostrarDialogoCerrarSesion = false
                        usuarioViewModel.cerrarSesion()
                        navController.navigate("inicioSesión") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                ) {
                    Text("Aceptar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoCerrarSesion = false }) {
                    Text("Cancelar")
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colorFondoApp,
        bottomBar = {
            BottomNavigationBarAjustes(navController = navController)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(id = R.string.ajustes),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Notificaciones
            SettingsSection {
                HeaderItem(
                    icon = R.drawable.ic_notifications,
                    title = stringResource(id = R.string.notificaciones)
                )
                Spacer(modifier = Modifier.height(8.dp))
                SwitchItem(
                    title = stringResource(id = R.string.activar_recordatorios),
                    subtitle = stringResource(id = R.string.avisos_recordatorios),
                    checked = notificacionesActivas,
                    onCheckedChange = { activas ->
                        usuarioViewModel.setNotificacionesActivas(activas)
                        if (activas) {
                            NotificationHelper.createNotificationChannel(context)
                            NotificationHelper.scheduleAlarms(context)
                        } else {
                            NotificationHelper.cancelAllAlarms(context)
                        }
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
                DropdownItem(
                    label = stringResource(id = R.string.volumen),
                    value = stringResource(id = R.string.medio)
                )
            }

            // No molestar
            SettingsSection {
                HeaderItem(
                    icon = R.drawable.ic_do_not_disturb,
                    title = stringResource(id = R.string.no_molestar)
                )
                Spacer(modifier = Modifier.height(8.dp))
                SwitchItem(
                    title = stringResource(id = R.string.activar_horario_silencioso),
                    checked = modoNoMolestar,
                    onCheckedChange = { activo ->
                        usuarioViewModel.setModoNoMolestar(activo)
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // "Desde" time picker
                    val desdeParts = noMolestarDesde.split(":")
                    val desdeHora = desdeParts[0].toIntOrNull() ?: 22
                    val desdeMin = desdeParts[1].toIntOrNull() ?: 0

                    TimePickerFieldInteractive(
                        label = stringResource(id = R.string.desde),
                        time = formatarHora24a12(noMolestarDesde),
                        modifier = Modifier.weight(1f),
                        onTimeSelected = { h, m ->
                            usuarioViewModel.setNoMolestarDesde(
                                "%02d:%02d".format(h, m)
                            )
                        },
                        initialHour = desdeHora,
                        initialMinute = desdeMin
                    )

                    // "Hasta" time picker
                    val hastaParts = noMolestarHasta.split(":")
                    val hastaHora = hastaParts[0].toIntOrNull() ?: 8
                    val hastaMin = hastaParts[1].toIntOrNull() ?: 0

                    TimePickerFieldInteractive(
                        label = stringResource(id = R.string.hasta),
                        time = formatarHora24a12(noMolestarHasta),
                        modifier = Modifier.weight(1f),
                        onTimeSelected = { h, m ->
                            usuarioViewModel.setNoMolestarHasta(
                                "%02d:%02d".format(h, m)
                            )
                        },
                        initialHour = hastaHora,
                        initialMinute = hastaMin
                    )
                }
            }

            // Mis medicamentos
            SettingsSection {
                HeaderItem(
                    icon = R.drawable.ic_add,
                    title = stringResource(id = R.string.mis_medicamentos),
                    iconColor = colorAzulSecundario
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (medicamentos.isEmpty()) {
                    Text(
                        text = "No tienes medicamentos registrados.",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    medicamentos.forEach { med ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clickable {
                                    if (med.activo) {
                                        navController.navigate("editarMedicamento/${med.idMedicamento}")
                                    }
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = med.nombreMedicamento,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = if (med.activo) Color.Black else Color.Gray
                                )
                                Text(
                                    text = "${med.dosisValor} ${med.formaFarmaceutica}",
                                    color = Color.Gray,
                                    fontSize = 14.sp
                                )
                            }
                            if (!med.activo) {
                                Surface(
                                    color = colorFondoNaranja,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    Text(
                                        text = "Finalizado",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        color = colorNaranjaPendiente,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Icon(
                                painter = painterResource(id = R.drawable.ic_chevron_right),
                                contentDescription = null,
                                tint = Color.Black
                            )
                        }
                        if (med != medicamentos.last()) {
                            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f), thickness = 0.5.dp)
                        }
                    }
                }
            }

            // Acerca de
            SettingsSection {
                HeaderItem(
                    icon = R.drawable.ic_info,
                    title = stringResource(id = R.string.acerca_de)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(id = R.string.descripcion_app),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = Color.Gray
                )
            }

            // Cerrar sesión
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, colorAzulSecundario, RoundedCornerShape(12.dp))
                    .clickable { mostrarDialogoCerrarSesion = true },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_account_box),
                        contentDescription = null,
                        tint = colorAzulSecundario,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Cerrar sesión",
                        color = colorAzulSecundario,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Eliminar datos
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.Red, RoundedCornerShape(12.dp))
                    .clickable { mostrarDialogoEliminar = true },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_delete),
                        contentDescription = null,
                        tint = Color.Red,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(id = R.string.eliminar_todos_los_datos),
                        color = Color.Red,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

/** Convert "HH:mm" 24h string to "hh:mm a.m./p.m." display */
fun formatarHora24a12(hora24: String): String {
    return try {
        val parts = hora24.split(":")
        val h = parts[0].toInt()
        val m = parts[1].toInt()
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, h)
            set(java.util.Calendar.MINUTE, m)
        }
        val sdf = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.US)
        sdf.format(cal.time)
            .lowercase()
            .replace("am", "a.m.")
            .replace("pm", "p.m.")
    } catch (e: Exception) {
        hora24
    }
}

/** Time picker field that opens Android TimePickerDialog on click */
@Composable
fun TimePickerFieldInteractive(
    label: String,
    time: String,
    modifier: Modifier = Modifier,
    onTimeSelected: (Int, Int) -> Unit,
    initialHour: Int = 8,
    initialMinute: Int = 0
) {
    val context = LocalContext.current
    Column(modifier = modifier) {
        Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.Black)
        Spacer(modifier = Modifier.height(4.dp))
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.Black, RoundedCornerShape(20.dp))
                .clickable {
                    TimePickerDialog(
                        context,
                        { _, hour, minute -> onTimeSelected(hour, minute) },
                        initialHour,
                        initialMinute,
                        false
                    ).show()
                },
            shape = RoundedCornerShape(20.dp),
            color = Color.White
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = time, fontSize = 14.sp)
                Icon(
                    painter = painterResource(id = R.drawable.ic_clock),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Color.Black
                )
            }
        }
    }
}

@Composable
fun SettingsSection(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun HeaderItem(
    icon: Int,
    title: String,
    iconColor: Color = colorAzulSecundario
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
    }
}

@Composable
fun SwitchItem(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            if (subtitle != null) {
                Text(text = subtitle, fontSize = 12.sp, color = Color.Gray)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = colorVerdeOscuro,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color.LightGray,
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}

@Composable
fun DropdownItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        Surface(
            modifier = Modifier.border(1.dp, Color.Gray, RoundedCornerShape(8.dp)),
            shape = RoundedCornerShape(8.dp),
            color = Color.White
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = value, fontSize = 14.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    painter = painterResource(id = R.drawable.ic_chevron_right),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun TimePickerField(label: String, time: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.Black)
        Spacer(modifier = Modifier.height(4.dp))
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.Black, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            color = Color.White
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = time, fontSize = 14.sp)
                Icon(
                    painter = painterResource(id = R.drawable.ic_clock),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Color.Black
                )
            }
        }
    }
}

@Composable
fun BottomNavigationBarAjustes(navController: NavController) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            selected = false,
            onClick = { navController.navigate("inicio") },
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_home),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            },
            label = { Text(stringResource(id = R.string.inicio)) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = colorVerdeOscuro,
                selectedTextColor = colorVerdeOscuro,
                unselectedIconColor = colorAzulSecundario,
                unselectedTextColor = colorAzulSecundario,
                indicatorColor = Color.Transparent
            )
        )
        NavigationBarItem(
            selected = false,
            onClick = { navController.navigate("pantallaHistorial") },
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_calendar),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            },
            label = { Text(stringResource(id = R.string.historial)) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = colorVerdeOscuro,
                selectedTextColor = colorVerdeOscuro,
                unselectedIconColor = colorAzulSecundario,
                unselectedTextColor = colorAzulSecundario,
                indicatorColor = Color.Transparent
            )
        )
        NavigationBarItem(
            selected = true,
            onClick = { },
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_settings),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp)
                )
            },
            label = { Text(stringResource(id = R.string.ajustes)) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = colorVerdeOscuro,
                selectedTextColor = colorVerdeOscuro,
                unselectedIconColor = colorAzulSecundario,
                unselectedTextColor = colorAzulSecundario,
                indicatorColor = Color.Transparent
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PantallaAjustesPreview() {
    MediTrackTheme {
        PantallaAjustes(navController = NavController(LocalContext.current))
    }
}
