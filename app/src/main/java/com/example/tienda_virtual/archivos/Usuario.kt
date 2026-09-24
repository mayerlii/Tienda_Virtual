package com.example.tienda_virtual.archivos

data class Usuario(
    val id: Int,
    var nombre: String,
    var correo: String,
    var celular: String,
    var foto: String = "",
    var direccionEnvio: String = "Av. Principal Los Naranjos, Torre B, Apto 4-B",
    val rol: Rol
)

enum class Rol {
    VENDEDOR,
    COMPRADOR
}
