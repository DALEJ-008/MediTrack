package com.example.meditrack.ui.theme.pantallas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.meditrack.ui.theme.AdherenciaMedicamento
import com.example.meditrack.ui.theme.MedicamentoViewModel
import com.example.meditrack.ui.theme.RegistroDosisConInfo
import com.example.meditrack.ui.theme.colorFondoNaranja
import com.example.meditrack.ui.theme.colorNaranjaPendiente
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.example.meditrack.R
import com.example.meditrack.ui.theme.MediTrackTheme
import com.example.meditrack.ui.theme.colorAzulSecundario
import com.example.meditrack.ui.theme.colorFondoApp
import com.example.meditrack.ui.theme.colorVerdeOscuro
import java.text.SimpleDateFormat
import java.util.*

// ─── Colores estado calendario ───────────────────────────────────────────────
private val colorCalVerde   = Color(0xFF34A853)
private val colorCalNaranja = Color(0xFFF2994A)
private val colorCalRojo    = Color(0xFFEB5757)
private val colorCalHoy     = Color(0xFF157FFB)   // azul contorno
private val colorCalVacio   = Color(0xFFE0E0E0)

/** Estado del día para colorear la celda del calendario */
enum class EstadoDia { COMPLETO, PARCIAL, NINGUNO, SIN_REGISTROS, HOY_ACTIVO }

