package com.example.finapp.security

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ClipboardHelper(
    private val context: Context,
    private val scope: CoroutineScope,
) {
    fun copyWithAutoClear(label: String, text: String, clearAfterSeconds: Int = 30) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
        scope.launch {
            delay(clearAfterSeconds * 1000L)
            if (clipboard.primaryClip?.getItemAt(0)?.text?.toString() == text) {
                clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
            }
        }
    }
}
