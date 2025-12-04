package expo.modules.colorthief

import android.graphics.Bitmap
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * ColorThief - Grabs the dominant color or a representative color palette from an image.
 *
 * Based on ColorThiefSwift by Kazuki Ohara and the original Color Thief by Lokesh Dhakar.
 * Uses the MMCQ (modified median cut quantization) algorithm from the Leptonica library.
 */
object ColorThief {

    private const val DEFAULT_QUALITY = 10
    private const val DEFAULT_IGNORE_WHITE = true

    /**
     * Use the median cut algorithm to cluster similar colors and return the base color
     * from the largest cluster.
     *
     * @param bitmap the source bitmap
     * @param quality 1 is highest quality. 10 is default. Higher = faster but less accurate.
     * @param ignoreWhite if true, white pixels are ignored
     * @return the dominant color as [r, g, b] array, or null if extraction fails
     */
    fun getColor(
        bitmap: Bitmap,
        quality: Int = DEFAULT_QUALITY,
        ignoreWhite: Boolean = DEFAULT_IGNORE_WHITE
    ): IntArray? {
        val palette = getPalette(bitmap, 5, quality, ignoreWhite)
        return palette?.firstOrNull()
    }

    /**
     * Use the median cut algorithm to cluster similar colors.
     *
     * @param bitmap the source bitmap
     * @param colorCount number of colors to extract
     * @param quality 1 is highest quality. 10 is default. Higher = faster but less accurate.
     * @param ignoreWhite if true, white pixels are ignored
     * @return array of colors as [r, g, b] arrays, or null if extraction fails
     */
    fun getPalette(
        bitmap: Bitmap,
        colorCount: Int,
        quality: Int = DEFAULT_QUALITY,
        ignoreWhite: Boolean = DEFAULT_IGNORE_WHITE
    ): Array<IntArray>? {
        val colorMap = getColorMap(bitmap, colorCount, quality, ignoreWhite) ?: return null
        return colorMap.palette()
    }

    private fun getColorMap(
        bitmap: Bitmap,
        colorCount: Int,
        quality: Int,
        ignoreWhite: Boolean
    ): MMCQ.ColorMap? {
        val pixels = getPixels(bitmap, quality, ignoreWhite)
        return MMCQ.quantize(pixels, colorCount)
    }

    private fun getPixels(bitmap: Bitmap, quality: Int, ignoreWhite: Boolean): Array<IntArray> {
        val width = bitmap.width
        val height = bitmap.height
        val pixelCount = width * height
        val pixels = IntArray(pixelCount)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val pixelList = mutableListOf<IntArray>()
        var i = 0
        while (i < pixelCount) {
            val pixel = pixels[i]
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            val a = (pixel shr 24) and 0xFF

            // If pixel is mostly opaque and not white
            if (a >= 125) {
                if (!(ignoreWhite && r > 250 && g > 250 && b > 250)) {
                    pixelList.add(intArrayOf(r, g, b))
                }
            }
            i += quality
        }

        return pixelList.toTypedArray()
    }
}

/**
 * MMCQ (modified median cut quantization) algorithm from the Leptonica library.
 */
object MMCQ {
    private const val SIGNAL_BITS = 5
    private const val RIGHT_SHIFT = 8 - SIGNAL_BITS
    private const val MULTIPLIER = 1 shl RIGHT_SHIFT
    private const val HISTOGRAM_SIZE = 1 shl (3 * SIGNAL_BITS)
    private const val VBOX_LENGTH = 1 shl SIGNAL_BITS
    private const val FRACTION_BY_POPULATION = 0.75
    private const val MAX_ITERATIONS = 1000

    private fun getColorIndex(r: Int, g: Int, b: Int): Int {
        return (r shl (2 * SIGNAL_BITS)) + (g shl SIGNAL_BITS) + b
    }

    class ColorMap {
        private val vboxes = mutableListOf<VBox>()

        fun push(vbox: VBox) {
            vboxes.add(vbox)
        }

        fun palette(): Array<IntArray> {
            return vboxes.map { it.avg() }.toTypedArray()
        }
    }

