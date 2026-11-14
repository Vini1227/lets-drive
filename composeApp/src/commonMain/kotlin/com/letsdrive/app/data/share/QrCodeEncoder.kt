package com.letsdrive.app.data.share

import kotlin.math.abs
import kotlin.math.max

/**
 * Gerador real de QR Code padrão ISO/IEC 18004 (Versão 2-L: matriz 25x25, 34 bytes de dados + 10 ECC Reed-Solomon)
 * e extrator inteligente de códigos de convite a partir de texto, URL ou leitura de QR Code.
 */
object QrCodeEncoder {

    private const val SIZE = 25
    private const val DATA_CODEWORDS = 34
    private const val EC_CODEWORDS = 10
    private const val MAX_PAYLOAD_BYTES = 32
    private const val FORMAT_L_MASK_0 = 0x77C4 // BCH(15,5) para ECC=L (01) + Mask=0 (000) XOR 0x5412

    private val gfExp = IntArray(512)
    private val gfLog = IntArray(256)

    init {
        var x = 1
        for (i in 0 until 255) {
            gfExp[i] = x
            gfLog[x] = i
            x = x shl 1
            if ((x and 0x100) != 0) {
                x = x xor 0x11D
            }
        }
        for (i in 255 until 512) {
            gfExp[i] = gfExp[i - 255]
        }
    }

    fun buildQrPayload(inviteCode: String): String {
        val normalized = normalizeInviteCode(inviteCode)
        val urlCandidate = "https://letsdrive.app/$normalized"
        return if (urlCandidate.encodeToByteArray().size <= MAX_PAYLOAD_BYTES) {
            urlCandidate
        } else {
            normalized
        }
    }

    fun normalizeInviteCode(inviteCode: String): String {
        val cleaned = inviteCode.trim().uppercase()
        return when {
            cleaned.isBlank() -> "#AB12CD34"
            cleaned.startsWith("#") -> cleaned
            else -> "#$cleaned"
        }
    }

    fun extractInviteCode(rawInput: String?): String? {
        val trimmed = rawInput?.trim().orEmpty()
        if (trimmed.isBlank()) return null
        val upper = trimmed.uppercase()

        val urlRegex = Regex("""(?:[?&](?:JOIN|CODE)=|/JOIN/|LETSDRIVE\.APP/)#?([A-Z0-9]{4,10})""")
        urlRegex.find(upper)?.groupValues?.getOrNull(1)?.let { code ->
            return "#$code"
        }

        val hashRegex = Regex("""#([A-Z0-9]{4,10})\b""")
        hashRegex.find(upper)?.groupValues?.getOrNull(1)?.let { code ->
            return "#$code"
        }

        val directToken = Regex("""^#?([A-Z0-9]{4,10})$""")
        directToken.matchEntire(upper)?.groupValues?.getOrNull(1)?.let { code ->
            return "#$code"
        }

        return null
    }

    fun encodeVersion2EccL(payload: String): Array<BooleanArray> {
        val rawBytes = payload.encodeToByteArray().let {
            if (it.size > MAX_PAYLOAD_BYTES) it.copyOf(MAX_PAYLOAD_BYTES) else it
        }

        val dataCodewords = buildDataCodewords(rawBytes)
        val ecCodewords = computeReedSolomon(dataCodewords)
        val allCodewords = IntArray(DATA_CODEWORDS + EC_CODEWORDS) { idx ->
            if (idx < DATA_CODEWORDS) dataCodewords[idx] else ecCodewords[idx - DATA_CODEWORDS]
        }

        val modules = Array(SIZE) { BooleanArray(SIZE) }
        val isFunction = Array(SIZE) { BooleanArray(SIZE) }

        placeFinderPattern(modules, isFunction, 0, 0)
        placeFinderPattern(modules, isFunction, 0, SIZE - 7)
        placeFinderPattern(modules, isFunction, SIZE - 7, 0)
        placeAlignmentPattern(modules, isFunction, 18, 18)
        placeTimingPatterns(modules, isFunction)
        reserveFormatInformation(isFunction)

        // Módulo escuro fixo em (4 * version + 9, 8) = (17, 8)
        setFunctionModule(modules, isFunction, 17, 8, true)

        placeDataAndMask0(modules, isFunction, allCodewords)
        writeFormatInformation(modules, FORMAT_L_MASK_0)

        return modules
    }

    private fun buildDataCodewords(bytes: ByteArray): IntArray {
        val bits = ArrayList<Int>(DATA_CODEWORDS * 8)

        fun appendBits(value: Int, length: Int) {
            for (i in (length - 1) downTo 0) {
                bits.add((value ushr i) and 1)
            }
        }

        // Modo Byte (0100) + tamanho em 8 bits (Versão 2)
        appendBits(0b0100, 4)
        appendBits(bytes.size, 8)
        for (b in bytes) {
            appendBits(b.toInt() and 0xFF, 8)
        }

        val totalDataBits = DATA_CODEWORDS * 8
        val terminatorLen = minOf(4, totalDataBits - bits.size)
        repeat(terminatorLen) { bits.add(0) }
        while (bits.size % 8 != 0) {
            bits.add(0)
        }

        val codewords = IntArray(DATA_CODEWORDS)
        val byteCount = bits.size / 8
        for (i in 0 until byteCount) {
            var v = 0
            for (b in 0 until 8) {
                v = (v shl 1) or bits[i * 8 + b]
            }
            codewords[i] = v
        }
        val padBytes = intArrayOf(0xEC, 0x11)
        var padIdx = 0
        for (i in byteCount until DATA_CODEWORDS) {
            codewords[i] = padBytes[padIdx % 2]
            padIdx++
        }
        return codewords
    }

