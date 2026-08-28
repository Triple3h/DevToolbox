package com.tripleh.devtoolbox.tools.diff

/**
 * Line-oriented Myers diff producing a list of change blocks.
 *
 * Each [DiffBlock] describes one contiguous run of lines that is an addition
 * (present only in the right text) or a deletion (present only in the left text).
 * Lines that are unchanged are not reported.
 */
data class DiffBlock(
    val leftStart: Int, // 0-based index into left lines
    val leftCount: Int,
    val rightStart: Int, // 0-based index into right lines
    val rightCount: Int,
) {
    val isAddition: Boolean get() = leftCount == 0 && rightCount > 0
    val isDeletion: Boolean get() = rightCount == 0 && leftCount > 0
}

object TextDiff {

    fun diff(left: String, right: String): List<DiffBlock> {
        val a = left.split('\n')
        val b = right.split('\n')
        if (a.size == b.size && a.indices.all { a[it] == b[it] }) return emptyList()

        val n = a.size
        val m = b.size
        val max = n + m
        val size = 2 * max + 1
        val v = IntArray(size)
        val trace = ArrayList<IntArray>()

        var d = 0
        var found = false
        outer@ while (d <= max) {
            trace.add(v.copyOf())
            val kStart = -d
            val kEnd = d
            var k = kStart
            while (k <= kEnd) {
                val idx = k + max
                var x = when {
                    k == -d || (k != d && v[idx - 1] < v[idx + 1]) -> v[idx + 1]
                    else -> v[idx - 1] + 1
                }
                var y = x - k
                while (x < n && y < m && a[x] == b[y]) {
                    x++
                    y++
                }
                v[idx] = x
                if (x >= n && y >= m) {
                    found = true
                    break@outer
                }
                k += 2
            }
            d++
        }
        if (!found) return emptyList()

        // Backtrack along the trace to collect only the insert/delete steps.
        var x = n
        var y = m
        val blocks = ArrayList<DiffBlock>()
        for (traceIdx in trace.indices.reversed()) {
            val vPrev = trace[traceIdx]
            val dd = traceIdx
            val k = x - y
            val idx = k + max
            val prevK = when {
                k == -dd || (k != dd && vPrev[idx - 1] < vPrev[idx + 1]) -> k + 1
                else -> k - 1
            }
            val prevX = vPrev[prevK + max]
            val prevY = prevX - prevK
            while (x > prevX && y > prevY) {
                x--
                y--
            }
            if (x > prevX) {
                // deletion run: left lines prevX..x-1
                blocks.add(DiffBlock(prevX, x - prevX, prevY, 0))
                x = prevX
            } else if (y > prevY) {
                // addition run: right lines prevY..y-1
                blocks.add(DiffBlock(prevX, 0, prevY, y - prevY))
                y = prevY
            }
        }
        // The backtrack visits blocks in reverse order; restore chronological order.
        blocks.reverse()
        return blocks
    }
}
