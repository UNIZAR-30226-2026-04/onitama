package com.example.onitama.api

import android.util.Log
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json


/**
 * Clase que contiene todos los mensajes que se pueden enviar al servidor en lo referente a comprar o activar skins de tablero y fichas
 **/
class Skin (

){

    @Serializable
    sealed class MensajeCliente

    @Serializable
    @SerialName("OBTENER_TIENDA_SKINS")
    data class MensajeObtenerTiendaSkins(
        val usuario: String
    ): MensajeCliente()

    @Serializable
    @SerialName("COMPRAR_SKIN")
    data class MensajeComprarSkin(
        val usuario: String,
        val skin_id: String
    ): MensajeCliente()

    @Serializable
    @SerialName("ACTIVAR_SKIN")
    data class MensajeActivarSkin(
        val usuario: String,
        val skin_id: String
    ): MensajeCliente()

    @Serializable
    sealed class MensajeServidor

    @Serializable
    data class Skin(
        val skin_id: String,
        val precio: Int,
        val owned: Boolean,
        val es_activa: Boolean
    )

    @Serializable
    @SerialName("TIENDA_SKINS")
    data class MensajeTiendaSkins(
        val usuario: String,
        val cores: Int,
        val skin_activa: String,
        val skins: List<Skin>
    ): MensajeServidor()

    @Serializable
    @SerialName("COMPRA_SKIN_OK")
    data class MensajeCompraSkinOk(
        val skin_id: String,
        val cores: Int
    ): MensajeServidor()

    @Serializable
    @SerialName("COMPRA_SKIN_ERROR")
    data class MensajeCompraSkinError(
        val skin_id: String,
        val codigo: String
    ): MensajeServidor()

    @Serializable
    @SerialName("SKIN_ACTIVADA")
    data class MensajeSkinActivada(
        val skin_activa: String
    ): MensajeServidor()

    @Serializable
    @SerialName("ACTIVAR_SKIN_ERROR")
    data class MensajeActivarSkinError(
        val skin_id: String,
        val codigo: String
    ): MensajeServidor()

    private val jsonSerializer = Json {
        ignoreUnknownKeys = true
        classDiscriminator = "tipo"
    }

    /**
     * Envía un mensaje al servidor para obtener las skins de la tienda.
     * usuario: cliente que quiere obtener las skins de la tienda
     */
    fun obtenerTiendaSkins(usuario: String) {
        try {
            val mensaje = MensajeObtenerTiendaSkins(usuario)
            val jsonMsg = jsonSerializer.encodeToString<MensajeCliente>(mensaje)
            ManejadorGlobal.enviarMensaje(jsonMsg)
        } catch (e: Exception) {
            Log.e("Skin_API", "Error al enviar obtenerTiendaSkins", e)
        }
    }

    /**
     * Envía un mensaje al servidor para comprar una skin.
     * usuario: cliente que quiere comprar la skin
     * skinId: id de la skin que quiere comprar
     */
    fun comprarSkin(usuario: String, skinId: String) {
        try {
            val mensaje = MensajeComprarSkin(usuario, skinId)
            val jsonMsg = jsonSerializer.encodeToString<MensajeCliente>(mensaje)
            ManejadorGlobal.enviarMensaje(jsonMsg)
        } catch (e: Exception) {
            Log.e("Skin_API", "Error al enviar comprarSkin", e)
        }
    }

    /**
     * Envía un mensaje al servidor para activar una skin.
     * usuario: cliente que quiere activar la skin
     * skinId: id de la skin que quiere activar
     */
    fun activarSkin(usuario: String, skinId: String) {
        try {
            val mensaje = MensajeActivarSkin(usuario, skinId)
            val jsonMsg = jsonSerializer.encodeToString<MensajeCliente>(mensaje)
            ManejadorGlobal.enviarMensaje(jsonMsg)
        } catch (e: Exception) {
            Log.e("Skin_API", "Error al enviar activarSkin", e)
        }
    }
}