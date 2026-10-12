package com.example.meditrack.ui.theme.pantallas

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.meditrack.R
import com.example.meditrack.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Locale

enum class TipoEstadoMedicamento {
    TOMADO, PENDIENTE, PERDIDO
}

@Composable
fun PantallaPrincipal(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: MedicamentoViewModel = viewModel(factory = MedicamentoViewModel.Factory)
) {
    val registrosHoy by viewModel.registrosHoy.collectAsState(initial = emptyList())
    val totalDosis = registrosHoy.size
    val dosisTomadas = registrosHoy.count { it.registro.estado == "Tomado" }

    var mostrarDialogoTerminar by remember { mutableStateOf(false) }
    var medicamentoSeleccionadoId by remember { mutableStateOf<Int?>(null) }
    var nombreMedicamentoSeleccionado by remember { mutableStateOf("") }

    if (mostrarDialogoTerminar) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoTerminar = false },
            title = { 
                Text(
                    text = "¿Terminar tratamiento?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ) 
            },
            text = { 
                Text(
                    text = "¿Estás seguro de que deseas quitar el medicamento \"$nombreMedicamentoSeleccionado\" de la pantalla de inicio? Se marcará como tratamiento finalizado.",
                    fontSize = 16.sp
                ) 
            },
            confirmButton = {
                Button(
                    onClick = {
                        medicamentoSeleccionadoId?.let { viewModel.terminarTratamiento(it) }
                        mostrarDialogoTerminar = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colorVerdeOscuro),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Aceptar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { mostrarDialogoTerminar = false }
                ) {
                    Text("Cancelar", color = Color.Gray, fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = Color.White
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colorFondoApp,
        bottomBar = {
            MediTrackBottomNavigation(navController = navController)
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = stringResource(id = R.string.buenos_dias),
                    style = MaterialTheme.typography.labelLarge.copy(
                        letterSpacing = 1.sp
                    ),
                    color = Color.Black.copy(alpha = 0.6f)
                )
                Text(
                    text = SimpleDateFormat("EEEE, d 'de' MMMM", Locale.getDefault()).format(java.util.Date()),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp
                    ),
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                if (totalDosis > 0) {
                    val summaryText = buildAnnotatedString {
                        append("Has tomado ")
                        withStyle(style = SpanStyle(color = colorVerdeOscuro, fontWeight = FontWeight.Bold)) {
                            append("$dosisTomadas")
                        }
                        append(" de $totalDosis medicamentos hoy")
                    }
                    
                    Text(
                        text = summaryText,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            lineHeight = 24.sp
                        ),
                        color = Color.Black.copy(alpha = 0.7f)
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))

                if (registrosHoy.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_clock),
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = Color.Gray.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No hay medicamentos registrados para hoy.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Haz clic en el botón de agregar para registrar uno.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        registrosHoy.forEach { info ->
                            val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
                            val horaStr = sdf.format(java.util.Date(info.registro.fechaHoraProgramada))
                            
                            val tipoEstado = when (info.registro.estado) {
                                "Tomado" -> TipoEstadoMedicamento.TOMADO
                                "Perdido" -> TipoEstadoMedicamento.PERDIDO
                                else -> TipoEstadoMedicamento.PENDIENTE
                            }

                            MedicamentoCard(
                                nombre = info.medicamento.nombreMedicamento,
                                dosis = "${info.medicamento.dosisValor} ${info.medicamento.formaFarmaceutica}",
                                hora = horaStr,
                                estado = info.registro.estado,
                                tipoEstado = tipoEstado,
                                onTomar = {
                                    viewModel.actualizarEstadoDosis(info.registro.idRegistro, "Tomado", "Tomado a tiempo")
                                },
                                onSaltar = {
                                    viewModel.actualizarEstadoDosis(info.registro.idRegistro, "Perdido", "Saltado por usuario")
                                },
                                onClickCard = {
                                    nombreMedicamentoSeleccionado = info.medicamento.nombreMedicamento
                                    medicamentoSeleccionadoId = info.medicamento.idMedicamento
                                    mostrarDialogoTerminar = true
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(80.dp)) // Espacio para el botón flotante
                    }
                }
            }

            Button(
                onClick = { navController.navigate("formularioMedicamento") },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 24.dp, bottom = 24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colorVerdeOscuro),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_add),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(id = R.string.agregar),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun MedicamentoCard(
    nombre: String,
    dosis: String,
    hora: String,
    estado: String,
    tipoEstado: TipoEstadoMedicamento,
    onTomar: () -> Unit = {},
    onSaltar: () -> Unit = {},
    onClickCard: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val containerColor = when (tipoEstado) {
        TipoEstadoMedicamento.TOMADO -> colorVerdeClaro
        TipoEstadoMedicamento.PENDIENTE -> Color.White
        TipoEstadoMedicamento.PERDIDO -> colorRojoClaroPerdido
    }

    val contentColor = when (tipoEstado) {
        TipoEstadoMedicamento.TOMADO -> colorVerdeOscuro
        TipoEstadoMedicamento.PENDIENTE -> colorNaranjaPendiente
        TipoEstadoMedicamento.PERDIDO -> colorRojoPerdido
    }

    Box(modifier = modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = { expanded = true },
                        onTap = { expanded = true }
                    )
                },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = containerColor),
            border = if (tipoEstado == TipoEstadoMedicamento.PENDIENTE) 
                CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color.Black.copy(alpha = 0.1f))) 
                else null
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Nombre del medicamento (ocupa toda la línea si es largo)
                Text(
                    text = nombre,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    color = Color.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(2.dp))
                // Dosis y chip de estado en la misma fila
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = dosis,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Black.copy(alpha = 0.6f),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = when (tipoEstado) {
                            TipoEstadoMedicamento.TOMADO -> Color.Black.copy(alpha = 0.05f)
                            TipoEstadoMedicamento.PENDIENTE -> colorFondoNaranja
                            TipoEstadoMedicamento.PERDIDO -> Color.Black.copy(alpha = 0.05f)
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = estado,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            color = contentColor,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_clock),
                        contentDescription = null,
                        tint = colorAzulSecundario,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = hora,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.Black
                    )
                }
                
                if (tipoEstado != TipoEstadoMedicamento.PENDIENTE) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val iconId = if (tipoEstado == TipoEstadoMedicamento.TOMADO) R.drawable.ic_check else R.drawable.ic_lost
                        Icon(
                            painter = painterResource(id = iconId),
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = estado,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = contentColor
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = onTomar,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = colorVerdeOscuro),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_check),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(id = R.string.tomar_ahora),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        OutlinedButton(
                            onClick = onSaltar,
                            modifier = Modifier.weight(0.6f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFFF5F5F5)),
                            border = null,
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_skip),
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(id = R.string.saltar),
                                color = Color.Black,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color.White)
        ) {
            DropdownMenuItem(
                text = { Text("Ya terminé mi tratamiento") },
                onClick = {
                    expanded = false
                    onClickCard()
                },
                leadingIcon = { 
                    Icon(
                        painter = painterResource(id = R.drawable.ic_check), 
                        contentDescription = null, 
                        tint = colorVerdeOscuro,
                        modifier = Modifier.size(20.dp)
                    ) 
                }
            )
        }
    }
}

@Composable
fun MediTrackBottomNavigation(navController: NavController, modifier: Modifier = Modifier) {
    NavigationBar(
        modifier = modifier,
        containerColor = Color.White,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            selected = true,
            onClick = { },
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_home),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp)
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
            selected = false,
            onClick = { navController.navigate("pantallaAjustes") },
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_settings),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
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
fun PantallaPrincipalPreview() {
    MediTrackTheme {
        PantallaPrincipal(navController = NavController(LocalContext.current))
    }
}
