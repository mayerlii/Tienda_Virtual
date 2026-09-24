package com.example.tienda_virtual.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.tienda_virtual.archivos.Producto
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductoDao {

    @Query("SELECT * FROM productos ORDER BY id DESC")
    fun obtenerTodosLosProductos(): Flow<List<Producto>>

    @Query("SELECT * FROM productos WHERE vendedorId = :vendedorId ORDER BY id DESC")
    fun obtenerProductosPorVendedor(vendedorId: Int): Flow<List<Producto>>

    @Query("SELECT * FROM productos WHERE id = :id")
    suspend fun obtenerProductoPorId(id: Int): Producto?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarProducto(producto: Producto): Long

    @Update
    suspend fun actualizarProducto(producto: Producto)

    @Delete
    suspend fun eliminarProducto(producto: Producto)
}
