package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import kotlin.math.abs

@Composable
fun QrCodeCanvas(
    url: String,
    modifier: Modifier = Modifier,
    qrColor: Color = Color(0xFF07090E),
    backgroundColor: Color = Color.White
) {
    val modules = remember(url) { QrCodeEncoder.encode(url) }

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val sizePx = size.minDimension
            val n = modules.size
            val quietZone = 2
            val totalCells = n + quietZone * 2
            val cellSize = sizePx / totalCells.toFloat()

            drawRect(
                color = backgroundColor,
                topLeft = Offset.Zero,
                size = Size(sizePx, sizePx)
            )

            for (r in 0 until n) {
                val row = modules[r]
                for (c in 0 until n) {
                    if (row[c]) {
                        drawRect(
                            color = qrColor,
                            topLeft = Offset((c + quietZone) * cellSize, (r + quietZone) * cellSize),
                            size = Size(cellSize + 0.6f, cellSize + 0.6f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Pure-Kotlin ISO/IEC 18004 QR Code encoder (Byte mode, Error Correction Level M, Versions 1..10).
 * Generates 100% standard scannable QR matrices completely offline with zero external dependencies.
 */
object QrCodeEncoder {

    private data class VersionSpec(
        val version: Int,
        val totalDataCodewords: Int,
        val ecCodewordsPerBlock: Int,
        val group1Blocks: Int,
        val group1DataCodewords: Int,
        val group2Blocks: Int = 0,
        val group2DataCodewords: Int = 0
    )

    // ISO/IEC 18004 Table 9 — Error Correction Level M (15% recovery), Versions 1..10
    private val VERSION_SPECS_M = listOf(
        VersionSpec(1, 16, 10, 1, 16),
        VersionSpec(2, 28, 16, 1, 28),
        VersionSpec(3, 44, 26, 1, 44),
        VersionSpec(4, 64, 18, 2, 32),
        VersionSpec(5, 86, 24, 2, 43),
        VersionSpec(6, 108, 16, 4, 27),
        VersionSpec(7, 124, 18, 4, 31),
        VersionSpec(8, 154, 22, 2, 38, 2, 39),
        VersionSpec(9, 182, 22, 3, 36, 2, 37),
        VersionSpec(10, 216, 26, 4, 43, 1, 44)
    )

    fun encode(content: String): Array<BooleanArray> {
        val rawBytes = content.toByteArray(Charsets.UTF_8)
        val spec = pickVersion(rawBytes.size)
        val clampedBytes = if (rawBytes.size > maxByteCapacity(spec)) {
            rawBytes.copyOf(maxByteCapacity(spec))
        } else {
            rawBytes
        }

        val dataCodewords = encodeDataCodewords(clampedBytes, spec)
        val fullCodewords = addErrorCorrectionAndInterleave(dataCodewords, spec)
        return buildBestQrMatrix(fullCodewords, spec.version)
    }

    private fun maxByteCapacity(spec: VersionSpec): Int {
        val headerBits = 4 + if (spec.version <= 9) 8 else 16
        return (spec.totalDataCodewords * 8 - headerBits) / 8
    }

    private fun pickVersion(byteLength: Int): VersionSpec {
        for (spec in VERSION_SPECS_M) {
            if (byteLength <= maxByteCapacity(spec)) return spec
        }
        return VERSION_SPECS_M.last()
    }

    private fun encodeDataCodewords(bytes: ByteArray, spec: VersionSpec): IntArray {
        val bits = ArrayList<Int>(spec.totalDataCodewords * 8)

        fun appendBits(value: Int, count: Int) {
            for (i in (count - 1) downTo 0) {
                bits.add((value ushr i) and 1)
            }
        }

        // Byte mode indicator: 0100
        appendBits(0b0100, 4)
        // Character count indicator: 8 bits for v1..v9, 16 bits for v10+
        appendBits(bytes.size, if (spec.version <= 9) 8 else 16)
        for (b in bytes) {
            appendBits(b.toInt() and 0xFF, 8)
        }

        val capacityBits = spec.totalDataCodewords * 8
        val terminatorBits = minOf(4, capacityBits - bits.size)
        repeat(terminatorBits) { bits.add(0) }
        while (bits.size % 8 != 0) bits.add(0)

        val codewords = IntArray(spec.totalDataCodewords)
        val byteCount = bits.size / 8
        for (i in 0 until byteCount) {
            var value = 0
            for (bit in 0 until 8) {
                value = (value shl 1) or bits[i * 8 + bit]
            }
            codewords[i] = value
        }

        var padToggle = true
        for (i in byteCount until spec.totalDataCodewords) {
            codewords[i] = if (padToggle) 0xEC else 0x11
            padToggle = !padToggle
        }
        return codewords
    }

    private fun addErrorCorrectionAndInterleave(data: IntArray, spec: VersionSpec): IntArray {
        val totalBlocks = spec.group1Blocks + spec.group2Blocks
        val dataBlocks = Array(totalBlocks) { IntArray(0) }
        val ecBlocks = Array(totalBlocks) { IntArray(0) }
        val divisor = reedSolomonComputeDivisor(spec.ecCodewordsPerBlock)

        var offset = 0
        for (i in 0 until totalBlocks) {
            val blockLen = if (i < spec.group1Blocks) spec.group1DataCodewords else spec.group2DataCodewords
            val block = data.copyOfRange(offset, offset + blockLen)
            dataBlocks[i] = block
            ecBlocks[i] = reedSolomonComputeRemainder(block, divisor)
            offset += blockLen
        }

        val maxDataLen = maxOf(spec.group1DataCodewords, spec.group2DataCodewords)
        val result = ArrayList<Int>(spec.totalDataCodewords + totalBlocks * spec.ecCodewordsPerBlock)
        for (col in 0 until maxDataLen) {
            for (block in dataBlocks) {
                if (col < block.size) result.add(block[col])
            }
        }
        for (col in 0 until spec.ecCodewordsPerBlock) {
            for (ec in ecBlocks) {
                result.add(ec[col])
            }
        }
        return result.toIntArray()
    }

    private fun reedSolomonComputeDivisor(degree: Int): IntArray {
        val result = IntArray(degree)
        result[degree - 1] = 1
        var root = 1
        for (i in 0 until degree) {
            for (j in 0 until degree) {
                result[j] = reedSolomonMultiply(result[j], root)
                if (j + 1 < degree) {
                    result[j] = result[j] xor result[j + 1]
                }
            }
            root = reedSolomonMultiply(root, 0x02)
        }
        return result
    }

    private fun reedSolomonComputeRemainder(data: IntArray, divisor: IntArray): IntArray {
        val result = IntArray(divisor.size)
        for (b in data) {
            val factor = b xor result[0]
            System.arraycopy(result, 1, result, 0, result.size - 1)
            result[result.size - 1] = 0
            for (i in divisor.indices) {
                result[i] = result[i] xor reedSolomonMultiply(divisor[i], factor)
            }
        }
        return result
    }

    private fun reedSolomonMultiply(x: Int, y: Int): Int {
        var z = 0
        for (i in 7 downTo 0) {
            z = (z shl 1) xor ((z ushr 7) * 0x11D)
            z = z xor (((y ushr i) and 1) * x)
        }
        return z
    }

    private fun buildBestQrMatrix(codewords: IntArray, version: Int): Array<BooleanArray> {
        val size = 17 + 4 * version
        val baseModules = Array(size) { BooleanArray(size) }
        val isFunction = Array(size) { BooleanArray(size) }

        drawFunctionPatterns(baseModules, isFunction, version, size)
        placeCodewords(baseModules, isFunction, codewords, size)

        var bestMask = 0
        var minPenalty = Int.MAX_VALUE
        var bestMatrix: Array<BooleanArray>? = null

        for (mask in 0 until 8) {
            val candidate = Array(size) { r -> baseModules[r].clone() }
            applyMask(candidate, isFunction, mask, size)
            drawFormatBits(candidate, mask, size)
            val penalty = computePenaltyScore(candidate, size)
            if (penalty < minPenalty) {
                minPenalty = penalty
                bestMask = mask
                bestMatrix = candidate
            }
        }

        val finalMatrix = bestMatrix ?: baseModules
        drawFormatBits(finalMatrix, bestMask, size)
        return finalMatrix
    }

    private fun drawFunctionPatterns(
        modules: Array<BooleanArray>,
        isFunction: Array<BooleanArray>,
        version: Int,
        size: Int
    ) {
        // Timing patterns
        for (i in 0 until size) {
            setFunctionModule(modules, isFunction, 6, i, i % 2 == 0)
            setFunctionModule(modules, isFunction, i, 6, i % 2 == 0)
        }

        // Three finder patterns + 1px separators
        drawFinderPattern(modules, isFunction, 0, 0, size)
        drawFinderPattern(modules, isFunction, size - 7, 0, size)
        drawFinderPattern(modules, isFunction, 0, size - 7, size)

        // Alignment patterns (v >= 2)
        val alignPos = alignmentPatternPositions(version, size)
        for (i in alignPos.indices) {
            for (j in alignPos.indices) {
                val isCorner = (i == 0 && j == 0) ||
                    (i == 0 && j == alignPos.lastIndex) ||
                    (i == alignPos.lastIndex && j == 0)
                if (!isCorner) {
                    drawAlignmentPattern(modules, isFunction, alignPos[i], alignPos[j])
                }
            }
        }

        // Dark module
        setFunctionModule(modules, isFunction, 4 * version + 9, 8, true)

        // Reserve format information bits
        for (i in 0..8) {
            if (i != 6) {
                setFunctionModule(modules, isFunction, 8, i, false)
                setFunctionModule(modules, isFunction, i, 8, false)
            }
        }
        for (i in 0 until 7) {
            setFunctionModule(modules, isFunction, size - 1 - i, 8, false)
        }
        for (i in 0 until 8) {
            setFunctionModule(modules, isFunction, 8, size - 8 + i, false)
        }

        // Version info (v >= 7)
        if (version >= 7) {
            var rem = version
            repeat(12) {
                rem = (rem shl 1) xor ((rem ushr 11) * 0x1F25)
            }
            val bits = (version shl 12) or rem
            for (i in 0 until 18) {
                val bit = ((bits ushr i) and 1) != 0
                val a = size - 11 + i % 3
                val b = i / 3
                setFunctionModule(modules, isFunction, a, b, bit)
                setFunctionModule(modules, isFunction, b, a, bit)
            }
        }
    }

    private fun setFunctionModule(
        modules: Array<BooleanArray>,
        isFunction: Array<BooleanArray>,
        row: Int,
        col: Int,
        isDark: Boolean
    ) {
        if (row in modules.indices && col in modules.indices) {
            modules[row][col] = isDark
            isFunction[row][col] = true
        }
    }

    private fun drawFinderPattern(
        modules: Array<BooleanArray>,
        isFunction: Array<BooleanArray>,
        topRow: Int,
        leftCol: Int,
        size: Int
    ) {
        for (dr in -1..7) {
            for (dc in -1..7) {
                val r = topRow + dr
                val c = leftCol + dc
                if (r in 0 until size && c in 0 until size) {
                    val dist = maxOf(abs(dr - 3), abs(dc - 3))
                    val dark = dist == 0 || dist == 1 || dist == 3
                    setFunctionModule(modules, isFunction, r, c, dark)
                }
            }
        }
    }

    private fun drawAlignmentPattern(
        modules: Array<BooleanArray>,
        isFunction: Array<BooleanArray>,
        centerRow: Int,
        centerCol: Int
    ) {
        for (dr in -2..2) {
            for (dc in -2..2) {
                val dist = maxOf(abs(dr), abs(dc))
                setFunctionModule(modules, isFunction, centerRow + dr, centerCol + dc, dist != 1)
            }
        }
    }

    private fun alignmentPatternPositions(version: Int, size: Int): IntArray {
        if (version == 1) return IntArray(0)
        val numAlign = version / 7 + 2
        val step = if (version == 32) 26 else ((version * 4 + numAlign * 2 + 1) / (numAlign * 2 - 2)) * 2
        val result = IntArray(numAlign)
        result[0] = 6
        var pos = size - 7
        for (i in numAlign - 1 downTo 1) {
            result[i] = pos
            pos -= step
        }
        return result
    }

    private fun placeCodewords(
        modules: Array<BooleanArray>,
        isFunction: Array<BooleanArray>,
        codewords: IntArray,
        size: Int
    ) {
        var bitIndex = 0
        val totalBits = codewords.size * 8
        var right = size - 1
        while (right >= 1) {
            if (right == 6) right = 5
            for (vert in 0 until size) {
                for (j in 0 until 2) {
                    val c = right - j
                    val upward = ((right + 1) and 2) == 0
                    val r = if (upward) size - 1 - vert else vert
                    if (!isFunction[r][c] && bitIndex < totalBits) {
                        val cw = codewords[bitIndex ushr 3]
                        val bit = ((cw ushr (7 - (bitIndex and 7))) and 1) != 0
                        modules[r][c] = bit
                        bitIndex++
                    }
                }
            }
            right -= 2
        }
    }

    private fun applyMask(
        modules: Array<BooleanArray>,
        isFunction: Array<BooleanArray>,
        mask: Int,
        size: Int
    ) {
        for (r in 0 until size) {
            for (c in 0 until size) {
                if (!isFunction[r][c]) {
                    val invert = when (mask) {
                        0 -> (r + c) % 2 == 0
                        1 -> r % 2 == 0
                        2 -> c % 3 == 0
                        3 -> (r + c) % 3 == 0
                        4 -> (r / 2 + c / 3) % 2 == 0
                        5 -> (r * c) % 2 + (r * c) % 3 == 0
                        6 -> ((r * c) % 2 + (r * c) % 3) % 2 == 0
                        else -> ((r + c) % 2 + (r * c) % 3) % 2 == 0
                    }
                    if (invert) {
                        modules[r][c] = !modules[r][c]
                    }
                }
            }
        }
    }

    private fun drawFormatBits(modules: Array<BooleanArray>, mask: Int, size: Int) {
        // EC Level M = formatBits 0 (00 in binary), combined with 3-bit mask
        val data = (0 shl 3) or mask
        var rem = data
        repeat(10) {
            rem = (rem shl 1) xor ((rem ushr 9) * 0x537)
        }
        val bits = ((data shl 10) or rem) xor 0x5412

        // First copy (top-left): bit 0 at (8,0) .. bit 14 at (0,8)
        for (i in 0..5) {
            modules[8][i] = ((bits ushr i) and 1) != 0
        }
        modules[8][7] = ((bits ushr 6) and 1) != 0
        modules[8][8] = ((bits ushr 7) and 1) != 0
        modules[7][8] = ((bits ushr 8) and 1) != 0
        for (i in 9..14) {
            modules[14 - i][8] = ((bits ushr i) and 1) != 0
        }

        // Second copy (top-right & bottom-left): bit 0 at (8, size-1) .. bit 14 at (size-1, 8)
        for (i in 0..7) {
            modules[8][size - 1 - i] = ((bits ushr i) and 1) != 0
        }
        for (i in 8..14) {
            modules[size - 15 + i][8] = ((bits ushr i) and 1) != 0
        }
    }

    private fun computePenaltyScore(modules: Array<BooleanArray>, size: Int): Int {
        var score = 0

        // N1: 5+ consecutive same-colored modules in row or column
        for (r in 0 until size) {
            var runColor = false
            var runLen = 0
            for (c in 0 until size) {
                if (modules[r][c] == runColor) {
                    runLen++
                    if (runLen == 5) score += 3 else if (runLen > 5) score += 1
                } else {
                    runColor = modules[r][c]
                    runLen = 1
                }
            }
        }
        for (c in 0 until size) {
            var runColor = false
            var runLen = 0
            for (r in 0 until size) {
                if (modules[r][c] == runColor) {
                    runLen++
                    if (runLen == 5) score += 3 else if (runLen > 5) score += 1
                } else {
                    runColor = modules[r][c]
                    runLen = 1
                }
            }
        }

        // N2: 2x2 blocks of same color
        for (r in 0 until size - 1) {
            for (c in 0 until size - 1) {
                val color = modules[r][c]
                if (color == modules[r][c + 1] &&
                    color == modules[r + 1][c] &&
                    color == modules[r + 1][c + 1]
                ) {
                    score += 3
                }
            }
        }

        // N3: 1:1:3:1:1 finder-like patterns
        fun hasPattern(line: BooleanArray, start: Int): Boolean {
            if (start + 10 >= size) return false
            val p1 = !line[start] && !line[start + 1] && !line[start + 2] && !line[start + 3] &&
                line[start + 4] && !line[start + 5] && line[start + 6] && line[start + 7] &&
                line[start + 8] && !line[start + 9] && line[start + 10]
            val p2 = line[start] && !line[start + 1] && line[start + 2] && line[start + 3] &&
                line[start + 4] && !line[start + 5] && line[start + 6] && !line[start + 7] &&
                !line[start + 8] && !line[start + 9] && !line[start + 10]
            return p1 || p2
        }

        for (r in 0 until size) {
            for (c in 0 until size - 10) {
                if (hasPattern(modules[r], c)) score += 40
            }
        }
        val colLine = BooleanArray(size)
        for (c in 0 until size) {
            for (r in 0 until size) colLine[r] = modules[r][c]
            for (r in 0 until size - 10) {
                if (hasPattern(colLine, r)) score += 40
            }
        }

        // N4: dark ratio balance
        var darkCount = 0
        for (r in 0 until size) {
            for (c in 0 until size) {
                if (modules[r][c]) darkCount++
            }
        }
        val total = size * size
        val k = abs(darkCount * 20 - total * 10) / total
        score += k * 10

        return score
    }
}