@Composable
fun PantallaHistorial(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: MedicamentoViewModel = viewModel(factory = MedicamentoViewModel.Factory)
) {
    val hoy = Calendar.getInstance()
    var anioVisto  by remember { mutableStateOf(hoy.get(Calendar.YEAR)) }
    var mesVisto   by remember { mutableStateOf(hoy.get(Calendar.MONTH)) }

    val registrosMes by viewModel.getRegistrosMes(anioVisto, mesVisto)
        .collectAsState(initial = emptyList())

    val adherenciaMeds by viewModel.getAdherenciaPorMedicamento(anioVisto, mesVisto)
        .collectAsState(initial = emptyList())

    // Stats del mes completo
    val dosisTomadas  = registrosMes.count { it.registro.estado == "Tomado" }
    val totalDosis    = registrosMes.size
    val dosisPerdidas = registrosMes.count { it.registro.estado == "Perdido" }
    val adherencia    = if (totalDosis > 0) (dosisTomadas * 100 / totalDosis) else 0

    // Estado para la pantalla de detalle de medicamento
    var medicamentoDetalle by remember { mutableStateOf<AdherenciaMedicamento?>(null) }

    if (medicamentoDetalle != null) {
        PantallaDetalleMedicamento(
            adherencia = medicamentoDetalle!!,
            onBack = { medicamentoDetalle = null }
        )
        return
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colorFondoApp,
        bottomBar = { BottomNavigationBar(navController = navController) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(id = R.string.historial),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(24.dp))

            CalendarCard(
                anio        = anioVisto,
                mes         = mesVisto,
                registros   = registrosMes,
                hoy         = hoy,
                onMesAnterior = {
                    if (mesVisto == 0) { mesVisto = 11; anioVisto-- }
                    else mesVisto--
                },
                onMesSiguiente = {
                    val esHoy = anioVisto == hoy.get(Calendar.YEAR) && mesVisto == hoy.get(Calendar.MONTH)
                    if (!esHoy) {
                        if (mesVisto == 11) { mesVisto = 0; anioVisto++ }
                        else mesVisto++
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
            LegendCard()
            Spacer(modifier = Modifier.height(16.dp))

            SummaryStatsRow(
                adherencia = "$adherencia%",
                tomadas    = "$dosisTomadas/$totalDosis",
                perdidas   = "$dosisPerdidas"
            )

            Spacer(modifier = Modifier.height(16.dp))

            AdherenceByMedicationCard(
                adherencias = adherenciaMeds,
                onMedicamentoClick = { medicamentoDetalle = it }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ─── Calendario dinámico ─────────────────────────────────────────────────────

@Composable
fun CalendarCard(
    anio: Int,
    mes: Int,
    registros: List<RegistroDosisConInfo>,
    hoy: Calendar,
    onMesAnterior: () -> Unit,
    onMesSiguiente: () -> Unit,
    modifier: Modifier = Modifier
) {
    val nombresMes = listOf(
        "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
        "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
    )

    // Calcular estado por día del mes
    val estadosPorDia: Map<Int, EstadoDia> = remember(anio, mes, registros, hoy) {
        val calHoy = Calendar.getInstance()
        val esHoyMismo = (anio == calHoy.get(Calendar.YEAR) && mes == calHoy.get(Calendar.MONTH))

        val mapa = mutableMapOf<Int, EstadoDia>()
        val regsPorDia = registros.groupBy {
            val c = Calendar.getInstance().apply { timeInMillis = it.registro.fechaHoraProgramada }
            c.get(Calendar.DAY_OF_MONTH)
        }

        val calMes = Calendar.getInstance().apply { set(anio, mes, 1) }
        val diasEnMes = calMes.getActualMaximum(Calendar.DAY_OF_MONTH)

        for (dia in 1..diasEnMes) {
            val esDiaHoy = esHoyMismo && dia == calHoy.get(Calendar.DAY_OF_MONTH)
            if (esDiaHoy) {
                mapa[dia] = EstadoDia.HOY_ACTIVO
                continue
            }

            // Solo colorear días pasados (ya terminaron)
            val esAnterior = if (esHoyMismo) dia < calHoy.get(Calendar.DAY_OF_MONTH)
                             else anio < calHoy.get(Calendar.YEAR) ||
                                  (anio == calHoy.get(Calendar.YEAR) && mes < calHoy.get(Calendar.MONTH))

            if (!esAnterior) continue   // días futuros: sin color

            val regsDelDia = regsPorDia[dia] ?: emptyList()
            if (regsDelDia.isEmpty()) {
                mapa[dia] = EstadoDia.SIN_REGISTROS
            } else {
                val tomados  = regsDelDia.count { it.registro.estado == "Tomado" }
                val perdidos = regsDelDia.count { it.registro.estado == "Perdido" }
                mapa[dia] = when {
                    tomados == regsDelDia.size -> EstadoDia.COMPLETO
                    perdidos == regsDelDia.size -> EstadoDia.NINGUNO
                    else -> EstadoDia.PARCIAL
                }
            }
        }
        mapa
    }

    // Primer día de semana del mes (Dom=1 … Sáb=7)
    val calPrimer = Calendar.getInstance().apply { set(anio, mes, 1) }
    val offsetInicial = calPrimer.get(Calendar.DAY_OF_WEEK) - 1  // 0-based

    val calMes2 = Calendar.getInstance().apply { set(anio, mes, 1) }
    val diasEnMes = calMes2.getActualMaximum(Calendar.DAY_OF_MONTH)

    val celdas: List<Int?> = buildList {
        repeat(offsetInicial) { add(null) }
        for (d in 1..diasEnMes) add(d)
    }

    val esMesActual = anio == hoy.get(Calendar.YEAR) && mes == hoy.get(Calendar.MONTH)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Cabecera mes + flechas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onMesAnterior) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Mes anterior",
                        tint = Color(0xFFF2994A)
                    )
                }
                Text(
                    text = "${nombresMes[mes]} $anio",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                )
                IconButton(
                    onClick = onMesSiguiente,
                    enabled = !esMesActual
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Mes siguiente",
                        tint = if (esMesActual) Color.LightGray else Color(0xFFF2994A)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Cabecera días
            val diasSemana = listOf("Dom", "Lun", "Mar", "Mie", "Jue", "Vie", "Sáb")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                diasSemana.forEach { d ->
                    Text(
                        text = d,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        color = Color.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Filas del calendario
            val chunks = celdas.chunked(7)
            chunks.forEach { semana ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    val celdaSemana = if (semana.size < 7) semana + List(7 - semana.size) { null } else semana
                    celdaSemana.forEach { dia ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            if (dia != null) {
                                val estado = estadosPorDia[dia]
                                DiaCalendario(dia = dia, estado = estado)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DiaCalendario(dia: Int, estado: EstadoDia?) {
    val bgColor: Color
    val textColor: Color
    val borderColor: Color?
    val hasBorder: Boolean

    when (estado) {
        EstadoDia.HOY_ACTIVO -> {
            bgColor     = Color.Transparent
            textColor   = colorCalHoy
            borderColor = colorCalHoy
            hasBorder   = true
        }
        EstadoDia.COMPLETO -> {
            bgColor     = colorCalVerde
            textColor   = Color.White
            borderColor = null
            hasBorder   = false
        }
        EstadoDia.PARCIAL -> {
            bgColor     = colorCalNaranja
            textColor   = Color.White
            borderColor = null
            hasBorder   = false
        }
        EstadoDia.NINGUNO -> {
            bgColor     = colorCalRojo
            textColor   = Color.White
            borderColor = null
            hasBorder   = false
        }
        EstadoDia.SIN_REGISTROS -> {
            bgColor     = colorCalVacio
            textColor   = Color.DarkGray
            borderColor = null
            hasBorder   = false
        }
        null -> {
            // Día futuro sin estado: apariencia neutra
            bgColor     = Color.Transparent
            textColor   = Color.Black
            borderColor = null
            hasBorder   = false
        }
    }

    val baseModifier = Modifier
        .size(32.dp)
        .background(bgColor, RoundedCornerShape(8.dp))

    val finalModifier = if (hasBorder && borderColor != null)
        baseModifier.border(2.dp, borderColor, RoundedCornerShape(8.dp))
    else baseModifier

    Box(
        modifier = finalModifier,
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = dia.toString(),
            color = textColor,
            fontWeight = if (estado == EstadoDia.HOY_ACTIVO) FontWeight.Bold else FontWeight.Normal,
            fontSize = 13.sp
        )
    }
}

// ─── Leyenda ─────────────────────────────────────────────────────────────────

@Composable
fun LegendCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LegendItem(color = colorVerdeOscuro,  label = stringResource(id = R.string.tomado))
                LegendItem(color = Color(0xFFEB5757), label = stringResource(id = R.string.perdido))
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LegendItem(color = Color(0xFFF2994A), label = stringResource(id = R.string.parcial))
                LegendItem(color = Color(0xFFE0E0E0), label = stringResource(id = R.string.sin_medicamento))
            }
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(color, RoundedCornerShape(4.dp))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, fontSize = 14.sp, color = Color.Gray)
    }
}

// ─── Stats resumen ────────────────────────────────────────────────────────────

@Composable
fun SummaryStatsRow(adherencia: String, tomadas: String, perdidas: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SummaryStatCard(modifier = Modifier.weight(1f), value = adherencia,
            label = stringResource(id = R.string.adherencia), valueColor = colorVerdeOscuro)
        SummaryStatCard(modifier = Modifier.weight(1f), value = tomadas,
            label = stringResource(id = R.string.tomadas), valueColor = colorAzulSecundario)
        SummaryStatCard(modifier = Modifier.weight(1f), value = perdidas,
            label = stringResource(id = R.string.perdidas), valueColor = Color(0xFFEB5757))
    }
}

@Composable
fun SummaryStatCard(modifier: Modifier = Modifier, value: String, label: String, valueColor: Color) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = valueColor)
            Text(text = label, fontSize = 12.sp, color = Color.Gray)
        }
    }
}