    class VBox(
        var rMin: Int,
        var rMax: Int,
        var gMin: Int,
        var gMax: Int,
        var bMin: Int,
        var bMax: Int,
        private val histogram: IntArray
    ) {
        private var _avg: IntArray? = null
        private var _volume: Int? = null
        private var _count: Int? = null

        fun copy(): VBox {
            return VBox(rMin, rMax, gMin, gMax, bMin, bMax, histogram)
        }

        fun volume(force: Boolean = false): Int {
            if (_volume == null || force) {
                _volume = (rMax - rMin + 1) * (gMax - gMin + 1) * (bMax - bMin + 1)
            }
            return _volume!!
        }

        fun count(force: Boolean = false): Int {
            if (_count == null || force) {
                var count = 0
                for (r in rMin..rMax) {
                    for (g in gMin..gMax) {
                        for (b in bMin..bMax) {
                            count += histogram[getColorIndex(r, g, b)]
                        }
                    }
                }
                _count = count
            }
            return _count!!
        }

        fun avg(force: Boolean = false): IntArray {
            if (_avg == null || force) {
                var ntot = 0
                var rSum = 0
                var gSum = 0
                var bSum = 0

                for (r in rMin..rMax) {
                    for (g in gMin..gMax) {
                        for (b in bMin..bMax) {
                            val hval = histogram[getColorIndex(r, g, b)]
                            ntot += hval
                            rSum += (hval * (r + 0.5) * MULTIPLIER).toInt()
                            gSum += (hval * (g + 0.5) * MULTIPLIER).toInt()
                            bSum += (hval * (b + 0.5) * MULTIPLIER).toInt()
                        }
                    }
                }

                _avg = if (ntot > 0) {
                    intArrayOf(rSum / ntot, gSum / ntot, bSum / ntot)
                } else {
                    intArrayOf(
                        min(MULTIPLIER * (rMin + rMax + 1) / 2, 255),
                        min(MULTIPLIER * (gMin + gMax + 1) / 2, 255),
                        min(MULTIPLIER * (bMin + bMax + 1) / 2, 255)
                    )
                }
            }
            return _avg!!
        }

        fun widestColorChannel(): Int {
            val rWidth = rMax - rMin
            val gWidth = gMax - gMin
            val bWidth = bMax - bMin
            return when (max(rWidth, max(gWidth, bWidth))) {
                rWidth -> 0 // R
                gWidth -> 1 // G
                else -> 2   // B
            }
        }
    }

    fun quantize(pixels: Array<IntArray>, maxColors: Int): ColorMap? {
        if (pixels.isEmpty() || maxColors < 2 || maxColors > 256) {
            return null
        }

        val histogram = IntArray(HISTOGRAM_SIZE)
        var rMin = Int.MAX_VALUE
        var rMax = Int.MIN_VALUE
        var gMin = Int.MAX_VALUE
        var gMax = Int.MIN_VALUE
        var bMin = Int.MAX_VALUE
        var bMax = Int.MIN_VALUE

        for (pixel in pixels) {
            val r = pixel[0] shr RIGHT_SHIFT
            val g = pixel[1] shr RIGHT_SHIFT
            val b = pixel[2] shr RIGHT_SHIFT

            rMin = min(rMin, r)
            rMax = max(rMax, r)
            gMin = min(gMin, g)
            gMax = max(gMax, g)
            bMin = min(bMin, b)
            bMax = max(bMax, b)

            histogram[getColorIndex(r, g, b)]++
        }

        val vbox = VBox(rMin, rMax, gMin, gMax, bMin, bMax, histogram)
        val pq = mutableListOf(vbox)

        val target = ceil(FRACTION_BY_POPULATION * maxColors).toInt()
        iterate(pq, { a, b -> a.count() - b.count() }, target, histogram)
        pq.sortWith { a, b -> a.count() * a.volume() - b.count() * b.volume() }
        iterate(pq, { a, b -> a.count() * a.volume() - b.count() * b.volume() }, maxColors, histogram)

        pq.reverse()
        val colorMap = ColorMap()
        pq.forEach { colorMap.push(it) }
        return colorMap
    }

