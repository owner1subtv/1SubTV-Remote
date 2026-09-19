package com.onesubtv.remote

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/** Varint-delimited protobuf framing used by Android TV Remote v2. */
object AndroidTvFraming {
    private const val MAX_FRAME_SIZE = 65_536
    fun writeFrame(out: OutputStream, payload: ByteArray) {
        require(payload.size <= MAX_FRAME_SIZE)
        writeVarint(out, payload.size.toLong()); out.write(payload); out.flush()
    }
    @Throws(IOException::class)
    fun readFrame(input: InputStream): ByteArray {
        val len=readVarint(input)
        require(len in 0..MAX_FRAME_SIZE.toLong()) { "Bad frame length: $len" }
        val b=ByteArray(len.toInt()); var n=0
        while(n<b.size){ val r=input.read(b,n,b.size-n); if(r<0) throw IOException("EOF"); n+=r }
        return b
    }
    fun writeVarint(out: OutputStream, value: Long) {
        var v=value
        while(v and 0x7f.inv().toLong()!=0L){ out.write(((v and 0x7f) or 0x80).toInt()); v=v ushr 7 }
        out.write((v and 0x7f).toInt())
    }
    fun readVarint(input: InputStream): Long {
        var r=0L; var shift=0
        repeat(10){ val b=input.read(); if(b<0) throw IOException("EOF in varint"); r=r or ((b and 0x7f).toLong() shl shift); if(b and 0x80==0) return r; shift+=7 }
        error("Malformed varint")
    }
}