// ─── Adherencia por medicamento ───────────────────────────────────────────────

@Composable
fun AdherenceByMedicationCard(
    adherencias: List<AdherenciaMedicamento>,
    onMedicamentoClick: (AdherenciaMedicamento) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(id = R.string.adherencia_por_medicamento),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (adherencias.isEmpty()) {
                Text(text = "No hay datos para mostrar.", fontSize = 14.sp, color = Color.Gray)
            } else {
                adherencias.forEachIndexed { index, adh ->
                    val med = adh.medicamento
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onMedicamentoClick(adh) }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = med.nombreMedicamento,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (med.activo) Color.Black else Color.Gray
                            )
                            Text(
                                text = "${adh.dosisTomadas}/${adh.totalDosis} dosis",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
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
                            val pct = adh.porcentaje
                            val pctColor = when {
                                pct >= 80 -> colorVerdeOscuro
                                pct >= 50 -> colorCalNaranja
                                else -> colorCalRojo
                            }
                            val bgColor = when {
                                pct >= 80 -> Color(0xFFE8F5E9)
                                pct >= 50 -> Color(0xFFFFF3E0)
                                else -> Color(0xFFFFEBEE)
                            }
                            Box(
                                modifier = Modifier
                                    .background(color = bgColor, shape = CircleShape)
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "$pct%",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = pctColor
                                )
                            }
                        }
                    }
                    if (index < adherencias.lastIndex) {
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f), thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

// ─── Pantalla de detalle de medicamento ──────────────────────────────────────