    private fun gfMul(a: Int, b: Int): Int {
        if (a == 0 || b == 0) return 0
        return gfExp[gfLog[a] + gfLog[b]]
    }

    private fun computeReedSolomon(data: IntArray): IntArray {
        var gen = intArrayOf(1)
        for (i in 0 until EC_CODEWORDS) {
            val next = IntArray(gen.size + 1)
            val root = gfExp[i]
            for (j in gen.indices) {
                next[j] = next[j] xor gen[j]
                next[j + 1] = next[j + 1] xor gfMul(gen[j], root)
            }
            gen = next
        }

        val buffer = IntArray(data.size + EC_CODEWORDS)
        for (i in data.indices) {
            buffer[i] = data[i]
        }

        for (i in data.indices) {
            val factor = buffer[i]
            if (factor != 0) {
                for (j in gen.indices) {
                    buffer[i + j] = buffer[i + j] xor gfMul(gen[j], factor)
                }
            }
        }

        return IntArray(EC_CODEWORDS) { idx -> buffer[data.size + idx] }
    }

    private fun setFunctionModule(
        modules: Array<BooleanArray>,
        isFunction: Array<BooleanArray>,
        r: Int,
        c: Int,
        dark: Boolean
    ) {
        if (r in 0 until SIZE && c in 0 until SIZE) {
            modules[r][c] = dark
            isFunction[r][c] = true
        }
    }

    private fun placeFinderPattern(
        modules: Array<BooleanArray>,
        isFunction: Array<BooleanArray>,
        top: Int,
        left: Int
    ) {
        for (dr in -1..7) {
            for (dc in -1..7) {
                val r = top + dr
                val c = left + dc
                if (r !in 0 until SIZE || c !in 0 until SIZE) continue
                val dark = dr in 0..6 && dc in 0..6 &&
                    (dr == 0 || dr == 6 || dc == 0 || dc == 6 || (dr in 2..4 && dc in 2..4))
                setFunctionModule(modules, isFunction, r, c, dark)
            }
        }
    }

    private fun placeAlignmentPattern(
        modules: Array<BooleanArray>,
        isFunction: Array<BooleanArray>,
        centerR: Int,
        centerC: Int
    ) {
        for (dr in -2..2) {
            for (dc in -2..2) {
                val dark = max(abs(dr), abs(dc)) != 1
                setFunctionModule(modules, isFunction, centerR + dr, centerC + dc, dark)
            }
        }
    }

    private fun placeTimingPatterns(
        modules: Array<BooleanArray>,
        isFunction: Array<BooleanArray>
    ) {
        for (i in 8 until SIZE - 8) {
            val dark = i % 2 == 0
            setFunctionModule(modules, isFunction, 6, i, dark)
            setFunctionModule(modules, isFunction, i, 6, dark)
        }
    }

    private fun reserveFormatInformation(isFunction: Array<BooleanArray>) {
        for (i in 0..8) {
            if (i != 6) {
                isFunction[8][i] = true
                isFunction[i][8] = true
            }
        }
        for (i in (SIZE - 8) until SIZE) {
            isFunction[8][i] = true
        }
        for (i in (SIZE - 7) until SIZE) {
            isFunction[i][8] = true
        }
    }

    private fun placeDataAndMask0(
        modules: Array<BooleanArray>,
        isFunction: Array<BooleanArray>,
        codewords: IntArray
    ) {
        val totalBits = codewords.size * 8
        var bitIdx = 0
        var upward = true
        var rightCol = SIZE - 1

        while (rightCol >= 1) {
            if (rightCol == 6) {
                rightCol--
            }
            for (step in 0 until SIZE) {
                val r = if (upward) SIZE - 1 - step else step
                for (offset in 0..1) {
                    val c = rightCol - offset
                    if (!isFunction[r][c]) {
                        val bit = if (bitIdx < totalBits) {
                            val cw = codewords[bitIdx / 8]
                            val shift = 7 - (bitIdx % 8)
                            ((cw ushr shift) and 1) == 1
                        } else {
                            false
                        }
                        bitIdx++
                        // Máscara 0: inverte bit quando (r + c) % 2 == 0
                        modules[r][c] = if ((r + c) % 2 == 0) !bit else bit
                    }
                }
            }
            upward = !upward
            rightCol -= 2
        }
    }

    private fun writeFormatInformation(
        modules: Array<BooleanArray>,
        formatBits: Int
    ) {
        fun bitAt(pos: Int): Boolean = ((formatBits ushr pos) and 1) == 1

        // Cópia 1 ao redor do finder superior esquerdo (bit 14 -> bit 0)
        for (i in 0..5) modules[8][i] = bitAt(14 - i)
        modules[8][7] = bitAt(8)
        modules[8][8] = bitAt(7)
        modules[7][8] = bitAt(6)
        for (i in 0..5) modules[5 - i][8] = bitAt(5 - i)

        // Cópia 2 dividida entre inferior esquerdo (bits 14..8) e superior direito (bits 7..0)
        for (i in 0..6) modules[SIZE - 1 - i][8] = bitAt(14 - i)
        for (i in 0..7) modules[8][SIZE - 8 + i] = bitAt(7 - i)
    }
}

