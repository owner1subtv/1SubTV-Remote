package com.onesubtv.remote

import android.annotation.SuppressLint
import java.net.InetSocketAddress
import java.net.Socket
import java.security.*
import java.security.cert.X509Certificate
import javax.net.ssl.*

/** Isolated mTLS socket factory for pairing (6467) and commands (6466). */
class AndroidTvSocketFactory(private val material:AndroidTvCertFactory.Material,private val expectedFingerprint:String?=null){
    fun connect(host:String,port:Int):SSLSocket{
        val ctx=SSLContext.getInstance("TLS");ctx.init(arrayOf(KM(material.cert,material.keyPair.private)),arrayOf(trustAll()),SecureRandom())
        val s=ctx.socketFactory.createSocket() as SSLSocket;s.connect(InetSocketAddress(host,port),8000);s.startHandshake()
        val cert=s.session.peerCertificates.first() as X509Certificate
        val fp=sha256(cert.encoded)
        if(expectedFingerprint!=null&&!expectedFingerprint.equals(fp,true)){s.close();throw SecurityException("TV certificate changed; re-pair required")}
        return s
    }
    @SuppressLint("CustomX509TrustManager","TrustAllX509TrustManager")
    private fun trustAll()=object:X509TrustManager{override fun checkClientTrusted(c:Array<X509Certificate>,a:String){};override fun checkServerTrusted(c:Array<X509Certificate>,a:String){};override fun getAcceptedIssuers()=emptyArray<X509Certificate>()}
    private fun sha256(b:ByteArray)=MessageDigest.getInstance("SHA-256").digest(b).joinToString(""){"%02x".format(it)}
    private class KM(private val c:X509Certificate,private val k:PrivateKey):X509ExtendedKeyManager(){
        override fun getClientAliases(t:String,i:Array<java.security.Principal>?)=arrayOf("1subtv")
        override fun chooseClientAlias(t:Array<String>,i:Array<java.security.Principal>?,s:Socket?)="1subtv"
        override fun chooseEngineClientAlias(t:Array<String>,i:Array<java.security.Principal>?,e:SSLEngine?)="1subtv"
        override fun getServerAliases(t:String,i:Array<java.security.Principal>?)=arrayOf("1subtv")
        override fun chooseServerAlias(t:String,i:Array<java.security.Principal>?,s:Socket?)="1subtv"
        override fun getCertificateChain(a:String)=arrayOf(c)
        override fun getPrivateKey(a:String)=k
    }
}
