package com.example.tienda_virtual.archivos

data class CarritoItem(
    val producto: Producto,
    var cantidad: Int
) {
    fun subtotal(): Double {
        return producto.precio * cantidad
    }
}