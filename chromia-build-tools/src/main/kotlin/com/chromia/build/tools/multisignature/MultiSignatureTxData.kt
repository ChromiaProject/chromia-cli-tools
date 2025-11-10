package com.chromia.build.tools.multisignature

import mu.KLogging
import net.postchain.common.data.Hash
import net.postchain.common.hexStringToByteArray
import net.postchain.common.toHex
import net.postchain.gtv.GtvDecoder.decodeGtv
import net.postchain.gtv.GtvEncoder.encodeGtv
import net.postchain.gtv.mapper.GtvObjectMapper.fromGtv
import net.postchain.gtv.mapper.GtvObjectMapper.toGtvDictionary

class MultiSignatureTxData(val transaction: ByteArray, val txRid: Hash) {
    fun encode(): String = encodeGtv(toGtvDictionary(this)).toHex()

    companion object : KLogging() {
        fun decode(transactionData: String): MultiSignatureTxData {
            val gtv = decodeGtv(transactionData.hexStringToByteArray())
            return fromGtv(gtv, MultiSignatureTxData::class)
        }
    }
}
