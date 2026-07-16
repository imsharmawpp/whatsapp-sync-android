package com.whatsappsync.app.share

import android.content.Intent
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Retains a share until the Sync ViewModel has consumed it. */
object ShareImportCoordinator {
    private val _incoming = MutableStateFlow<List<Uri>?>(null)
    val incoming = _incoming.asStateFlow()

    fun accept(intent: Intent?) {
        val shareIntent = intent ?: return
        if (shareIntent.action !in setOf(Intent.ACTION_SEND, Intent.ACTION_SEND_MULTIPLE)) return

        val uris = buildList {
            addAll(shareIntent.sharedStreams())
            shareIntent.sharedStream()?.let(::add)
            shareIntent.clipData?.let { clipData ->
                repeat(clipData.itemCount) { index ->
                    clipData.getItemAt(index).uri?.let(::add)
                }
            }
            shareIntent.data?.let(::add)
        }.distinct()

        _incoming.value = uris
    }

    fun consume(uris: List<Uri>) {
        if (_incoming.value == uris) _incoming.value = null
    }

    @Suppress("DEPRECATION")
    private fun Intent.sharedStream(): Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
    } else {
        getParcelableExtra(Intent.EXTRA_STREAM)
    }

    @Suppress("DEPRECATION")
    private fun Intent.sharedStreams(): List<Uri> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java).orEmpty()
    } else {
        getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM).orEmpty()
    }
}
