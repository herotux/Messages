package org.fossify.messages.activities

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.ImageFormat
import android.media.ImageReader
import android.os.*
import android.view.Surface
import android.view.TextureView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import org.fossify.messages.extensions.getMessagesDB
import org.fossify.messages.helpers.BankAccountsFeature
import org.fossify.messages.helpers.IranianBankRegistry
import org.fossify.messages.models.BankAccount
import java.util.concurrent.atomic.AtomicBoolean

class BankCardScannerActivity : SimpleActivity() {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val saving = AtomicBoolean(false)
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

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        setContentView(R.layout.activity_bank_card_scanner)
        preview = findViewById(R.id.bank_card_camera_preview)
        manager = getSystemService(CameraManager::class.java)
        thread = HandlerThread("BankCardScanner").also { it.start() }
        handler = Handler(thread.looper)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            preview.surfaceTextureListener = listener
        } else ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 4108)
    }

    override fun onRequestPermissionsResult(code: Int, permissions: Array<out String>, results: IntArray) {
        super.onRequestPermissionsResult(code, permissions, results)
        if (code == 4108 && results.firstOrNull() == PackageManager.PERMISSION_GRANTED) preview.surfaceTextureListener = listener else finish()
    }

    private val listener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, w: Int, h: Int) = openCamera()
        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, w: Int, h: Int) = Unit
        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit
        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean { closeCamera(); return true }
    }

    private fun openCamera() {
        try {
            val id = manager.cameraIdList.firstOrNull { manager.getCameraCharacteristics(it).get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK } ?: return
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) return
            reader = ImageReader.newInstance(1280, 720, ImageFormat.YUV_420_888, 2).also { it.setOnImageAvailableListener({ analyze(it) }, handler) }
            manager.openCamera(id, object : CameraDevice.StateCallback() {
                override fun onOpened(d: CameraDevice) { camera = d; bind(d) }
                override fun onDisconnected(d: CameraDevice) { d.close(); camera = null }
                override fun onError(d: CameraDevice, error: Int) { d.close(); camera = null; runOnUiThread { Toast.makeText(this@BankCardScannerActivity, "Camera error", Toast.LENGTH_SHORT).show(); finish() } }
            }, handler)
        } catch (_: Exception) { finish() }
    }

    private fun bind(d: CameraDevice) {
        val texture = preview.surfaceTexture ?: return
        val previewSurface = Surface(texture)
        val readerSurface = reader?.surface ?: return
        texture.setDefaultBufferSize(1280, 720)
        d.createCaptureSession(listOf(previewSurface, readerSurface), object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(s: CameraCaptureSession) {
                session = s
                try {
                    val request = d.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                        addTarget(previewSurface); addTarget(readerSurface)
                        set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                    }.build()
                    s.setRepeatingRequest(request, null, handler)
                } catch (_: Exception) { finish() }
            }
            override fun onConfigureFailed(s: CameraCaptureSession) { finish() }
        }, handler)
    }

    private fun analyze(r: ImageReader) {
        val image = r.acquireLatestImage() ?: return
        val now = SystemClock.elapsedRealtime()
        if (saving.get() || now - lastRun < 300L) { image.close(); return }
        lastRun = now
        recognizer.process(InputImage.fromMediaImage(image, 0)).addOnSuccessListener { result ->
            val card = BankAccountsFeature.cardRegexForScanner(result.text)
            if (card == null) { lastCard = null; stable = 0; return@addOnSuccessListener }
            stable = if (card == lastCard) stable + 1 else 1
            lastCard = card
            if (stable >= 2 && saving.compareAndSet(false, true)) {
                val bank = IranianBankRegistry.findByCard(card)
                if (bank == null) { saving.set(false); stable = 0; return@addOnSuccessListener }
                save(card, bank.id.name, BankAccountsFeature.extractHolderForScanner(result.text), BankAccountsFeature.extractIbanForScanner(result.text))
            }
        }.addOnCompleteListener { image.close() }
    }

    private fun save(card: String, bank: String, holder: String, iban: String) {
        Thread {
            try {
                getMessagesDB().BankAccountsDao().insert(BankAccount(bankId = bank, cardNumber = card, holderName = holder, iban = iban))
                runOnUiThread { Toast.makeText(this, "Bank card saved", Toast.LENGTH_SHORT).show(); finish() }
            } catch (_: Exception) { saving.set(false) }
        }.start()
    }

    private fun closeCamera() {
        try { session?.close(); camera?.close(); reader?.close() } catch (_: Exception) { }
        session = null; camera = null; reader = null
    }

    override fun onDestroy() {
        closeCamera(); if (::thread.isInitialized) thread.quitSafely(); recognizer.close(); super.onDestroy()
    }
}
