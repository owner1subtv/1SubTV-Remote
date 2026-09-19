package com.onesubtv.remote

import android.util.Log

private const val TAG = "1SubTVRemote"
fun logD(message:String)=Log.d(TAG,message)
fun logI(message:String)=Log.i(TAG,message)
fun logW(message:String)=Log.w(TAG,message)
fun logE(message:String, error:Throwable?=null)=if(error==null) Log.e(TAG,message) else Log.e(TAG,message,error)
