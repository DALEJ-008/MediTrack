package com.example.meditrack.ui.theme.pantallas

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.meditrack.R
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.meditrack.ui.theme.UsuarioViewModel
import com.example.meditrack.ui.theme.colorFondoApp

@Composable
fun pantallaInicioSesion(
    navController: NavController,
    viewModel: UsuarioViewModel = viewModel()
) {
    val usuarioLogueado by viewModel.usuarioLogueado.collectAsState()
    val error by viewModel.error.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Al detectar un login exitoso, lleva a la pantalla principal
    LaunchedEffect(usuarioLogueado) {
        if (usuarioLogueado != null) {
            navController.navigate("inicio") {
                popUpTo("inicioSesión") { inclusive = true }
            }
        }
    }

    // Mostrar errores (ej: contraseña incorrecta) en un Snackbar
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.limpiarError()
        }
    }

    val colorAzul = Color(0xFF157FFB)
    var correoUsuario by remember { mutableStateOf("") }
    var contrasenaUsuario by remember { mutableStateOf("") }
    var contrasenaVisible by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = colorFondoApp
    ) { padding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp)) {

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.meditrack1),
                    contentDescription = null,
                    modifier = Modifier.size(300.dp)
                )

                Column(modifier = Modifier.offset(y = (-40.dp))) {
                    Text(text = "Iniciar Sesión", fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("¡Hola! Es bueno verte de nuevo", fontSize = 18.sp, fontWeight = FontWeight.Light)

                    Spacer(modifier = Modifier.height(20.dp))

                    Text("Correo electrónico", fontSize = 16.sp, color = colorAzul)
                    TextField(
                        value = correoUsuario,
                        onValueChange = { correoUsuario = it },
                        label = { Text("Ingresa tu email") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Text("Contraseña", fontSize = 16.sp, color = colorAzul)
                    TextField(
                        value = contrasenaUsuario,
                        onValueChange = { contrasenaUsuario = it },
                        label = { Text("Ingresa tu contraseña") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (contrasenaVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            val image = if (contrasenaVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                            IconButton(onClick = { contrasenaVisible = !contrasenaVisible }) {
                                Icon(imageVector = image, contentDescription = null)
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )

                    Spacer(modifier = Modifier.height(30.dp))

                    Button(
                        onClick = {
                            if (correoUsuario.isNotEmpty() && contrasenaUsuario.isNotEmpty()) {
                                viewModel.login(correoUsuario, contrasenaUsuario)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = colorAzul)
                    ) {
                        Text("Iniciar Sesión", color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "¿Olvidaste tu contraseña?",
                        color = colorAzul,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { /* Navegación */ }
                    )

                    Spacer(modifier = Modifier.height(30.dp))
                    Text("¿No tienes una cuenta?", color = Color.Gray)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Regístrate",
                        color = colorAzul,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            navController.navigate("registro")
                        }
                    )
                }
            }
        }
    }
}
