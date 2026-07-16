package com.whatsappsync.app.share

import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object ShareImportCoordinator {
    private val _incoming = MutableSharedFlow<List<Uri>>(extraBufferCapacity = 1)
    val incoming = _incoming.asSharedFlow()

    fun accept(intent: Intent?) {
        if (intent?.action != Intent.ACTION_SEND && intent?.action != Intent.ACTION_SEND_MULTIPLE) return
        val uris = when (intent.action) {
            Intent.ACTION_SEND_MULTIPLE -> intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java).orEmpty()
            else -> listOfNotNull(intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java))
        }
        if (uris.isNotEmpty()) _incoming.tryEmit(uris)
    }
}
