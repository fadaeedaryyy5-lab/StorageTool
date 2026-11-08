package app.storagetool

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.widget.*
import dadb.AdbKeyPair
import dadb.Dadb
import java.io.File
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private lateinit var logView: TextView
    private var adb: Dadb? = null
    private val io = Executors.newSingleThreadExecutor()
    private val uuidRe = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(48, 32, 48, 32) }
        fun btn(t: String, f: () -> Unit) = Button(this).apply { text = t; setOnClickListener { f() } }.also { root.addView(it) }
        btn("1) اتصال به ADB محلی") { connect() }
        btn("2) نمایش دیسک‌های خارجی") { listDisks() }
        btn("3) تبدیل دیسک به حافظه داخلی (Adopt)") { adopt() }
        btn("4) نصب پیش‌فرض روی حافظه خارجی") { run { sh("pm set-install-location 2") } }
        btn("5) انتقال یک برنامه به حافظه خارجی") { moveApp() }
        logView = TextView(this).apply { textSize = 16f }
        root.addView(ScrollView(this).apply { addView(logView) })
        setContentView(root)
        log("ابتدا در تنظیمات برنامه‌نویس، USB debugging و ADB over network را روشن کنید.")
    }

    private fun log(s: String) = runOnUiThread { logView.append(s + "\n") }
    private fun run(f: () -> Unit) = io.execute { try { f() } catch (e: Exception) { log("خطا: ${e.message}") } }

    private fun sh(cmd: String): String {
        val d = adb ?: throw IllegalStateException("ابتدا متصل شوید")
        val out = d.shell(cmd).allOutput.trim()
        log("$ $cmd\n$out"); return out
    }

    private fun connect() = run {
        val priv = File(filesDir, "adbkey"); val pub = File(filesDir, "adbkey.pub")
        if (!priv.exists()) AdbKeyPair.generate(priv, pub)
        log("در حال اتصال... اگر پنجره Allow USB debugging آمد، تایید کنید.")
        adb = Dadb.create("127.0.0.1", 5555, AdbKeyPair.read(priv, pub))
        log("متصل شد: " + sh("getprop ro.product.model"))
    }

    private fun listDisks() = run { sh("sm set-force-adoptable true"); sh("sm list-disks adoptable") }

    private fun adopt() = run {
        val disk = sh("sm list-disks adoptable").lines().firstOrNull { it.isNotBlank() }
        if (disk == null) { log("دیسکی پیدا نشد"); return@run }
        runOnUiThread {
            AlertDialog.Builder(this).setTitle("هشدار")
                .setMessage("دیسک $disk کاملاً پاک و فرمت می‌شود. ادامه؟")
                .setPositiveButton("بله") { _, _ -> run { sh("sm partition $disk private") } }
                .setNegativeButton("خیر", null).show()
        }
    }

    private fun moveApp() = run {
        val uuid = uuidRe.find(sh("sm list-volumes private"))?.value
        if (uuid == null) { log("حافظه Adopt‌شده‌ای پیدا نشد"); return@run }
        val pkgs = sh("pm list packages -3").lines().map { it.removePrefix("package:").trim() }.filter { it.isNotEmpty() }.sorted()
        runOnUiThread {
            AlertDialog.Builder(this).setTitle("کدام برنامه؟")
                .setItems(pkgs.toTypedArray()) { _, i -> run { sh("pm move-package ${pkgs[i]} $uuid") } }.show()
        }
    }
}
