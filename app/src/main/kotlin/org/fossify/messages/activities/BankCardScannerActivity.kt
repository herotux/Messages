package org.fossify.messages.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.*
import android.view.Surface
import android.view.TextureView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import org.fossify.messages.helpers.BankAccountsFeature
import org.fossify.messages.helpers.IranianBankRegistry
import java.util.concurrent.atomic.AtomicBoolean

class BankCardScannerActivity : SimpleActivity() {
    companion object {
        const val EXTRA_RETURN_TO_FORM = "return_to_bank_card_form"
        const val EXTRA_CARD = "bank_card"
        const val EXTRA_HOLDER = "bank_holder"
        const val EXTRA_IBAN = "bank_iban"
    }

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val closing = AtomicBoolean(false)
    private lateinit var preview: TextureView
    private lateinit var manager: CameraManager
    private lateinit var thread: HandlerThread
    private lateinit var handler: Handler
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
        setContentView(R.layout.activity_bank_card_scanner)
        preview = findViewById(R.id.bank_card_camera_preview)
        manager = getSystemService(CameraManager::class.java)
        thread = HandlerThread("BankCardScanner").also { it.start() }
        handler = Handler(thread.looper)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) preview.surfaceTextureListener = listener
        else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private val listener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, w: Int, h: Int) { openCamera() }
        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, w: Int, h: Int) = Unit
        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit
        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean { closeCamera(); return true }
    }

    private fun openCamera() {
        if (closing.get() || isFinishing || isDestroyed) return
        try {
            val id = manager.cameraIdList.firstOrNull { manager.getCameraCharacteristics(it).get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK } ?: return fail("دوربین پشت پیدا نشد")
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) return
            reader = ImageReader.newInstance(1280, 720, ImageFormat.YUV_420_888, 2).also { it.setOnImageAvailableListener({ analyze(it) }, handler) }
            manager.openCamera(id, object : CameraDevice.StateCallback() {
                override fun onOpened(d: CameraDevice) { if (closing.get()) { d.close(); return }; camera = d; bind(d) }
                override fun onDisconnected(d: CameraDevice) { d.close(); camera = null }
                override fun onError(d: CameraDevice, error: Int) { d.close(); camera = null; fail("خطا در دسترسی به دوربین") }
            }, handler)
        } catch (_: SecurityException) { fail("مجوز دوربین داده نشده است") }
        catch (_: Exception) { fail("باز کردن دوربین ممکن نیست") }
    }

    private fun bind(d: CameraDevice) {
        if (closing.get() || isFinishing || isDestroyed) { d.close(); return }
        val texture = preview.surfaceTexture ?: return
        val previewSurface = Surface(texture)
        val readerSurface = reader?.surface ?: return
        texture.setDefaultBufferSize(1280, 720)
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
                        s.setRepeatingRequest(request, null, handler)
                    } catch (_: CameraAccessException) { fail("دوربین آماده نشد") }
                }
                override fun onConfigureFailed(s: CameraCaptureSession) { s.close(); fail("دوربین آماده نشد") }
            }, handler)
        } catch (_: Exception) { fail("دوربین آماده نشد") }
    }

    private fun analyze(r: ImageReader) {
        if (closing.get()) return
        val image = try { r.acquireLatestImage() } catch (_: Exception) { null } ?: return
        val now = SystemClock.elapsedRealtime()
        if (now - lastRun < 350L) { image.close(); return }
        lastRun = now
        recognizer.process(InputImage.fromMediaImage(image, 0)).addOnSuccessListener { result ->
            if (closing.get()) return@addOnSuccessListener
            val card = BankAccountsFeature.cardRegexForScanner(result.text) ?: run { lastCard = null; stable = 0; return@addOnSuccessListener }
            stable = if (card == lastCard) stable + 1 else 1
            lastCard = card
            if (stable >= 2) {
                val bank = IranianBankRegistry.findByCard(card) ?: run { stable = 0; return@addOnSuccessListener }
                if (!closing.compareAndSet(false, true)) return@addOnSuccessListener
                val holder = BankAccountsFeature.extractHolderForScanner(result.text)
                val iban = BankAccountsFeature.extractIbanForScanner(result.text)
                if (intent.getBooleanExtra(EXTRA_RETURN_TO_FORM, false)) {
                    setResult(RESULT_OK, Intent().apply { putExtra(EXTRA_CARD, card); putExtra(EXTRA_HOLDER, holder); putExtra(EXTRA_IBAN, iban) })
                    finish()
                } else finish()
            }
        }.addOnCompleteListener { image.close() }
    }

    private fun fail(message: String) {
        if (isFinishing || isDestroyed) return
        runOnUiThread { if (!isFinishing && !isDestroyed) { Toast.makeText(this, message, Toast.LENGTH_SHORT).show(); finish() } }
    }

    private fun closeCamera() {
        closing.set(true)
        try { session?.stopRepeating() } catch (_: Exception) { }
        try { session?.close(); camera?.close(); reader?.close() } catch (_: Exception) { }
        session = null; camera = null; reader = null
    }

    override fun onDestroy() {
        closeCamera()
        if (::thread.isInitialized) thread.quitSafely()
        recognizer.close()
        super.onDestroy()
    }
}