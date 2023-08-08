package com.aya.mdnssearch

import android.net.nsd.NsdServiceInfo
import timber.log.Timber

class Utils {

    companion object {
        fun byteArrayToString(byteArray: ByteArray): String {
            return byteArray.joinToString(", ") { it.toString() }
        }

        fun byteArrayToHex(byteArray: ByteArray): String {
            return byteArray.joinToString("") { "%02x ".format(it) }
        }

        fun byteArrayToDecimal(byteArray: ByteArray): Int {
            var result = 0
            for (i in byteArray.indices) {
                result = result shl 8 or (byteArray[i].toInt() and 0xFF)
            }
            return result
        }

        fun getAttributes(serviceInfo: NsdServiceInfo): String? {
            val attributes = serviceInfo.attributes
            val resultBuilder = StringBuilder()

            if (attributes != null) {
                for ((key, value) in attributes) {
                    val attributeValue = if (value != null) byteArrayToDecimal(value) else null
                    resultBuilder.append("$key:$attributeValue \n ")
                }
            }

            return resultBuilder.toString().trimEnd(',', ' ')
        }

        fun getAttributes_debug(serviceInfo: NsdServiceInfo) {
            val attributes = serviceInfo.attributes
            if (attributes != null) {
                for ((key, value) in attributes) {
                    val attributeValue = if (value != null) byteArrayToHex(value) else null
                    Timber.d("getAttributes ${serviceInfo.serviceName} -> $key:$attributeValue")
                }
            }
        }
    }


}