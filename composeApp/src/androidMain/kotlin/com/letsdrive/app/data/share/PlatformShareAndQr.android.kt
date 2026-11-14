package com.letsdrive.app.data.share

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import com.letsdrive.app.data.location.AndroidLocationHolder

actual object PlatformShareAndQr {

    actual fun shareTripInvite(inviteCode: String, destinationName: String, inviteLink: String) {
        val ctx = AndroidLocationHolder.appContext ?: return
        val normalizedCode = QrCodeEncoder.normalizeInviteCode(inviteCode)
        val shareText = InviteShareMessages.buildInviteText(normalizedCode, destinationName, inviteLink)

        runCatching {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Convite Let's Drive ($normalizedCode)")
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            val chooser = Intent.createChooser(sendIntent, "Compartilhar convite da rota").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            ctx.startActivity(chooser)
        }
    }

    actual fun scanOrReadInviteCode(): String? {
        val ctx = AndroidLocationHolder.appContext ?: return null
        return runCatching {
            val clipboard = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                ?: return@runCatching null
            val clipText = clipboard.primaryClip
                ?.takeIf { it.itemCount > 0 }
                ?.getItemAt(0)
                ?.coerceToText(ctx)
                ?.toString()
            QrCodeEncoder.extractInviteCode(clipText)
        }.getOrNull()
    }

    actual fun consumeInitialInviteCodeFromLaunch(): String? = null
}

