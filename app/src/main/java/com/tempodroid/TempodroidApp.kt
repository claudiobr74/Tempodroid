package com.tempodroid

import android.app.Application

class TempodroidApp : Application() {
    override fun onCreate() {
        super.onCreate()
        /*
         * Intencionalmente sem refresh/scheduling aqui.
         *
         * O processo pode ser criado apenas para receber o alarme astronômico
         * de 5 minutos. Agendar ou disparar clima neste ponto transformaria um
         * redraw local em acesso de rede. O ciclo de vida do AppWidget, o boot
         * receiver e a própria UI são responsáveis por iniciar as agendas
         * quando realmente existem widgets instalados.
         */
    }
}
