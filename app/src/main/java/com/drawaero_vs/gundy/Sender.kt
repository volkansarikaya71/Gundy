package com.drawaero_vs.gundy2

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class Sender(private val context: Context) {

    fun send(message: String) {

        if (message.isBlank()) {
            Toast.makeText(context, "Lütfen bir mesaj girin.", Toast.LENGTH_SHORT).show()
            return
        }

        val request = MessageRequest(
            device_id = "4c21aec5-2086-4f4c-9179-bce7b92d7455",
            text = message
        )

        if (context is LifecycleOwner) {
            context.lifecycleScope.launch {
                try {
                    val response = RetrofitClient.apiService.sendMessage(request)
                    if (response.success) {
                        Toast.makeText(context, "✅ Mesaj gönderildi", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "❌ Gönderilemedi", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "🚫 Hata oluştu", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(context, "Hatalı yaşam döngüsü", Toast.LENGTH_SHORT).show()
        }
    }
}


