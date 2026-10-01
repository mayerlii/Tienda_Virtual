package com.example.tienda_virtual.data.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.content.UriMatcher
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.tienda_virtual.data.local.AppDatabase

class ProductoContentProvider : ContentProvider() {

    private lateinit var database: SupportSQLiteDatabase

    override fun onCreate(): Boolean {
        val appContext = context ?: return false
        database = AppDatabase.getDatabase(appContext).openHelper.writableDatabase
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor {
        val (where, whereArgs) = selectionForUri(uri, selection, selectionArgs)
        val columns = (projection ?: ProductoContract.Columns.ALL).also { selectedColumns ->
            require(selectedColumns.all { it in ProductoContract.Columns.ALL }) {
                "La proyección contiene una columna no soportada"
            }
        }.joinToString(", ")
        val sql = buildString {
            append("SELECT $columns FROM ${ProductoContract.TABLE_NAME}")
            if (!where.isNullOrBlank()) append(" WHERE $where")
            append(" ORDER BY ${sortOrder ?: "${ProductoContract.Columns.ID} DESC"}")
        }
        val cursor = database.query(SimpleSQLiteQuery(sql, whereArgs ?: emptyArray()))
        cursor.setNotificationUri(context?.contentResolver, uri)
        return cursor
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri {
        requireCollectionUri(uri)
        val rowId = database.insert(
            ProductoContract.TABLE_NAME,
            SQLiteDatabase.CONFLICT_ABORT,
            values ?: ContentValues(),
        )
        check(rowId != -1L) { "No se pudo insertar el producto" }
        val resultUri = Uri.withAppendedPath(ProductoContract.CONTENT_URI, rowId.toString())
        context?.contentResolver?.notifyChange(resultUri, null)
        return resultUri
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        requireValues(values)
        val contentValues = values ?: error("Se requieren valores para modificar un producto")
        val (where, whereArgs) = selectionForUri(uri, selection, selectionArgs)
        val updatedRows = database.update(
            ProductoContract.TABLE_NAME,
            SQLiteDatabase.CONFLICT_ABORT,
            contentValues,
            where,
            whereArgs,
        )
        if (updatedRows > 0) {
            context?.contentResolver?.notifyChange(uri, null)
        }
        return updatedRows
    }

    override fun delete(
        uri: Uri,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        val (where, whereArgs) = selectionForUri(uri, selection, selectionArgs)
        val deletedRows = database.delete(
            ProductoContract.TABLE_NAME,
            where,
            whereArgs,
        )
        if (deletedRows > 0) {
            context?.contentResolver?.notifyChange(uri, null)
        }
        return deletedRows
    }

    override fun getType(uri: Uri): String = when (matcher.match(uri)) {
        PRODUCTS -> ProductoContract.CONTENT_TYPE
        PRODUCT_ID -> ProductoContract.CONTENT_ITEM_TYPE
        else -> throw IllegalArgumentException("URI no soportada: $uri")
    }

    private fun selectionForUri(
        uri: Uri,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Pair<String?, Array<String>?> {
        return when (matcher.match(uri)) {
            PRODUCTS -> selection to normalizeArgs(selectionArgs)
            PRODUCT_ID -> {
                val idSelection = "${ProductoContract.Columns.ID} = ?"
                val productId: String = uri.lastPathSegment ?: error("Falta el id del producto")
                val args: Array<String> = buildList {
                    add(productId)
                    normalizeArgs(selectionArgs)?.forEach(::add)
                }.toTypedArray()
                if (selection.isNullOrBlank()) {
                    idSelection to arrayOf(productId)
                } else {
                    "$idSelection AND ($selection)" to args
                }
            }
            else -> throw IllegalArgumentException("URI no soportada: $uri")
        }
    }

    private fun normalizeArgs(selectionArgs: Array<out String>?): Array<String>? =
        selectionArgs?.map { it.toString() }?.toTypedArray()

    private fun requireCollectionUri(uri: Uri) {
        if (matcher.match(uri) != PRODUCTS) {
            throw IllegalArgumentException("Insertar requiere la URI de colección: $uri")
        }
    }

    private fun requireValues(values: ContentValues?) {
        require(values != null && values.size() > 0) { "Se requieren valores para modificar un producto" }
    }

    companion object {
        private const val PRODUCTS = 1
        private const val PRODUCT_ID = 2

        private val matcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(ProductoContract.AUTHORITY, ProductoContract.PATH_PRODUCTOS, PRODUCTS)
            addURI(ProductoContract.AUTHORITY, "${ProductoContract.PATH_PRODUCTOS}/#", PRODUCT_ID)
        }
    }
}
