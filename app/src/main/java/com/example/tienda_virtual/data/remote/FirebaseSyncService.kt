package com.example.tienda_virtual.data.remote

import android.content.Context
import android.util.Log
import com.example.tienda_virtual.archivos.Producto
import com.example.tienda_virtual.viewmodel.UsuarioCuenta
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * DTO para la sincronización de usuarios (Vendedor / Comprador) con Firebase.
 */
data class UsuarioFirebaseDto(
    val nombre: String = "",
    val correo: String = "",
    val celular: String = "",
    val direccionEnvio: String = "",
    val rol: String = "",
    val foto: String = "",
    val ultimaActualizacion: Long = System.currentTimeMillis()
)

/**
 * Servicio de sincronización en la nube para Firebase.
 * Sincroniza en tiempo real tanto con Cloud Firestore (SDK nativo con google-services.json)
 * como con la API HTTP/REST de Firebase (Punto 5).
 */
class FirebaseSyncService(context: Context) {

    companion object {
        private const val TAG = "FirebaseSyncService"
        private const val PREFS_NAME = "firebase_sync_prefs"
        private const val KEY_BASE_URL = "custom_firebase_url"

        // URL del proyecto Firebase configurado
        private const val DEFAULT_FIREBASE_URL = "https://tienda-virtual-86979-default-rtdb.firebaseio.com"
        private const val COLLECTION_USUARIOS = "usuarios"
        private const val COLLECTION_PRODUCTOS = "productos"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    // Instancia de Cloud Firestore
    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .writeTimeout(8, TimeUnit.SECONDS)
        .build()

    fun obtenerBaseUrl(): String {
        val url = prefs.getString(KEY_BASE_URL, DEFAULT_FIREBASE_URL) ?: DEFAULT_FIREBASE_URL
        return url.trimEnd('/')
    }

    fun configurarBaseUrl(nuevaUrl: String) {
        prefs.edit().putString(KEY_BASE_URL, nuevaUrl.trim().trimEnd('/')).apply()
    }

    private fun sanitizarKey(correo: String): String {
        return correo.trim().lowercase().replace(".", "_").replace("@", "_at_")
    }

    // 
    // 1. SINCRONIZACIÓN DE USUARIOS (VENDEDOR Y COMPRADOR)
    // 

    /**
     * Sincroniza la información del vendedor o comprador en Firebase (Firestore + REST).
     */
    suspend fun sincronizarUsuario(usuario: UsuarioCuenta): Boolean = withContext(Dispatchers.IO) {
        var exito = false
        val correoDocId = usuario.correo.trim().lowercase()

        val datos = hashMapOf(
            "nombre" to usuario.nombre,
            "correo" to usuario.correo,
            "celular" to usuario.celular,
            "direccionEnvio" to usuario.direccionEnvio,
            "rol" to usuario.rol.name,
            "foto" to if (usuario.foto.length > 500) "" else usuario.foto,
            "ultimaActualizacion" to System.currentTimeMillis()
        )

        // 1. Intento en Cloud Firestore
        try {
            firestore.collection(COLLECTION_USUARIOS)
                .document(correoDocId)
                .set(datos, SetOptions.merge())
                .await()
            Log.d(TAG, "Usuario sincronizado en Firestore: ${usuario.correo} (${usuario.rol})")
            exito = true
        } catch (e: Exception) {
            Log.w(TAG, "Firestore sync aviso para usuario ${usuario.correo}: ${e.message}")
        }

        // 2. Respaldo vía HTTP/REST
        try {
            val key = sanitizarKey(usuario.correo)
            val url = "${obtenerBaseUrl()}/$COLLECTION_USUARIOS/$key.json"
            val jsonBody = gson.toJson(datos).toRequestBody(jsonMediaType)
            val request = Request.Builder().url(url).put(jsonBody).build()
            httpClient.newCall(request).execute().use { resp ->
                if (resp.isSuccessful) exito = true
            }
        } catch (_: Exception) {
            // No interrumpe si Firestore ya tuvo éxito
        }

        return@withContext exito
    }

    // 
    // 2. SINCRONIZACIÓN DEL CATÁLOGO DE PRODUCTOS (CACHÉ HÍBRIDA)
    // 

    /**
     * Descarga el catálogo de productos desde Firebase (Firestore con fallback a REST).
     */
    suspend fun descargarCatalogoProductosRemoto(): List<Producto> = withContext(Dispatchers.IO) {
        // 1. Intentar desde Cloud Firestore
        try {
            val snapshot = firestore.collection(COLLECTION_PRODUCTOS).get().await()
            if (!snapshot.isEmpty) {
                val lista = snapshot.documents.mapNotNull { doc ->
                    try {
                        val id = doc.getLong("id")?.toInt() ?: doc.id.toIntOrNull() ?: return@mapNotNull null
                        Producto(
                            id = id,
                            nombre = doc.getString("nombre") ?: "",
                            foto = doc.getString("foto") ?: "",
                            categoria = doc.getString("categoria") ?: "",
                            precio = doc.getDouble("precio") ?: 0.0,
                            descripcion = doc.getString("descripcion") ?: "",
                            vendedorId = doc.getLong("vendedorId")?.toInt() ?: 0,
                            vendedorNombre = doc.getString("vendedorNombre") ?: "Vendedor JAS",
                            vendedorCorreo = doc.getString("vendedorCorreo") ?: "",
                            stock = doc.getLong("stock")?.toInt() ?: 10
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                Log.d(TAG, "Descargados ${lista.size} productos desde Cloud Firestore")
                if (lista.isNotEmpty()) return@withContext lista
            }
        } catch (e: Exception) {
            Log.w(TAG, "Firestore descargar productos aviso: ${e.message}")
        }

        // 2. Fallback a HTTP/REST
        try {
            val url = "${obtenerBaseUrl()}/$COLLECTION_PRODUCTOS.json"
            val request = Request.Builder().url(url).get().build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val bodyString = response.body?.string() ?: return@withContext emptyList()
                if (bodyString == "null" || bodyString.isBlank()) return@withContext emptyList()

                val productosList = mutableListOf<Producto>()
                if (bodyString.trim().startsWith("[")) {
                    val listType = object : TypeToken<List<Producto?>>() {}.type
                    val lista: List<Producto?> = gson.fromJson(bodyString, listType)
                    productosList.addAll(lista.filterNotNull())
                } else {
                    val mapType = object : TypeToken<Map<String, Producto>>() {}.type
                    val mapa: Map<String, Producto> = gson.fromJson(bodyString, mapType)
                    productosList.addAll(mapa.values)
                }
                Log.d(TAG, "Descargados ${productosList.size} productos vía REST")
                return@withContext productosList
            }
        } catch (e: Exception) {
            Log.w(TAG, "Fallo al consultar catálogo remoto: ${e.message}")
            return@withContext emptyList()
        }
    }

    /**
     * Sube o actualiza un producto en Firebase (Firestore + REST).
     */
    suspend fun subirOActualizarProducto(producto: Producto): Boolean = withContext(Dispatchers.IO) {
        var exito = false
        val prodMap = hashMapOf(
            "id" to producto.id,
            "nombre" to producto.nombre,
            "foto" to producto.foto,
            "categoria" to producto.categoria,
            "precio" to producto.precio,
            "descripcion" to producto.descripcion,
            "vendedorId" to producto.vendedorId,
            "vendedorNombre" to producto.vendedorNombre,
            "vendedorCorreo" to producto.vendedorCorreo,
            "stock" to producto.stock,
            "ultimaActualizacion" to System.currentTimeMillis()
        )

        try {
            firestore.collection(COLLECTION_PRODUCTOS)
                .document(producto.id.toString())
                .set(prodMap, SetOptions.merge())
                .await()
            Log.d(TAG, "✅ Producto ${producto.id} (${producto.nombre}) sincronizado en Firestore")
            exito = true
        } catch (e: Exception) {
            Log.w(TAG, "Firestore subir producto aviso: ${e.message}")
        }

        try {
            val url = "${obtenerBaseUrl()}/$COLLECTION_PRODUCTOS/${producto.id}.json"
            val jsonBody = gson.toJson(producto).toRequestBody(jsonMediaType)
            val request = Request.Builder().url(url).put(jsonBody).build()
            httpClient.newCall(request).execute().use { resp ->
                if (resp.isSuccessful) exito = true
            }
        } catch (_: Exception) {}

        return@withContext exito
    }

    /**
     * Elimina un producto en Firebase (Firestore + REST).
     */
    suspend fun eliminarProductoRemoto(productoId: Int): Boolean = withContext(Dispatchers.IO) {
        var exito = false
        try {
            firestore.collection(COLLECTION_PRODUCTOS)
                .document(productoId.toString())
                .delete()
                .await()
            exito = true
        } catch (e: Exception) {
            Log.w(TAG, "Firestore eliminar producto aviso: ${e.message}")
        }

        try {
            val url = "${obtenerBaseUrl()}/$COLLECTION_PRODUCTOS/$productoId.json"
            val request = Request.Builder().url(url).delete().build()
            httpClient.newCall(request).execute().use { resp ->
                if (resp.isSuccessful) exito = true
            }
        } catch (_: Exception) {}

        return@withContext exito
    }

    /**
     * Si la nube se encuentra vacía, puebla Firestore y REST con los productos iniciales.
     */
    suspend fun poblarCatalogoInicialSiVacio(productosLocales: List<Producto>): Boolean = withContext(Dispatchers.IO) {
        try {
            val remotos = descargarCatalogoProductosRemoto()
            if (remotos.isEmpty() && productosLocales.isNotEmpty()) {
                Log.d(TAG, "Poblando Firebase con ${productosLocales.size} productos locales iniciales...")
                for (p in productosLocales) {
                    subirOActualizarProducto(p)
                }
            }
            return@withContext true
        } catch (e: Exception) {
            Log.w(TAG, "Error al poblar catálogo inicial: ${e.message}")
            return@withContext false
        }
    }
}
