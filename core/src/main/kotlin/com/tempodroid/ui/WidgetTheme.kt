package com.tempodroid.ui

object WidgetTypography {
    const val TEMPERATURE_SP = 34
    const val CONDITION_SP = 12
    const val CLOCK_SP = 16
    const val DATE_SP = 12
    const val CITY_SP = 11
    const val TIMELINE_SP = 8
    const val SUMMARY_SP = 11
    const val NOW_LABEL_SP = 9
}

object WidgetTheme {
    const val TEXT_PRIMARY = 0xFFFFFFFF.toInt()
    const val TEXT_SECONDARY = 0xE6FFFFFF.toInt()
    const val TEXT_TERTIARY = 0xCCFFFFFF.toInt()
    const val GLASS_FILL = 0x2EFFFFFF
    const val GLASS_STROKE = 0x33FFFFFF
    const val SUMMARY_FILL = 0x38FFFFFF
    const val MARKER_FILL = 0xFFFFFFFF.toInt()

    /**
     * Relógio: TextClock via AndroidRemoteViews integrado ao Glance.
     *
     * O widget NÃO é redesenhado a cada minuto. O TextClock do framework
     * atualiza só o texto da hora no processo do launcher, sem WorkManager
     * por minuto nem serviço persistente. Céu, clima e timeline continuam
     * no ciclo de 30 minutos (e nos eventos de criação/boot/localização).
     *
     * Fallback: se o launcher não hospedar bem o RemoteViews aninhado,
     * o Glance ainda mostra a hora do último snapshot em [displayTime].
     */
    const val CLOCK_STRATEGY =
        "TextClock via AndroidRemoteViews no Glance; sem refresh total por minuto."
}
