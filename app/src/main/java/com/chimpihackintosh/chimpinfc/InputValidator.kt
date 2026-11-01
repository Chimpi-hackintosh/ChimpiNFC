package com.chimpihackintosh.chimpinfc

import java.net.URI
import java.util.Locale

/** Validação local, sem rastreamento nem envio de dados pela internet. */
object InputValidator {
    fun normalize(kind: String, raw: String): String {
        val value = raw.trim()
        require(value.isNotEmpty()) { "Digite o conteúdo antes de gravar" }
        require(value.toByteArray(Charsets.UTF_8).size <= 8192) {
            "Conteúdo muito grande para uma etiqueta NFC comum"
        }
        return when (kind) {
            "text" -> value
            "url" -> {
                val parsed = try { URI(value) } catch (_: Exception) {
                    throw IllegalArgumentException("Link inválido")
                }
                val scheme = parsed.scheme?.lowercase(Locale.ROOT)
                val host = parsed.host?.lowercase(Locale.ROOT)
                require(scheme == "https" || scheme == "http") { "Use HTTP ou HTTPS" }
                require(!host.isNullOrBlank()) { "O link precisa de um domínio válido" }
                require(parsed.userInfo == null) { "Não coloque senha na URL" }
                require(host != "apple.com" && !host.endsWith(".apple.com") &&
                    host != "icloud.com" && !host.endsWith(".icloud.com")) {
                    "Não é permitido copiar links oficiais da Apple para uma etiqueta comum"
                }
                value
            }
            else -> throw IllegalArgumentException("Tipo de conteúdo desconhecido")
        }
    }

    fun isPublicAirTagUrl(raw: String): Boolean {
        val uri = try { URI(raw.trim()) } catch (_: Exception) { return false }
        return uri.scheme.equals("https", ignoreCase = true) &&
            uri.host.equals("found.apple.com", ignoreCase = true)
    }
}
