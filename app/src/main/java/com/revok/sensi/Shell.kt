package com.revok.sensi

import android.content.pm.PackageManager
import rikka.shizuku.Shizuku

object Shell {
    enum class Mode { SHIZUKU, ROOT, NONE }

    fun mode(): Mode = when {
        runCatching { Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED }
            .getOrDefault(false) -> Mode.SHIZUKU
        rootAvailable() -> Mode.ROOT
        else -> Mode.NONE
    }

    fun requestShizuku() { runCatching { if (Shizuku.pingBinder()) Shizuku.requestPermission(1) } }

    private fun rootAvailable() = runCatching {
        val p = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
        p.inputStream.bufferedReader().readText().contains("uid=0")
    }.getOrDefault(false)

    fun run(cmd: String): String = when (mode()) {
        Mode.SHIZUKU -> exec(shizukuProc(cmd))
        Mode.ROOT -> exec(Runtime.getRuntime().exec(arrayOf("su", "-c", cmd)))
        Mode.NONE -> "Sem acesso: ative o Shizuku ou o root."
    }

    private fun shizukuProc(cmd: String): Process {
        val m = Shizuku::class.java.getDeclaredMethod(
            "newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java)
        m.isAccessible = true
        return m.invoke(null, arrayOf("sh", "-c", cmd), null, null) as Process
    }

    private fun exec(p: Process): String {
        val out = p.inputStream.bufferedReader().readText()
        val err = p.errorStream.bufferedReader().readText()
        p.waitFor()
        return (out + err).trim()
    }
}
