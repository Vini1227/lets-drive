package com.letsdrive.app.data.share

private fun jsShareOrCopy(title: String, text: String, url: String): Unit = js(
    "(() => { try { if (typeof navigator !== 'undefined' && navigator.share) { navigator.share({ title: title, text: text, url: url }).catch(function() { if (navigator.clipboard && navigator.clipboard.writeText) { navigator.clipboard.writeText(text); } }); } else if (typeof navigator !== 'undefined' && navigator.clipboard && navigator.clipboard.writeText) { navigator.clipboard.writeText(text); } } catch (e) {} })()"
)

private fun jsPromptInviteCode(): String = js(
    "(() => { try { return window.prompt('Aponte a câmera do celular para o QR Code ou cole aqui o código/link do convite (ex: #AB123456):') || ''; } catch (e) { return ''; } })()"
)

private fun jsConsumeLaunchUrlInvite(): String = js(
    "(() => { try { return (window.location.search || '') + ' ' + (window.location.hash || ''); } catch (e) { return ''; } })()"
)

actual object PlatformShareAndQr {
    private var launchConsumed = false

    actual fun shareTripInvite(inviteCode: String, destinationName: String, inviteLink: String) {
        val normalizedCode = QrCodeEncoder.normalizeInviteCode(inviteCode)
        val shareText = InviteShareMessages.buildInviteText(normalizedCode, destinationName, inviteLink)
        jsShareOrCopy(
            title = "Convite Let's Drive ($normalizedCode)",
            text = shareText,
            url = inviteLink
        )
    }

    actual fun scanOrReadInviteCode(): String? {
        val entered = jsPromptInviteCode()
        return QrCodeEncoder.extractInviteCode(entered)
    }

    actual fun consumeInitialInviteCodeFromLaunch(): String? {
        if (launchConsumed) return null
        launchConsumed = true
        val urlText = jsConsumeLaunchUrlInvite()
        return QrCodeEncoder.extractInviteCode(urlText)
    }
}

