package com.onesubtv.remote

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.math.BigInteger
import java.security.*
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.*
import javax.security.auth.x500.X500Principal

/** Creates the persistent RSA client identity required by Remote v2 mTLS pairing. */
object AndroidTvCertFactory {
    data class Material(val cert:X509Certificate,val keyPair:KeyPair)
    private val ALG=byteArrayOf(0x06,0x09,0x2A,0x86.toByte(),0x48,0x86.toByte(),0xF7.toByte(),0x0D,0x01,0x01,0x0B)
    fun generate(now:Long=System.currentTimeMillis()):Material{
        val kp=KeyPairGenerator.getInstance("RSA").apply{initialize(2048,SecureRandom())}.generateKeyPair()
        val name=X500Principal("CN=1SubTV-Remote,O=1SubTV,OU=AndroidTvRemote")
        val tbs=seq(tagged(0,integer(BigInteger.valueOf(2))),integer(BigInteger(64,SecureRandom()).abs()),alg(),name.encoded,
            seq(time(Date(now-86400000L)),time(Date(now+5L*365*86400000L))),name.encoded,kp.public.encoded)
        val sig=Signature.getInstance("SHA256withRSA").run{initSign(kp.private);update(tbs);sign()}
        val der=seq(tbs,alg(),bitString(sig))
        val cert=CertificateFactory.getInstance("X.509").generateCertificate(ByteArrayInputStream(der)) as X509Certificate
        return Material(cert,kp)
    }
    private fun alg()=seq(ALG,byteArrayOf(0x05,0x00))
    private fun seq(vararg p:ByteArray)=tlv(0x30,concat(*p))
    private fun integer(v:BigInteger)=tlv(0x02,v.toByteArray())
    private fun bitString(b:ByteArray)=tlv(0x03,byteArrayOf(0)+b)
    private fun tagged(t:Int,b:ByteArray)=tlv(0xA0 or t,b)
    private fun time(d:Date):ByteArray{ val c=Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply{time=d}; val y=c.get(Calendar.YEAR); val f=SimpleDateFormat(if(y in 1950..2049)"yyMMddHHmmss'Z'" else "yyyyMMddHHmmss'Z'",Locale.US).apply{timeZone=TimeZone.getTimeZone("UTC")}; return tlv(if(y in 1950..2049)0x17 else 0x18,f.format(d).toByteArray(Charsets.US_ASCII)) }
    private fun tlv(tag:Int,b:ByteArray):ByteArray{val o=ByteArrayOutputStream();o.write(tag);if(b.size<128)o.write(b.size) else {val l=len(b.size);o.write(0x80 or l.size);o.write(l)};o.write(b);return o.toByteArray()}
    private fun len(n:Int):ByteArray{var v=n;val a=mutableListOf<Byte>();while(v>0){a.add(0,(v and 255).toByte());v=v ushr 8};return a.toByteArray()}
    private fun concat(vararg p:ByteArray):ByteArray{val o=ByteArrayOutputStream();p.forEach{o.write(it)};return o.toByteArray()}
}
