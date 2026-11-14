package com.letsdrive.app.data.share

internal object InviteShareMessages {
    fun buildInviteText(inviteCode: String, destinationName: String, inviteLink: String): String {
        val code = QrCodeEncoder.normalizeInviteCode(inviteCode)
        return "Entre na nossa rota pelo Let's Drive rumo a $destinationName!\n" +
            "Código da sala: $code\n" +
            "Link: $inviteLink"
    }
}

expect object PlatformShareAndQr {
    fun shareTripInvite(inviteCode: String, destinationName: String, inviteLink: String)
    fun scanOrReadInviteCode(): String?
    fun consumeInitialInviteCodeFromLaunch(): String?
}

