package org.fossify.messages.activities

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import org.fossify.commons.activities.SimpleActivity
import org.fossify.messages.extensions.getMessagesDB
import org.fossify.messages.helpers.BankAccountsFeature
import org.fossify.messages.helpers.IranianBankRegistry
import org.fossify.messages.models.BankAccount

class BankCardScannerActivity : SimpleActivity() {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        launchCamera()
    }

    private fun launchCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) == null) {
            Toast.makeText(this, "Camera is not available", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        startActivityForResult(intent, REQUEST_CAMERA)
    }

    @Deprecated("Camera result API is retained for compatibility with the existing app architecture")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_CAMERA) return
        if (resultCode != Activity.RESULT_OK) {
            finish()
            return
        }
        val bitmap = data?.extras?.get("data") as? Bitmap
        if (bitmap == null) {
            Toast.makeText(this, "Could not read the card image", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        recognize(bitmap)
    }

    private fun recognize(bitmap: Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)
        recognizer.process(image)
            .addOnSuccessListener { result ->
                val text = result.text
                val card = BankAccountsFeature.cardRegexForScanner(text)
                if (card == null) {
                    Toast.makeText(this, "Card number was not detected", Toast.LENGTH_SHORT).show()
                    finish()
                    return@addOnSuccessListener
                }
                val bank = IranianBankRegistry.findByCard(card)
                if (bank == null) {
                    Toast.makeText(this, "Bank could not be identified", Toast.LENGTH_SHORT).show()
                    finish()
                    return@addOnSuccessListener
                }
                val holder = BankAccountsFeature.extractHolderForScanner(text)
                val iban = BankAccountsFeature.extractIbanForScanner(text)
                save(card, bank.id.name, holder, iban)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Card scan failed", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun save(card: String, bankId: String, holder: String, iban: String) {
        val account = BankAccount(
            bankId = bankId,
            cardNumber = card,
            holderName = holder,
            iban = iban
        )
        Thread {
            try {
                getMessagesDB().BankAccountsDao().insert(account)
                runOnUiThread {
                    Toast.makeText(this, "Bank card saved", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (_: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Could not save bank card", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }.start()
    }

    override fun onDestroy() {
        recognizer.close()
        super.onDestroy()
    }

    companion object {
        const val REQUEST_CAMERA = 4107
    }
}
