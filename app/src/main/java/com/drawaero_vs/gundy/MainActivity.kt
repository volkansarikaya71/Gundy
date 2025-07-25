package com.drawaero_vs.gundy2

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.drawaero_vs.gundy2.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var selecttingImage: Uri? = null
    private var selecttingBitMap: Bitmap? = null
    private lateinit var permissionsLauncher: ActivityResultLauncher<String>
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private var cameraImageUri: Uri? = null
    private val sender = Sender(this)


    private lateinit var mikrofon: Mikrofon
    private var pendingPermissionAction: (() -> Unit)? = null



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)

            registerLauncher()
            registerCameraLauncher()
            mikrofon = Mikrofon(this)

            if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
                val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                if (!sharedText.isNullOrEmpty()) {
                    sender.send(sharedText)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Başlatma sırasında hata: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    fun showImageSourceMenu(view: View) {
        try {
            val dialog = android.app.AlertDialog.Builder(this)
                .setTitle("Gundy'e Nasıl Resim Yollayacağını Seç")
                .setItems(arrayOf("📷 Gundy'e Resim Yollamak İçin Kamerayı Aç", "🖼️ Gundy'e Resim Yollamak İçin Galeriyi Aç")) { _, which ->
                    when (which) {
                        0 -> {
                            val permission = Manifest.permission.CAMERA
                            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                                pendingPermissionAction = { openCamera() }
                                permissionsLauncher.launch(permission)
                            } else {
                                openCamera()
                            }
                        }
                        1 -> openGallery(view)
                    }
                }
                .create()
            dialog.show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Menü açılırken hata oluştu: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openGallery(view: View) {
        try {
            val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_IMAGES
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }

            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                    Snackbar.make(view, "Galeriye erişmek için izin gerekli", Snackbar.LENGTH_INDEFINITE)
                        .setAction("İzin Ver") {
                            pendingPermissionAction = {
                                val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                                activityResultLauncher.launch(intent)
                            }
                            permissionsLauncher.launch(permission)
                        }.show()
                } else {
                    pendingPermissionAction = {
                        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                        activityResultLauncher.launch(intent)
                    }
                    permissionsLauncher.launch(permission)
                }
            } else {
                val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Galeri açılırken hata oluştu: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openCamera() {
        try {
            val imageFile = createImageFile()
            cameraImageUri = FileProvider.getUriForFile(
                this,
                "$packageName.fileprovider",
                imageFile
            )

            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri)
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            cameraLauncher.launch(cameraIntent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Kamera açılırken hata oluştu: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createImageFile(): File {
        return try {
            val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val storageDir: File = cacheDir
            File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Dosya oluşturulamadı: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            // fallback boş dosya
            File("")
        }
    }

    private fun registerLauncher() {
        activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            try {
                if (result.resultCode == RESULT_OK && result.data != null) {
                    selecttingImage = result.data!!.data
                    val bitmap = if (Build.VERSION.SDK_INT >= 28) {
                        val source = ImageDecoder.createSource(contentResolver, selecttingImage!!)
                        ImageDecoder.decodeBitmap(source)
                    } else {
                        MediaStore.Images.Media.getBitmap(contentResolver, selecttingImage)
                    }
                    selecttingBitMap = bitmap
                    binding.imageView.scaleType = ImageView.ScaleType.FIT_XY
                    binding.imageView.setImageBitmap(bitmap)
                    uploadSelectedImage()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Resim yüklenirken hata: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }

        permissionsLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
            if (result) {
                pendingPermissionAction?.invoke()
                pendingPermissionAction = null
            } else {
                Toast.makeText(this, "İzin verilmedi", Toast.LENGTH_SHORT).show()
                pendingPermissionAction = null
            }
        }
    }

    private fun registerCameraLauncher() {
        cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            try {
                if (result.resultCode == RESULT_OK && cameraImageUri != null) {
                    val bitmap = if (Build.VERSION.SDK_INT >= 28) {
                        val source = ImageDecoder.createSource(contentResolver, cameraImageUri!!)
                        ImageDecoder.decodeBitmap(source)
                    } else {
                        MediaStore.Images.Media.getBitmap(contentResolver, cameraImageUri)
                    }
                    selecttingBitMap = bitmap
                    binding.imageView.scaleType = ImageView.ScaleType.FIT_XY
                    binding.imageView.setImageBitmap(bitmap)
                    uploadSelectedImage()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Kamera sonrası işleme hata: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }



    fun writeSender(view: View) {
        try {
            val originalText = binding.EditText.text.toString()
            val cleanedText = originalText
                .replace(Regex("[çÇ]"), "c")
                .replace(Regex("[ğĞ]"), "g")
                .replace(Regex("[ıI]"), "i")
                .replace("İ", "I") // özel büyük i
                .replace(Regex("[öÖ]"), "o")
                .replace(Regex("[şŞ]"), "s")
                .replace(Regex("[üÜ]"), "u")

            sender.send(cleanedText)
            binding.EditText.text.clear()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Mesaj gönderilirken hata: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }





    private fun uploadSelectedImage() {
        try {
            if (selecttingBitMap == null) {
                Toast.makeText(this, "Lütfen önce bir resim seçin.", Toast.LENGTH_SHORT).show()
                return
            }

            val file = createTempFile("upload_image", ".jpg", cacheDir)
            file.outputStream().use { fos ->
                selecttingBitMap!!.compress(Bitmap.CompressFormat.JPEG, 90, fos)
            }

            val imageRequestBody = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val imagePart = MultipartBody.Part.createFormData("file", file.name, imageRequestBody) // API parametre adıyla uyumlu olmalı

            val deviceId = "4c21aec5-2086-4f4c-9179-bce7b92d7455"
            val deviceIdRequestBody = deviceId.toRequestBody("text/plain".toMediaTypeOrNull())

            RetrofitClient.apiService.uploadImage(deviceIdRequestBody, imagePart)
                .enqueue(object : retrofit2.Callback<ApiResponse> {
                    override fun onResponse(call: Call<ApiResponse>, response: retrofit2.Response<ApiResponse>) {
                        if (response.isSuccessful) {
                            val apiResponse = response.body()
                            if (apiResponse != null && apiResponse.success) {
                                Toast.makeText(this@MainActivity, apiResponse.message, Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this@MainActivity, "Sunucudan boş veya başarısız yanıt geldi", Toast.LENGTH_SHORT).show()
                                println("YANIT BODY NULL MU: ${response.body() == null}")
                            }
                        } else {
                            val error = response.errorBody()?.string()
                            Toast.makeText(this@MainActivity, "Sunucu hatası: ${response.code()}", Toast.LENGTH_SHORT).show()
                            println("ERROR BODY: $error")
                        }
                    }

                    override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                        Toast.makeText(this@MainActivity, "Hata: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
                        println("FAILURE: ${t.localizedMessage}")
                    }
                })
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Beklenmeyen hata: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }



    fun youtubeBtn(view: View) {
        val context = view.context
        val packageName = "com.google.android.youtube"

        try {
            // YouTube uygulaması yüklü mü kontrol et
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                // YouTube uygulaması yüklü, direkt aç
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } else {
                // YouTube yüklü değil, Play Store'a yönlendir
                openPlayStore(context, packageName)
            }
        } catch (e: Exception) {
            // Hata durumunda Play Store'a yönlendir
            openPlayStore(context, packageName)
        }
    }

    private fun openPlayStore(context: Context, packageName: String) {
        try {
            // Play Store uygulamasında aç
            val playStoreIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
            playStoreIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(playStoreIntent)
        } catch (e: ActivityNotFoundException) {
            // Play Store uygulaması yoksa web browser'da aç
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
            webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(webIntent)
        }
    }

    fun heartBtn(view: View)
    {
        sender.send("pheart")
    }

    fun close(view: View)
    {
        sender.send("pclose")
    }

    fun speakToText(view: View) {
        mikrofon.startListening { message ->
            runOnUiThread {
                val limitedMessage = if (message.length > 100) message.substring(0, 100) else message
                binding.EditText.setText(limitedMessage)
                println(limitedMessage)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        mikrofon.onRequestPermissionsResult(requestCode, grantResults)
    }
}