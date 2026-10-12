package com.example.meditrack.ui.theme.pantallas

import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.meditrack.data.Horario
import com.example.meditrack.ui.theme.MedicamentoViewModel
import com.example.meditrack.ui.theme.colorAzulSecundario
import com.example.meditrack.ui.theme.colorFondoApp
import com.example.meditrack.ui.theme.colorVerdeOscuro
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditarMedicamento(
    idMedicamento: Int,
    navController: NavController,
    onBack: () -> Unit = {},
    viewModel: MedicamentoViewModel = viewModel(factory = MedicamentoViewModel.Factory)
) {
    val context = LocalContext.current
    val colorAzul = colorAzulSecundario
    val colorVerde = colorVerdeOscuro

    // Cargar medicamento
    val medicamentos by viewModel.todosLosMedicamentos.collectAsState(initial = emptyList())
    val med = medicamentos.find { it.idMedicamento == idMedicamento }

    // Estados del formulario
    var dosis by remember(med) { mutableStateOf(med?.dosisValor?.let { if (it == 0f) "" else it.toString() } ?: "") }
    var notas by remember(med) { mutableStateOf(med?.notas ?: "") }
    val horarios = remember { mutableStateListOf<String>() }
    var horariosInicializados by remember { mutableStateOf(false) }

    // Cargar horarios actuales del medicamento
    val horariosFlow = remember(idMedicamento) {
        viewModel.getHorariosPorMedicamento(idMedicamento)
    }
    val horariosActuales by horariosFlow.collectAsState(initial = emptyList())

    // Inicializar la lista de horarios una sola vez
    LaunchedEffect(horariosActuales) {
        if (!horariosInicializados && horariosActuales.isNotEmpty()) {
            horarios.clear()
            horariosActuales.forEach { horario ->
                // horario.hora is java.sql.Time whose getTime() is ms since midnight
                val totalMs = horario.hora.time
                val horas = ((totalMs / 3600000) % 24).toInt()
                val mins = ((totalMs % 3600000) / 60000).toInt()
                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, horas)
                    set(Calendar.MINUTE, mins)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val formatter = SimpleDateFormat("hh:mm a", Locale.US)
                val horaStr = formatter.format(cal.time)
                    .lowercase(Locale.getDefault())
                    .replace("am", "a.m.")
                    .replace("pm", "p.m.")
                horarios.add(horaStr)
            }
            horariosInicializados = true
        } else if (!horariosInicializados && horariosActuales.isEmpty() && medicamentos.isNotEmpty()) {
            // Medicamento existe pero no tiene horarios todavía
            horarios.add("08:00 a.m.")
            horariosInicializados = true
        }
    }

    if (med == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = colorVerde)
        }
        return
    }

    Scaffold(
        containerColor = colorFondoApp,
        topBar = {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = colorAzul)
                    Spacer(Modifier.width(4.dp))
                    Text("Atrás", color = colorAzul, fontSize = 18.sp)
                }
                Text(
                    text = "Editar medicamento",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    text = med.nombreMedicamento,
                    fontSize = 16.sp,
                    color = Color.Gray
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Campo: Dosis
            FormItem(label = "Dosis") {
                OutlinedTextField(
                    value = dosis,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() || it == '.' } && input.count { it == '.' } <= 1) {
                            dosis = input
                        }
                    },
                    placeholder = { Text("0") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    suffix = {
                        if (med.formaFarmaceutica.isNotEmpty()) {
                            Text(
                                text = med.formaFarmaceutica,
                                color = colorAzul,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = textFieldColors()
                )
            }

            // Sección: Horarios
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Horarios (${horarios.size}/4)",
                        fontWeight = FontWeight.SemiBold,
                        color = Color.DarkGray
                    )
                    OutlinedButton(
                        onClick = {
                            if (horarios.size < 4) {
                                horarios.add("12:00 p.m.")
                            } else {
                                Toast.makeText(
                                    context,
                                    "Ya llegó al límite de horarios para este medicamento",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, colorAzul),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp), tint = colorAzul)
                        Text("Añadir", fontSize = 14.sp, color = colorAzul)
                    }
                }

                if (horarios.isEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "No hay horarios. Añade al menos uno.",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }

                horarios.forEachIndexed { index, hora ->
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    val calendar = Calendar.getInstance()
                                    try {
                                        val currentFormat = SimpleDateFormat("hh:mm a", Locale.US)
                                        val normalizedHora = hora
                                            .replace("a.m.", "AM", ignoreCase = true)
                                            .replace("p.m.", "PM", ignoreCase = true)
                                        val date = currentFormat.parse(normalizedHora)
                                        if (date != null) calendar.time = date
                                    } catch (_: Exception) {}

                                    TimePickerDialog(
                                        context,
                                        { _, hourOfDay, minute ->
                                            val cal = Calendar.getInstance()
                                            cal.set(Calendar.HOUR_OF_DAY, hourOfDay)
                                            cal.set(Calendar.MINUTE, minute)
                                            val formatter = SimpleDateFormat("hh:mm a", Locale.US)
                                            horarios[index] = formatter.format(cal.time)
                                                .lowercase(Locale.getDefault())
                                                .replace("am", "a.m.")
                                                .replace("pm", "p.m.")
                                        },
                                        calendar.get(Calendar.HOUR_OF_DAY),
                                        calendar.get(Calendar.MINUTE),
                                        false
                                    ).show()
                                }
                        ) {
                            OutlinedTextField(
                                value = hora,
                                onValueChange = {},
                                readOnly = true,
                                enabled = false,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                trailingIcon = { Icon(Icons.Default.AccessTime, null) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = Color.Black,
                                    disabledBorderColor = Color.LightGray,
                                    disabledTrailingIconColor = Color.DarkGray,
                                    disabledContainerColor = Color.White
                                )
                            )
                        }

                        if (horarios.size > 1) {
                            Spacer(Modifier.width(8.dp))
                            IconButton(
                                onClick = { horarios.removeAt(index) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Quitar horario",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Campo: Notas
            FormItem(label = "Notas (opcional)") {
                OutlinedTextField(
                    value = notas,
                    onValueChange = { notas = it },
                    placeholder = { Text("Ej: Tomar con alimentos", color = Color.LightGray) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = textFieldColors()
                )
            }

            Spacer(Modifier.height(16.dp))

            // Botón Guardar cambios
            Button(
                onClick = {
                    if (horarios.isEmpty()) {
                        Toast.makeText(context, "Agrega al menos un horario", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val dosisFloat = dosis.toFloatOrNull() ?: 0f
                    viewModel.editarMedicamento(
                        idMedicamento = idMedicamento,
                        nuevaDosis = dosisFloat,
                        nuevosHorarios = horarios.toList(),
                        nuevasNotas = notas,
                        onSuccess = {
                            Toast.makeText(context, "Cambios guardados", Toast.LENGTH_SHORT).show()
                            onBack()
                        }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colorVerde)
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Guardar cambios", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            // Botón Cancelar
            Button(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White)
            ) {
                Text("Cancelar", fontSize = 18.sp, color = Color.Black)
            }

            Spacer(Modifier.height(30.dp))
        }
    }
}
