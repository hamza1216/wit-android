package com.waterloo.wit.utils

import android.util.Log

class LogUtils {
    companion object {
        fun e(tag: String, message: String?){
            Log.e(tag, message)
        }
        fun d(tag: String, message: String?){
            Log.d(tag, message)
        }
        fun i(tag: String, message: String?){
            Log.i(tag, message)
        }
        fun w(tag: String, message: String?){
            Log.w(tag, message)
        }


    }
}