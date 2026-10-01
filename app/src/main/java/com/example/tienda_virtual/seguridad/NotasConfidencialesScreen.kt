package com.example.tienda_virtual.seguridad

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotasConfidencialesScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val storageManager = remember { SeguridadStorageManager(context) }

    var tituloNota by remember { mutableStateOf("Credenciales Vendedor y PIN Bancario") }
    var contenidoNota by remember {
        mutableStateOf("Usuario: admin_tienda\nClave Secreta: Udenar2026MOVIL\nPIN de Transacciones: 8492\nToken API: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    }

    var notaCifradaGuardada by remember { mutableStateOf<NotaConfidencial?>(null) }
    var rawCifradoTexto by remember { mutableStateOf("") }
    var notaExternaTexto by remember { mutableStateOf<String?>(null) }

    var rutaCifrada by remember { mutableStateOf("") }
    var rutaExterna by remember { mutableStateOf("") }

    // Función para refrescar los datos desde el almacenamiento
    fun actualizarEstadoLectura() {
        notaCifradaGuardada = storageManager.leerNotaCifrada()
        rawCifradoTexto = storageManager.obtenerContenidoRawCifrado()
        notaExternaTexto = storageManager.leerNotaExternaInsegura()
        rutaCifrada = storageManager.obtenerRutaArchivoCifrado()
        rutaExterna = storageManager.obtenerRutaArchivoExterno()
    }

    LaunchedEffect(Unit) {
        actualizarEstadoLectura()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Seguridad y Cifrado") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        storageManager.limpiarPruebas()
                        actualizarEstadoLectura()
                        Toast.makeText(context, "Almacenamiento limpiado", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Limpiar pruebas",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header explicativo
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Cifrado Local vs Almacenamiento SD",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Guarde una nota confidencial para contrastar el almacenamiento interno con EncryptedSharedPreferences (AES-256) frente al almacenamiento externo sin cifrar.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Formulario de Nota
            Card(
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Datos de la Nota Confidencial",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = tituloNota,
                        onValueChange = { tituloNota = it },
                        label = { Text("Título de la Nota") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = contenidoNota,
                        onValueChange = { contenidoNota = it },
                        label = { Text("Contenido Confidencial (PIN, Token, Claves)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (tituloNota.isBlank() || contenidoNota.isBlank()) {
                                    Toast.makeText(
                                        context,
                                        "Por favor complete el título y contenido",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@Button
                                }
                                val nota = NotaConfidencial(
                                    titulo = tituloNota.trim(),
                                    contenido = contenidoNota.trim()
                                )
                                // Guardar en ambos medios
                                storageManager.guardarNotaCifrada(nota)
                                storageManager.guardarNotaExternaInsegura(nota)

                                actualizarEstadoLectura()
                                Toast.makeText(
                                    context,
                                    "Nota guardada en almacenamiento Interno Cifrado y Externo SD",
                                    Toast.LENGTH_LONG
                                ).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Guardar y Comparar")
                        }
                    }
                }
            }

            // Sección 1: Almacenamiento Interno Privado Cifrado
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "1. Almacenamiento Interno Privado (Cifrado)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                    }

                    Text(
                        text = "Mecanismo: EncryptedSharedPreferences (Android Keystore + MasterKey AES256-GCM y AES256-SIV para llaves).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Ruta
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Ruta: $rutaCifrada",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(rutaCifrada))
                                    Toast.makeText(context, "Ruta copiada", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = "Copiar",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Lo que ve la App Autorizada
                    Text(
                        text = "Lectura desde la App (Descifrado transparente en tiempo de ejecución):",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (notaCifradaGuardada != null) {
                        Surface(
                            color = Color(0xFFE8F5E9),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "Título: ${notaCifradaGuardada?.titulo}",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1B5E20),
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Contenido: ${notaCifradaGuardada?.contenido}",
                                    color = Color(0xFF1B5E20),
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "Fecha: ${notaCifradaGuardada?.fecha}",
                                    color = Color(0xFF2E7D32),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "No se ha guardado ninguna nota todavía.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    // Lo que ve un atacante / ADB / Device File Explorer (RAW)
                    Text(
                        text = "Inspección RAW del archivo físico (Lo que ve ADB o un atacante sin acceso al KeyStore):",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 140.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF212121))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = if (rawCifradoTexto.isNotBlank()) rawCifradoTexto else "Archivo no generado aún.",
                            color = Color(0xFF81C784),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            modifier = Modifier
                                .verticalScroll(rememberScrollState())
                                .horizontalScroll(rememberScrollState())
                        )
                    }
                }
            }

            // Sección 2: Almacenamiento Externo Sin Cifrar
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "2. Almacenamiento Externo / SD (Inseguro)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Text(
                        text = "Mecanismo: Archivo plano (.txt) en almacenamiento compartido/externo sin cifrado. Vulnerable a extracción directa por ADB, Device Explorer o apps maliciosas con permisos de lectura.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Ruta
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Ruta: $rutaExterna",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(rutaExterna))
                                    Toast.makeText(context, "Ruta copiada", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = "Copiar",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Lo que ve un atacante / ADB al inspeccionar la SD
                    Text(
                        text = "Inspección RAW del archivo físico externo (Texto Plano Expuesto):",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 140.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF263238))
                            .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = if (!notaExternaTexto.isNullOrBlank()) notaExternaTexto!! else "Archivo externo no generado aún.",
                            color = Color(0xFFFF8A80),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            modifier = Modifier
                                .verticalScroll(rememberScrollState())
                                .horizontalScroll(rememberScrollState())
                        )
                    }
                }
            }

            // Comandos ADB para auditoría
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Comandos de Inspección ADB",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Ejecute estos comandos en su terminal para comprobar la diferencia de seguridad:",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Surface(
                        color = Color(0xFF1E1E1E),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = """
                                # 1. Inspeccionar almacenamiento externo (100% Legible):
                                adb shell cat "$rutaExterna"
                                
                                # 2. Inspeccionar archivo cifrado (Cifrado con Keystore):
                                adb shell run-as com.example.tienda_virtual cat shared_prefs/encrypted_notes_prefs.xml
                            """.trimIndent(),
                            color = Color(0xFFCE9178),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
