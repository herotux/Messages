package org.fossify.messages.activities

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.ImageReader
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.view.Surface
import android.view.TextureView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import org.fossify.messages.R
import org.fossify.messages.helpers.IranianBankRegistry
import java.util.concurrent.atomic.AtomicBoolean

/** Standalone scanner. Camera/OCR failures are converted into a safe UI exit instead of a crash. */
class BankCardScannerActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_RETURN_TO_FORM = "return_to_bank_card_form"
        const val EXTRA_CARD = "bank_card"
        const val EXTRA_HOLDER = "bank_holder"
        const val EXTRA_IBAN = "bank_iban"
        private val CARD_REGEX = Regex("(?<!\\d)(?:[0-9۰-۹]{4}[ -]?){3}[0-9۰-۹]{4}(?!\\d)")
        private val IBAN_REGEX = Regex("(?i)(?<![A-Z0-9])IR[0-9۰-۹]{2}(?:[ -]?[0-9۰-۹]){22}(?![0-9])")
    }

    private var recognizer: TextRecognizer? = null
    private val closing = AtomicBoolean(false)
    private lateinit var preview: TextureView
    private var manager: CameraManager? = null
    private var thread: HandlerThread? = null
    private var handler: Handler? = null
    private var camera: CameraDevice? = null
    private var session: CameraCaptureSession? = null
    private var reader: ImageReader? = null
    private var lastCard: String? = null
    private var stable = 0
    private var lastRun = 0L

    private val cameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (isFinishing || isDestroyed) return@registerForActivityResult
        if (granted) preview.surfaceTextureListener = listener else finish()
    }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        try {
            setContentView(R.layout.activity_bank_card_scanner)
            preview = findViewById(R.id.bank_card_camera_preview)
            manager = getSystemService(CameraManager::class.java)
            thread = HandlerThread("BankCardScanner").also { it.start() }
            handler = Handler(thread!!.looper)
            recognizer = runCatching { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }.getOrNull()
        } catch (_: Exception) {
            Toast.makeText(this, "اسکنر کارت در این نسخه در دسترس نیست", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            preview.surfaceTextureListener = listener
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private val listener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, w: Int, h: Int) { openCamera() }
        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, w: Int, h: Int) = Unit
        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit
        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean { closeCamera(); return true }
    }

    private fun openCamera() {
        if (closing.get() || isFinishing || isDestroyed) return
        val cm = manager ?: return fail("دوربین در دسترس نیست")
        val h = handler ?: return fail("اسکنر آماده نیست")
        try {
            val id = cm.cameraIdList.firstOrNull { id ->
                cm.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
            } ?: return fail("دوربین پشت پیدا نشد")
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) return fail("مجوز دوربین داده نشده است")
            reader = ImageReader.newInstance(1280, 720, ImageFormat.YUV_420_888, 2).also { imageReader ->
                imageReader.setOnImageAvailableListener({ analyze(imageReader) }, h)
            }
            cm.openCamera(id, object : CameraDevice.StateCallback() {
                override fun onOpened(d: CameraDevice) { if (closing.get()) d.close() else { camera = d; bind(d) } }
                override fun onDisconnected(d: CameraDevice) { d.close(); camera = null }
                override fun onError(d: CameraDevice, error: Int) { d.close(); camera = null; fail("خطا در دسترسی به دوربین") }
            }, h)
        } catch (_: SecurityException) { fail("مجوز دوربین داده نشده است") }
        catch (_: Exception) { fail("باز کردن دوربین ممکن نیست") }
    }

    private fun bind(d: CameraDevice) {
        if (closing.get() || isFinishing || isDestroyed) { d.close(); return }
        val texture = preview.surfaceTexture ?: return fail("پیش‌نمایش دوربین آماده نیست")
        val h = handler ?: return fail("اسکنر آماده نیست")
        texture.setDefaultBufferSize(1280, 720)
        val previewSurface = Surface(texture)
        val readerSurface = reader?.surface ?: return fail("پردازش تصویر آماده نیست")
        try {
            d.createCaptureSession(listOf(previewSurface, readerSurface), object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(s: CameraCaptureSession) {
                    if (closing.get()) { s.close(); return }
                    session = s
                    try {
                        val request = d.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                            addTarget(previewSurface)
                            addTarget(readerSurface)
                            set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                        }.build()
                        s.setRepeatingRequest(request, null, h)
                    } catch (_: Exception) { fail("دوربین آماده نشد") }
                }
                override fun onConfigureFailed(s: CameraCaptureSession) { s.close(); fail("دوربین آماده نشد") }
            }, h)
        } catch (_: Exception) { fail("دوربین آماده نشد") }
    }

    private fun analyze(r: ImageReader) {
        if (closing.get()) return
        val image = try { r.acquireLatestImage() } catch (_: Exception) { null } ?: return
        val now = SystemClock.elapsedRealtime()
        if (now - lastRun < 400L) { image.close(); return }
        lastRun = now
        val detector = recognizer
        if (detector == null) { image.close(); return }
        detector.process(InputImage.fromMediaImage(image, 0)).addOnSuccessListener { result ->
            if (closing.get()) return@addOnSuccessListener
            val normalized = normalizeDigits(result.text)
            val card = CARD_REGEX.find(normalized)?.value?.let(::normalizeCard)?.takeIf { IranianBankRegistry.isValidCardNumber(it) }
                ?: run { stable = 0; lastCard = null; return@addOnSuccessListener }
            stable = if (card == lastCard) stable + 1 else 1
            lastCard = card
            if (stable >= 2 && IranianBankRegistry.findByCard(card) != null && closing.compareAndSet(false, true)) {
                val holder = extractHolder(result.text)
                val iban = IBAN_REGEX.find(normalized)?.value?.let(::normalizeIban).orEmpty()
                setResult(Activity.RESULT_OK, Intent().apply {
                    putExtra(EXTRA_CARD, card)
                    putExtra(EXTRA_HOLDER, holder)
                    putExtra(EXTRA_IBAN, iban)
                })
                finish()
            }
        }.addOnCompleteListener { image.close() }
    }

    private fun normalizeDigits(value: String) = buildString(value.length) {
        value.forEach { c ->
            append(when (c) {
                in '۰'..'۹' -> ('0'.code + c.code - '۰'.code).toChar()
                in '٠'..'٩' -> ('0'.code + c.code - '٠'.code).toChar()
                else -> c
            })
        }
    }

    private fun normalizeCard(value: String) = normalizeDigits(value).filter(Char::isDigit)
    private fun normalizeIban(value: String) = normalizeDigits(value).replace(" ", "").replace("-", "").uppercase()
    private fun extractHolder(text: String): String = Regex("(?i)(?:به\\s*نام|بنام|نام\\s*صاحب\\s*کارت|صاحب\\s*کارت|name)\\s*[:：-]?\\s*([\\p{L}][\\p{L} ._-]{2,39})")
        .find(text)?.groupValues?.getOrNull(1)?.trim().orEmpty()

    private fun fail(message: String) {
        if (isFinishing || isDestroyed) return
        runOnUiThread {
            if (!isFinishing && !isDestroyed) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun closeCamera() {
        closing.set(true)
        try { session?.stopRepeating() } catch (_: Exception) { }
        try { session?.close() } catch (_: Exception) { }
        try { camera?.close() } catch (_: Exception) { }
        try { reader?.close() } catch (_: Exception) { }
        session = null
        camera = null
        reader = null
    }

    override fun onDestroy() {
        closeCamera()
        thread?.quitSafely()
        thread = null
        handler = null
        recognizer?.close()
        recognizer = null
        super.onDestroy()
    }
}