    private fun iterate(
        queue: MutableList<VBox>,
        comparator: (VBox, VBox) -> Int,
        target: Int,
        histogram: IntArray
    ) {
        var niters = 0
        while (niters < MAX_ITERATIONS) {
            if (queue.isEmpty()) break
            val vbox = queue.removeAt(queue.size - 1)
            if (vbox.count() == 0) {
                queue.sortWith(comparator)
                niters++
                continue
            }

            val vboxes = medianCutApply(histogram, vbox)
            queue.add(vboxes[0])
            if (vboxes.size == 2) {
                queue.add(vboxes[1])
            }
            queue.sortWith(comparator)

            if (queue.size >= target) return
            niters++
        }
    }

    private fun medianCutApply(histogram: IntArray, vbox: VBox): List<VBox> {
        if (vbox.count() == 0) return emptyList()
        if (vbox.count() == 1) return listOf(vbox)

        val axis = vbox.widestColorChannel()
        val total: Int
        val partialSum = IntArray(VBOX_LENGTH) { -1 }

        when (axis) {
            0 -> { // R
                var sum = 0
                for (r in vbox.rMin..vbox.rMax) {
                    var innerSum = 0
                    for (g in vbox.gMin..vbox.gMax) {
                        for (b in vbox.bMin..vbox.bMax) {
                            innerSum += histogram[getColorIndex(r, g, b)]
                        }
                    }
                    sum += innerSum
                    partialSum[r] = sum
                }
                total = sum
            }
            1 -> { // G
                var sum = 0
                for (g in vbox.gMin..vbox.gMax) {
                    var innerSum = 0
                    for (r in vbox.rMin..vbox.rMax) {
                        for (b in vbox.bMin..vbox.bMax) {
                            innerSum += histogram[getColorIndex(r, g, b)]
                        }
                    }
                    sum += innerSum
                    partialSum[g] = sum
                }
                total = sum
            }
            else -> { // B
                var sum = 0
                for (b in vbox.bMin..vbox.bMax) {
                    var innerSum = 0
                    for (r in vbox.rMin..vbox.rMax) {
                        for (g in vbox.gMin..vbox.gMax) {
                            innerSum += histogram[getColorIndex(r, g, b)]
                        }
                    }
                    sum += innerSum
                    partialSum[b] = sum
                }
                total = sum
            }
        }

        val lookAheadSum = IntArray(VBOX_LENGTH) { -1 }
        for (i in partialSum.indices) {
            if (partialSum[i] != -1) {
                lookAheadSum[i] = total - partialSum[i]
            }
        }

        return doCut(axis, vbox, partialSum, lookAheadSum, total)
    }

    private fun doCut(
        axis: Int,
        vbox: VBox,
        partialSum: IntArray,
        lookAheadSum: IntArray,
        total: Int
    ): List<VBox> {
        val vboxMin: Int
        val vboxMax: Int
        when (axis) {
            0 -> { vboxMin = vbox.rMin; vboxMax = vbox.rMax }
            1 -> { vboxMin = vbox.gMin; vboxMax = vbox.gMax }
            else -> { vboxMin = vbox.bMin; vboxMax = vbox.bMax }
        }

        for (i in vboxMin..vboxMax) {
            if (partialSum[i] > total / 2) {
                val vbox1 = vbox.copy()
                val vbox2 = vbox.copy()

                val left = i - vboxMin
                val right = vboxMax - i
                var d2 = if (left <= right) {
                    min(vboxMax - 1, i + right / 2)
                } else {
                    max(vboxMin, (i - 1 - left / 2.0).toInt())
                }

                while (d2 < 0 || partialSum[d2] <= 0) d2++
                var count2 = lookAheadSum[d2]
                while (count2 == 0 && d2 > 0 && partialSum[d2 - 1] > 0) {
                    d2--
                    count2 = lookAheadSum[d2]
                }

                when (axis) {
                    0 -> { vbox1.rMax = d2; vbox2.rMin = d2 + 1 }
                    1 -> { vbox1.gMax = d2; vbox2.gMin = d2 + 1 }
                    else -> { vbox1.bMax = d2; vbox2.bMin = d2 + 1 }
                }

                return listOf(vbox1, vbox2)
            }
        }

        throw IllegalStateException("VBox can't be cut")
    }
}