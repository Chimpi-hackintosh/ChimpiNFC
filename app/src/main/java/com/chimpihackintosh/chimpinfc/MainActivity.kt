package com.chimpihackintosh.chimpinfc

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView

class MainActivity : Activity(), NfcAdapter.ReaderCallback {
    private data class PendingWrite(val type: String, val value: String)
    private var nfc: NfcAdapter? = null
    @Volatile private var pending: PendingWrite? = null
    private lateinit var status: TextView
    private lateinit var scanned: TextView
    private lateinit var input: EditText
    private lateinit var types: RadioGroup
    private lateinit var armButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        status = findViewById(R.id.statusText)
        scanned = findViewById(R.id.scanResult)
        input = findViewById(R.id.payloadInput)
        types = findViewById(R.id.recordType)
        armButton = findViewById(R.id.armWriteButton)
        nfc = NfcAdapter.getDefaultAdapter(this)
        armButton.setOnClickListener { prepareWrite() }
        findViewById<Button>(R.id.cancelWriteButton).setOnClickListener {
            pending = null
            armButton.text = "Preparar gravação"
            showStatus("Modo leitura • nenhuma gravação pendente")
        }
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_NFC) || nfc == null) {
            showStatus("Este celular não tem hardware NFC, então não pode ler ou gravar etiquetas")
        }
    }

    override fun onResume() {
        super.onResume()
        val adapter = nfc ?: return
        if (!adapter.isEnabled) {
            showStatus("Ative o NFC nas configurações do Android")
            AlertDialog.Builder(this)
                .setTitle("NFC desligado")
                .setMessage("Ative o NFC para ler e gravar etiquetas")
                .setPositiveButton("Configurações") { _, _ ->
                    startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
                }
                .setNegativeButton("Agora não", null)
                .show()
            return
        }
        adapter.enableReaderMode(
            this, this,
            NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_NFC_F or NfcAdapter.FLAG_READER_NFC_V or
                NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS,
            null
        )
        showStatus("NFC ativo • aproxime uma etiqueta para ler")
    }

    override fun onPause() {
        nfc?.disableReaderMode(this)
        pending = null
        if (::armButton.isInitialized) armButton.text = "Preparar gravação"
        super.onPause()
    }

    private fun prepareWrite() {
        if (nfc?.isEnabled != true) {
            showStatus("Ative o NFC para gravar")
            return
        }
        val kind = if (types.checkedRadioButtonId == R.id.radioUrl) "url" else "text"
        val value = try {
            InputValidator.normalize(kind, input.text.toString())
        } catch (error: IllegalArgumentException) {
            showStatus("Erro: " + (error.message ?: "conteúdo inválido"))
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Confirmar gravação NFC")
            .setMessage(
                "A gravação substituirá os registros anteriores da etiqueta NFC comum. " +
                "Grave apenas etiquetas suas ou autorizadas. AirTags da Apple não são regraváveis"
            )
            .setPositiveButton("Confirmar") { _, _ ->
                pending = PendingWrite(kind, value)
                armButton.text = "Pronto para gravar"
                showStatus("GRAVAÇÃO ARMADA • aproxime uma etiqueta NFC regravável")
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onTagDiscovered(tag: Tag) {
        val request = pending
        if (request != null) {
            pending = null // Apenas uma tentativa, não repetir automaticamente
            runOnUiThread { armButton.text = "Preparar gravação" }
            writeTag(tag, request)
        } else {
            readTag(tag)
        }
    }

    private fun writeTag(tag: Tag, request: PendingWrite) {
        val ndef = Ndef.get(tag)
        if (ndef == null) {
            showStatus("Etiqueta não compatível com NDEF regravável • nada foi alterado")
            return
        }
        try {
            val record = if (request.type == "url")
                NdefRecord.createUri(request.value)
            else
                NdefRecord.createTextRecord("pt", request.value)
            val message = NdefMessage(arrayOf(record))
            ndef.connect()
            if (!ndef.isWritable) {
                showStatus("Etiqueta somente leitura • não foi alterada")
                return
            }
            if (message.toByteArray().size > ndef.maxSize) {
                showStatus("Sem espaço: necessário " + message.toByteArray().size +
                    " bytes, disponível " + ndef.maxSize)
                return
            }
            ndef.writeNdefMessage(message)
            showStatus("✅ Texto ou link gravado na etiqueta NFC com sucesso!")
        } catch (error: Exception) {
            showStatus("Falha na gravação: " + (error.localizedMessage ?: "tente novamente"))
        } finally {
            try { ndef.close() } catch (_: Exception) { }
        }
    }

    private fun readTag(tag: Tag) {
        val ndef = Ndef.get(tag)
        if (ndef == null) {
            showReading("Etiqueta detectada, mas não há conteúdo NDEF legível")
            showStatus("TAG detectada sem NDEF")
            return
        }
        try {
            ndef.connect()
            val message = ndef.ndefMessage
            if (message == null || message.records.isEmpty()) {
                showReading("Etiqueta sem registros NDEF")
                return
            }
            showReading(message.records.map(::describeRecord).joinToString("\n\n").take(6000))
            showStatus("✅ Leitura concluída • " + message.records.size + " registro(s)")
        } catch (error: Exception) {
            showStatus("Falha na leitura: " + (error.localizedMessage ?: "tente novamente"))
        } finally {
            try { ndef.close() } catch (_: Exception) { }
        }
    }

    private fun describeRecord(record: NdefRecord): String {
        val uri = try { record.toUri()?.toString() } catch (_: Exception) { null }
        if (uri != null) {
            return if (InputValidator.isPublicAirTagUrl(uri))
                "🍎 Link NFC público de AirTag (somente leitura):\n" + uri +
                "\nConfira o site da Apple manualmente, sem abrir links automaticamente"
            else "🔗 Link NFC:\n" + uri + "\nConfira antes de abrir links desconhecidos"
        }
        if (record.tnf == NdefRecord.TNF_WELL_KNOWN &&
            record.type.contentEquals(NdefRecord.RTD_TEXT)) {
            val payload = record.payload
            if (payload.isEmpty()) return "Texto NFC vazio"
            val languageLength = payload[0].toInt() and 0x3F
            if (payload.size < 1 + languageLength) return "Registro de texto inválido"
            val charset = if ((payload[0].toInt() and 0x80) != 0)
                Charsets.UTF_16 else Charsets.UTF_8
            return "📝 Texto NFC:\n" + String(
                payload, 1 + languageLength, payload.size - languageLength - 1, charset
            )
        }
        return "Registro NDEF de outro tipo, " + record.payload.size + " bytes"
    }

    private fun showStatus(value: String) {
        runOnUiThread { status.text = value }
    }

    private fun showReading(value: String) {
        runOnUiThread { scanned.text = value }
    }
}
