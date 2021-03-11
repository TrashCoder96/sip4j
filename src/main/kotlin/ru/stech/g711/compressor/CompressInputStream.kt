package ru.stech.g711.compressor

import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream

class CompressInputStream(input: InputStream?, useALaw: Boolean) : FilterInputStream(input) {
    private var compressor: Compressor? = null

    @Throws(IOException::class)
    override fun read(): Int {
        throw IOException(
            """${javaClass.name}.read() :
	Do not support simple read()."""
        )
    }

    @Throws(IOException::class)
    override fun read(b: ByteArray): Int {
        return read(b, 0, b.size)
    }

    @Throws(IOException::class)
    override fun read(b: ByteArray, off: Int, len: Int): Int {
        var offset = off
        var lenght = len
        var i: Int
        var sample: Int
        val inb: ByteArray
        inb = ByteArray(lenght shl 1) // get 16bit PCM data
        lenght = `in`.read(inb)
        if (lenght == -1) {
            return -1
        }
        i = 0
        while (i < lenght) {
            sample = inb[i++].toInt() and 0x00FF
            sample = sample or (inb[i++].toInt() shl 8)
            b[offset++] = compressor!!.compress(sample.toShort()).toByte()
        }
        return lenght shr 1
    }

    private val alawcompressor: Compressor = ALawCompressor()
    private val ulawcompressor: Compressor = uLawCompressor()

    init {
        compressor = if (useALaw) alawcompressor else ulawcompressor
    }
}

/*
	Mathematical Tools in Signal Processing with C++ and Java Simulations
		by	Willi-Hans Steeb
			International School for Scientific Computing
*/

