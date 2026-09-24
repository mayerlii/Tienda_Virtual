package com.example.tienda_virtual.archivos

data class Pedido(
    val id: Int,
    val compradorId: Int,
    val productos: List<CarritoItem>,
    val total: Double,
    var estado: EstadoPedido
)

enum class EstadoPedido {
    SOLICITUD_RECIBIDA,
    EN_PREPARACION,
    ENTREGADO,
    CANCELADO
}