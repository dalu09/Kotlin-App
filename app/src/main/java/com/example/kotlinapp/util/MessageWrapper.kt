package com.example.kotlinapp.util

/**
 * Se usa como un contenedor para datos que representan un evento de un solo uso (por ejemplo, un mensaje).
 */
open class MessageWrapper<out T>(private val content: T) {

    var hasBeenHandled = false
        private set // Permite la lectura externa, pero no la escritura.

    /**
     * Devuelve el contenido y previene que se vuelva a usar.
     */
    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled) {
            null
        } else {
            hasBeenHandled = true
            content
        }
    }

    /**
     * Devuelve el contenido, incluso si ya ha sido gestionado.
     */
    fun peekContent(): T = content
}
