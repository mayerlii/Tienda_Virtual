package com.example.tienda_virtual.seguridad

import android.content.Context
import android.os.Environment
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Modelo de datos para las notas confidenciales comparativas.
 */
data class NotaConfidencial(
    val titulo: String,
    val contenido: String,
    val fecha: String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
)

/**
 * Gestor encargado de almacenar notas confidenciales utilizando dos mecanismos opuestos:
 * 1. Almacenamiento Interno Privado con Cifrado (EncryptedSharedPreferences + MasterKey AES256_GCM).
 * 2. Almacenamiento Externo / SD sin Cifrado (Archivo plano en formato de texto accesible).
 */
class SeguridadStorageManager(private val context: Context) {

    companion object {
        private const val ENCRYPTED_PREFS_FILENAME = "encrypted_notes_prefs"
        private const val KEY_TITULO = "nota_secreta_titulo"
        private const val KEY_CONTENIDO = "nota_secreta_contenido"
        private const val KEY_FECHA = "nota_secreta_fecha"

        private const val EXTERNAL_INSECURE_FILENAME = "nota_insegura_externa.txt"
    }

    // Inicialización  de la MasterKey en Android KeyStore
    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    // Inicialización  de EncryptedSharedPreferences
    private val encryptedPrefs by lazy {
        EncryptedSharedPreferences.create(
            context,
            ENCRYPTED_PREFS_FILENAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // 
    // 1. ALMACENAMIENTO INTERNO CIFRADO (SEGURO)
    // 

    /**
     * Guarda la nota confidencial en EncryptedSharedPreferences dentro del almacenamiento
     * privado de la app (/data/user/0/com.example.tienda_virtual/shared_prefs/).
     */
    fun guardarNotaCifrada(nota: NotaConfidencial) {
        encryptedPrefs.edit()
            .putString(KEY_TITULO, nota.titulo)
            .putString(KEY_CONTENIDO, nota.contenido)
            .putString(KEY_FECHA, nota.fecha)
            .apply()
    }

    /**
     * Lee y descifra automáticamente la nota confidencial almacenada en memoria interna.
     */
    fun leerNotaCifrada(): NotaConfidencial? {
        val titulo = encryptedPrefs.getString(KEY_TITULO, null) ?: return null
        val contenido = encryptedPrefs.getString(KEY_CONTENIDO, "") ?: ""
        val fecha = encryptedPrefs.getString(KEY_FECHA, "") ?: ""
        return NotaConfidencial(titulo = titulo, contenido = contenido, fecha = fecha)
    }

    /**
     * Devuelve la ruta absoluta del archivo XML cifrado en el almacenamiento interno privado.
     */
    fun obtenerRutaArchivoCifrado(): String {
        val dataDir = context.applicationInfo.dataDir
        return "$dataDir/shared_prefs/$ENCRYPTED_PREFS_FILENAME.xml"
    }

    /**
     * Lee directamente el contenido RAW del archivo XML de SharedPreferences para mostrar
     * cómo se ven los datos cifrados sin la llave de descifrado (Base64 cifrado / Ciphertext).
     */
    fun obtenerContenidoRawCifrado(): String {
        val file = File(obtenerRutaArchivoCifrado())
        return if (file.exists()) {
            try {
                file.readText()
            } catch (e: Exception) {
                "Error al leer archivo cifrado: ${e.message}"
            }
        } else {
            "El archivo cifrado aún no existe."
        }
    }

    // 
    // 2. ALMACENAMIENTO EXTERNO / SD SIN CIFRADO (INSEGURO)
    // 

    /**
     * Obtiene el archivo en almacenamiento externo (simulado o físico en /sdcard/).
     */
    private fun obtenerArchivoExterno(): File {
        val externalDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            ?: context.getExternalFilesDir(null)
            ?: context.filesDir
        if (!externalDir.exists()) {
            externalDir.mkdirs()
        }
        return File(externalDir, EXTERNAL_INSECURE_FILENAME)
    }

    /**
     * Guarda una copia idéntica de la nota sensible en un archivo de texto plano en la SD/externo.
     */
    fun guardarNotaExternaInsegura(nota: NotaConfidencial) {
        val file = obtenerArchivoExterno()
        val contenidoPlano = """
            =====================================================
            DOCUMENTO CONFIDENCIAL - ALMACENAMIENTO EXTERNO
            ADVERTENCIA: ARCHIVO NO CIFRADO (VULNERABLE A INSPECCIÓN)
            =====================================================
            Fecha de Guardado: ${nota.fecha}
            Título: ${nota.titulo}
            Contenido Sensible:
            ${nota.contenido}
            =====================================================
        """.trimIndent()
        file.writeText(contenidoPlano)
    }

    /**
     * Lee la copia de la nota desde el almacenamiento externo.
     */
    fun leerNotaExternaInsegura(): String? {
        val file = obtenerArchivoExterno()
        return if (file.exists()) {
            try {
                file.readText()
            } catch (e: Exception) {
                "Error al leer archivo externo: ${e.message}"
            }
        } else {
            null
        }
    }

    /**
     * Devuelve la ruta absoluta del archivo en almacenamiento externo.
     */
    fun obtenerRutaArchivoExterno(): String {
        return obtenerArchivoExterno().absolutePath
    }

    /**
     * Borra ambos archivos para pruebas limpias.
     */
    fun limpiarPruebas() {
        encryptedPrefs.edit().clear().apply()
        val extFile = obtenerArchivoExterno()
        if (extFile.exists()) {
            extFile.delete()
        }
    }
}
