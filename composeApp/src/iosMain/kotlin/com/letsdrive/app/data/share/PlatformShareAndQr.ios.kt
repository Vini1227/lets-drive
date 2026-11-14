package com.letsdrive.app.data.share

import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIPasteboard
import platform.UIKit.UIWindow

actual object PlatformShareAndQr {

    actual fun shareTripInvite(inviteCode: String, destinationName: String, inviteLink: String) {
        val normalizedCode = QrCodeEncoder.normalizeInviteCode(inviteCode)
        val shareText = InviteShareMessages.buildInviteText(normalizedCode, destinationName, inviteLink)

        runCatching {
            val rootVc = UIApplication.sharedApplication.keyWindow?.rootViewController
                ?: (UIApplication.sharedApplication.windows.firstOrNull() as? UIWindow)?.rootViewController
                ?: return

            val controller = UIActivityViewController(
                activityItems = listOf(shareText),
                applicationActivities = null
            )
            rootVc.presentViewController(controller, animated = true, completion = null)
        }
    }

    actual fun scanOrReadInviteCode(): String? {
        return runCatching {
            QrCodeEncoder.extractInviteCode(UIPasteboard.generalPasteboard.string)
        }.getOrNull()
    }

    actual fun consumeInitialInviteCodeFromLaunch(): String? = null
}