@Composable
fun PantallaDetalleMedicamento(
    adherencia: AdherenciaMedicamento,
    onBack: () -> Unit
) {
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val med = adherencia.medicamento

    val pct = adherencia.porcentaje
    val pctColor = when {
        pct >= 80 -> colorVerdeOscuro
        pct >= 50 -> colorCalNaranja
        else -> colorCalRojo
    }

    Scaffold(
        containerColor = colorFondoApp,
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text(text = med.nombreMedicamento, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Atrás",
                            tint = colorAzulSecundario
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Tarjeta porcentaje grande
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "$pct%",
                        fontSize = 56.sp,
                        fontWeight = FontWeight.Bold,
                        color = pctColor
                    )
                    Text(
                        text = stringResource(id = R.string.adherencia),
                        fontSize = 16.sp,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Barra de progreso
                    LinearProgressIndicator(
                        progress = { pct / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = pctColor,
                        trackColor = Color(0xFFE0E0E0)
                    )
                }
            }

            // Dosis tomadas / perdidas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DetalleStatCard(
                    modifier = Modifier.weight(1f),
                    valor = "${adherencia.dosisTomadas}",
                    etiqueta = stringResource(id = R.string.tomadas),
                    color = colorVerdeOscuro,
                    fondo = Color(0xFFE8F5E9)
                )
                DetalleStatCard(
                    modifier = Modifier.weight(1f),
                    valor = "${adherencia.dosisPerdidas}",
                    etiqueta = stringResource(id = R.string.perdidas),
                    color = colorCalRojo,
                    fondo = Color(0xFFFFEBEE)
                )
                DetalleStatCard(
                    modifier = Modifier.weight(1f),
                    valor = "${adherencia.totalDosis}",
                    etiqueta = "Total",
                    color = colorAzulSecundario,
                    fondo = Color(0xFFE3F2FD)
                )
            }

            // Fechas y estado
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(text = "Información del tratamiento", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.Black)

                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.4f))

                    DetalleFila(etiqueta = "Fecha de inicio", valor = sdf.format(Date(adherencia.fechaInicio)))

                    if (adherencia.fechaFin != null) {
                        DetalleFila(etiqueta = "Fecha de fin", valor = sdf.format(Date(adherencia.fechaFin)))
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Estado", fontSize = 14.sp, color = Color.Gray)
                            Surface(
                                color = Color(0xFFE8F5E9),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "En tratamiento",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    color = colorVerdeOscuro,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    DetalleFila(etiqueta = "Forma farmacéutica", valor = med.formaFarmaceutica)

                    if (med.dosisValor > 0f) {
                        DetalleFila(etiqueta = "Dosis", valor = "${med.dosisValor}")
                    }
                }
            }
        }
    }
}

@Composable
private fun DetalleStatCard(
    modifier: Modifier = Modifier,
    valor: String,
    etiqueta: String,
    color: Color,
    fondo: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = fondo),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = valor, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = color)
            Text(text = etiqueta, fontSize = 12.sp, color = color.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun DetalleFila(etiqueta: String, valor: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = etiqueta, fontSize = 14.sp, color = Color.Gray)
        Text(text = valor, fontSize = 14.sp, color = Color.Black, fontWeight = FontWeight.Medium)
    }
}

// ─── Bottom Navigation ────────────────────────────────────────────────────────

@Composable
fun BottomNavigationBar(navController: NavController) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            icon = { Icon(painterResource(id = R.drawable.ic_home), contentDescription = null, modifier = Modifier.size(24.dp)) },
            label = { Text(stringResource(id = R.string.inicio)) },
            selected = false,
            onClick = { navController.navigate("inicio") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = colorVerdeOscuro,
                selectedTextColor = colorVerdeOscuro,
                unselectedIconColor = colorAzulSecundario,
                unselectedTextColor = colorAzulSecundario,
                indicatorColor = Color.Transparent
            )
        )
        NavigationBarItem(
            icon = { Icon(painterResource(id = R.drawable.ic_calendar), contentDescription = null, modifier = Modifier.size(28.dp)) },
            label = { Text(stringResource(id = R.string.historial)) },
            selected = true,
            onClick = { },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = colorVerdeOscuro,
                selectedTextColor = colorVerdeOscuro,
                unselectedIconColor = colorAzulSecundario,
                unselectedTextColor = colorAzulSecundario,
                indicatorColor = Color.Transparent
            )
        )
        NavigationBarItem(
            icon = { Icon(painterResource(id = R.drawable.ic_settings), contentDescription = null, modifier = Modifier.size(24.dp)) },
            label = { Text(stringResource(id = R.string.ajustes)) },
            selected = false,
            onClick = { navController.navigate("pantallaAjustes") },
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
fun PantallaHistorialPreview() {
    MediTrackTheme {
        PantallaHistorial(navController = NavController(LocalContext.current))
    }
}
