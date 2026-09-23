package com.pushblok.app

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Jednoduchý wrapper okolo Android TextToSpeech (funguje offline - beží priamo
 * v telefóne, nepotrebuje internet). Hlásky hovorí s malým "cooldownom", aby
 * appka nekecala pri každom snímku dookola.
 */
class VoiceCoach(context: Context) {

    private var tts: TextToSpeech? = null
    private var ready = false
    private var lastSpokenAt = 0L
    private var lastMessage: String? = null
    private val cooldownMs = 3000L

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("sk", "SK")
                // Ak appka slovenčinu nemá k dispozícii, TTS potichu spadne na predvolený jazyk.
                ready = true
            }
        }
    }

    fun speak(text: String) {
        if (!ready) return
        val now = System.currentTimeMillis()
        if (text == lastMessage && now - lastSpokenAt < cooldownMs) return
        lastMessage = text
        lastSpokenAt = now
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "pushblok_${now}")
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
