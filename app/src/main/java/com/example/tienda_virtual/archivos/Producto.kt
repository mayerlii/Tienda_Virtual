package com.example.tienda_virtual.archivos

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "productos")
data class Producto(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    var nombre: String,
    var foto: String,
    var categoria: String,
    var precio: Double,
    var descripcion: String,
    val vendedorId: Int = 0,
    var vendedorNombre: String = "Vendedor JAS",
    var vendedorCorreo: String = "",
    var stock: Int = 10,
)
