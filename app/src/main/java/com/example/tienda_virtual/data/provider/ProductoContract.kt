package com.example.tienda_virtual.data.provider

import android.content.ContentResolver
import android.net.Uri
import android.provider.BaseColumns

object ProductoContract {
    const val AUTHORITY = "com.example.tienda_virtual.provider"
    const val PATH_PRODUCTOS = "productos"
    const val TABLE_NAME = "productos"

    val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/$PATH_PRODUCTOS")

    const val CONTENT_TYPE = "vnd.android.cursor.dir/vnd.$AUTHORITY.$PATH_PRODUCTOS"
    const val CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.$AUTHORITY.$PATH_PRODUCTOS"

    object Columns : BaseColumns {
        const val ID = "id"
        const val NOMBRE = "nombre"
        const val FOTO = "foto"
        const val CATEGORIA = "categoria"
        const val PRECIO = "precio"
        const val DESCRIPCION = "descripcion"
        const val VENDEDOR_ID = "vendedorId"
        const val VENDEDOR_NOMBRE = "vendedorNombre"
        const val VENDEDOR_CORREO = "vendedorCorreo"
        const val STOCK = "stock"

        val ALL = arrayOf(
            ID,
            NOMBRE,
            FOTO,
            CATEGORIA,
            PRECIO,
            DESCRIPCION,
            VENDEDOR_ID,
            VENDEDOR_NOMBRE,
            VENDEDOR_CORREO,
            STOCK,
        )
    }

    object Permissions {
        const val READ = "com.example.tienda_virtual.permission.READ_PRODUCTS"
        const val WRITE = "com.example.tienda_virtual.permission.WRITE_PRODUCTS"
    }
}
