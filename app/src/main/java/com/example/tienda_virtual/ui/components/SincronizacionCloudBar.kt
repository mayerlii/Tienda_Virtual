package com.example.tienda_virtual.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tienda_virtual.viewmodel.EstadoSincronizacion
import com.example.tienda_virtual.viewmodel.TiendaViewModel

/**
 * Barra superior informativa que muestra el estado de la estrategia de Caché Híbrida
 * y la persistencia en la nube con Firebase (Punto 5).
 */
@Composable
fun SincronizacionCloudBar(
    viewModel: TiendaViewModel,
    modifier: Modifier = Modifier
) {
    var mostrarDetallesDialog by remember { mutableStateOf(false) }

    val (icono, colorTexto, colorFondo, textoEstado) = when (viewModel.estadoSincronizacion) {
        EstadoSincronizacion.CONECTADO_CACHED -> Quad(
            Icons.Default.CloudDone,
            Color(0xFF2E7D32),
            Color(0xFFE8F5E9),
            "Caché Local Sincronizada con Firebase"
        )
        EstadoSincronizacion.SINCRONIZANDO -> Quad(
            Icons.Default.CloudSync,
            Color(0xFF1565C0),
            Color(0xFFE3F2FD),
            "Sincronizando con Firebase en 2do plano..."
        )
        EstadoSincronizacion.ERROR_OFFLINE -> Quad(
            Icons.Default.CloudOff,
            Color(0xFFD84315),
            Color(0xFFFBE9E7),
            "Modo Offline (Operando con Caché Local)"
        )
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { mostrarDetallesDialog = true },
        color = colorFondo,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icono,
                    contentDescription = null,
                    tint = colorTexto,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = textoEstado,
                    color = colorTexto,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }

            IconButton(
                onClick = { viewModel.sincronizarConFirebaseSegundoPlano() },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refrescar caché",
                    tint = colorTexto,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }

    var mostrarConfigUrlDialog by remember { mutableStateOf(false) }
    var nuevaUrlInput by remember { mutableStateOf(viewModel.firebaseSyncService.obtenerBaseUrl()) }

    if (mostrarDetallesDialog) {
        AlertDialog(
            onDismissRequest = { mostrarDetallesDialog = false },
            icon = {
                Icon(
                    imageVector = icono,
                    contentDescription = null,
                    tint = colorTexto,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Persistencia Deslocalizada (Firebase)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Estrategia: Caché Híbrida Offline-First",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "1. Inmediato: La app expone primero los registros cacheados en Room Database (SQLite).\n" +
                                "2. En 2do plano: Consulta la API REST de Firebase para actualizar los productos e información de compradores y vendedores sin bloquear la UI.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        text = "• Productos en Room: ${viewModel.productos.size}\n" +
                                "• Usuarios Registrados: ${viewModel.usuariosRegistrados.size}\n" +
                                "• Usuario Activo: ${viewModel.usuarioActual?.nombre ?: "Ninguno"} (${viewModel.usuarioActual?.rol?.name ?: "-"})\n" +
                                "• Última sincronización: ${viewModel.ultimaSincronizacionHora}\n" +
                                "• Servidor: ${viewModel.firebaseSyncService.obtenerBaseUrl()}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        mostrarDetallesDialog = false
                        nuevaUrlInput = viewModel.firebaseSyncService.obtenerBaseUrl()
                        mostrarConfigUrlDialog = true
                    }) {
                        Text("Configurar URL")
                    }
                    Button(onClick = {
                        mostrarDetallesDialog = false
                        viewModel.sincronizarConFirebaseSegundoPlano()
                    }) {
                        Text("Sincronizar")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDetallesDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }

    if (mostrarConfigUrlDialog) {
        AlertDialog(
            onDismissRequest = { mostrarConfigUrlDialog = false },
            title = { Text("Configurar URL de Firebase") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Ingrese la URL base de su Firebase Realtime Database o API REST:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = nuevaUrlInput,
                        onValueChange = { nuevaUrlInput = it },
                        label = { Text("URL de Firebase") },
                        placeholder = { Text("") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (nuevaUrlInput.isNotBlank()) {
                        viewModel.firebaseSyncService.configurarBaseUrl(nuevaUrlInput.trim())
                        viewModel.sincronizarConFirebaseSegundoPlano()
                    }
                    mostrarConfigUrlDialog = false
                }) {
                    Text("Guardar y Probar")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarConfigUrlDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
