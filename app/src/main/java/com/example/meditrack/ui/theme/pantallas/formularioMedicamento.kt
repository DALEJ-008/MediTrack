package com.example.meditrack.ui.theme.pantallas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.meditrack.ui.theme.MediTrackTheme
import com.example.meditrack.ui.theme.MedicamentoViewModel
import com.example.meditrack.ui.theme.colorAzulSecundario
import com.example.meditrack.ui.theme.colorFondoApp
import com.example.meditrack.ui.theme.colorVerdeOscuro

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormularioMedicamento(
    navController: NavController,
    onBack: () -> Unit = {},
    onSave: () -> Unit = {},
    viewModel: MedicamentoViewModel = viewModel(factory = MedicamentoViewModel.Factory),
) {
    val context = LocalContext.current
    var dosis by remember { mutableStateOf("") }
    var formaFarmaceutica by remember { mutableStateOf("") }
    var notas by remember { mutableStateOf("") }
    val horarios = remember { mutableStateListOf("08:00 a.m.") }
    var nombreTexto by remember { mutableStateOf("") }
    var mostrarSugerencias by remember { mutableStateOf(false) }

    val colorFondo = colorFondoApp
    val colorAzul = colorAzulSecundario
    val colorVerde = colorVerdeOscuro

    Scaffold(
        containerColor = colorFondo,
        topBar = {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = colorAzul)
                    Spacer(Modifier.width(4.dp))
                    Text("Atrás", color = colorAzul, fontSize = 18.sp)
                }
                Text(
                    text = "Nuevo medicamento",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
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
            // Campo: Nombre
            FormItem(label = "Nombre del medicamento") {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = nombreTexto,
                        onValueChange = {
                            nombreTexto = it
                            mostrarSugerencias = true
                            viewModel.buscarMedicamentos(it)
                        },
                        placeholder = { Text("Ej: Paracetamol") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                            if (viewModel.cargando.value) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp,
                                    color = colorAzul
                                )
                            } else if (nombreTexto.isNotEmpty()) {
                                IconButton(onClick = {
                                    nombreTexto = ""
                                    viewModel.sugerencias.clear()
                                }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                                }
                            }
                        },
                        colors = textFieldColors()
                    )

                    if (mostrarSugerencias && viewModel.sugerencias.isNotEmpty()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 250.dp)
                                .padding(top = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White,
                            shadowElevation = 4.dp,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                viewModel.sugerencias.forEach { medicamento ->
                                    ListItem(
                                        headlineContent = {
                                            Text(
                                                text = medicamento.nombreMedicamento,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                        },
                                        supportingContent = {
                                            Text(
                                                text = medicamento.formaFarmaceutica,
                                                fontSize = 12.sp,
                                                color = Color.Gray
                                            )
                                        },
                                        modifier = Modifier.clickable {
                                            nombreTexto = medicamento.nombreMedicamento
                                            formaFarmaceutica = medicamento.formaFarmaceutica
                                            mostrarSugerencias = false
                                            viewModel.sugerencias.clear()
                                        }
                                    )
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        color = Color.LightGray.copy(alpha = 0.3f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

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
                        if (formaFarmaceutica.isNotEmpty()) {
                            Text(
                                text = formaFarmaceutica,
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

                        // Botón X para eliminar horario (solo si hay más de uno)
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

            // Botón Guardar
            Button(
                onClick = {
                    if (nombreTexto.isBlank()) {
                        Toast.makeText(context, "Por favor ingrese el nombre del medicamento", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val dosisFloat = dosis.toFloatOrNull() ?: 0f
                    viewModel.guardarMedicamento(
                        nombre = nombreTexto,
                        dosis = dosisFloat,
                        formaFarmaceutica = formaFarmaceutica,
                        notas = notas,
                        horariosSeleccionados = horarios.toList(),
                        onSuccess = {
                            onSave()
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
                Text("Guardar", fontSize = 18.sp, fontWeight = FontWeight.Bold)
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

@Composable
fun FormItem(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = label, fontWeight = FontWeight.SemiBold, color = Color.DarkGray)
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    focusedBorderColor = Color.LightGray,
    unfocusedBorderColor = Color.LightGray,
)

@Preview(showBackground = true)
@Composable
fun PreviewFormulario() {
    MediTrackTheme {
        FormularioMedicamento(navController = NavController(LocalContext.current))
    }
}
