package com.example.basicdfp

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.provider.Settings
import java.util.UUID
import android.media.MediaDrm
import android.net.Uri
import android.os.Build

import java.security.MessageDigest

class DfpFactory (gsfIdProvider: GsfIdProvider,
                  androidIdProvider: AndroidIdProvider,
                  mediaDrmIdProvider: MediaDrmIdProvider,) {


    private val rawData = DeviceIdRawData(
        androidId = androidIdProvider.getAndroidId(),
        gsfId = gsfIdProvider.getGsfAndroidId(),
        mediaDrmId = mediaDrmIdProvider.getMediaDrmId()
    )


    fun getDfp(hashed: Boolean = true): String{
        var a_id = if ( ! rawData.androidId().value.isEmpty())
            rawData.androidId().value
        else
            "none"

        var g_id = if (! rawData.gsfId().value.isEmpty())
             rawData.gsfId().value
        else
            "none"

        var m_id = if (! rawData.mediaDrmId().value.isEmpty())
            rawData.mediaDrmId().value
        else
            "none"
        if (hashed)
            return "${hashString(a_id, "SHA256")}|${hashString(g_id, "SHA256")}|${hashString(m_id,"SHA256")}"
        else
            return "$a_id|$g_id|$m_id"
    }

    private fun hashString(input: String, algorithm: String): String {
        return MessageDigest
            .getInstance(algorithm)
            .digest(input.toByteArray())
            .fold("", { str, it -> str + "%02x".format(it) })
    }
}


class MediaDrmIdProvider {
    fun getMediaDrmId() = executeSafe({
        mediaDrmId()
    }, null)

    private fun mediaDrmId(): String {
        val widevineUUID = UUID(WIDEWINE_UUID_MOST_SIG_BITS, WIDEWINE_UUID_LEAST_SIG_BITS)
        val wvDrm: MediaDrm?

        wvDrm = MediaDrm(widevineUUID)
        val mivevineId = wvDrm.getPropertyByteArray(MediaDrm.PROPERTY_DEVICE_UNIQUE_ID)
        releaseMediaDRM(wvDrm)
        val md: MessageDigest = MessageDigest.getInstance("SHA-256")
        md.update(mivevineId)

        return md.digest().toHexString()
    }

    private fun releaseMediaDRM(drmObject: MediaDrm) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            drmObject.close()
        } else {
            drmObject.release()
        }
    }
}

class AndroidIdProvider(
    private val contentResolver: ContentResolver
) {
    @SuppressLint("HardwareIds")
    fun getAndroidId(): String {
        return executeSafe({
            Settings.Secure.getString(
                contentResolver,
                Settings.Secure.ANDROID_ID
            )
        }, "")
    }
}

class GsfIdProvider(
    private val contentResolver: ContentResolver
) {

    fun getGsfAndroidId(): String? {
        return executeSafe(
            { getGsfId() }, ""
        )
    }

    private fun getGsfId(): String? {
        val URI = Uri.parse(URI_GSF_CONTENT_PROVIDER)
        val params = arrayOf(ID_KEY)
        return try {
            val cursor = contentResolver
                .query(URI, null, null, params, null)

            if (cursor == null) {
                return null
            }

            if (!cursor.moveToFirst() || cursor.columnCount < 2) {
                cursor.close()
                return null
            }
            try {
                val result = java.lang.Long.toHexString(cursor.getString(1).toLong())
                cursor.close()
                result
            } catch (e: NumberFormatException) {
                cursor.close()
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}

class DeviceIdRawData(
    private val androidId: String,
    private val gsfId: String?,
    private val mediaDrmId: String?
) : RawData() {

    override fun signals() = listOf(
        gsfId(),
        androidId(),
        mediaDrmId()
    )

    fun gsfId() = object : IdentificationSignal<String>(
        1,
        null,
        StabilityLevel.STABLE,
        GSF_ID_NAME,
        GSF_ID_DISPLAY_NAME,
        gsfId ?: ""
    ) {
        override fun toString() = gsfId ?: ""
    }

    fun androidId() = object : IdentificationSignal<String>(
        1,
        null,
        StabilityLevel.STABLE,
        ANDROID_ID_NAME,
        ANDROID_ID_DISPLAY_NAME,
        androidId
    ) {
        override fun toString() = androidId
    }

    fun mediaDrmId() = object : IdentificationSignal<String>(
        3,
        null,
        StabilityLevel.STABLE,
        MEDIA_DRM_NAME,
        MEDIA_DRM_DISPLAY_NAME,
        mediaDrmId ?: ""
    ) {
        override fun toString() = mediaDrmId ?: ""
    }
}
abstract class Signal<T>(
    val name: String,
    val value: T
) {
    abstract fun toMap(): Map<String, Any>
}


abstract class IdentificationSignal<T>(
    val addedInVersion: Int,
    val removedInVersion: Int?,
    val stabilityLevel: StabilityLevel,
    name: String,
    val displayName: String,
    value: T
) : Signal<T>(
    name, value
) {
    abstract override fun toString(): String

    override fun toMap() = wrapSignalToMap(this)

    private fun wrapSignalToMap(signal: Signal<*>): Map<String, Any> {
        val VALUE_KEY = "v"

        return when (val value = signal.value ?: emptyMap<String, Any>()) {
            is String -> mapOf(
                VALUE_KEY to value
            )
            is Int -> mapOf(
                VALUE_KEY to value
            )
            is Long -> mapOf(
                VALUE_KEY to value
            )
            is Boolean -> mapOf(
                VALUE_KEY to value
            )
            is Map<*, *> -> mapOf(
                VALUE_KEY to value
            )
            else -> {
                mapOf(
                    VALUE_KEY to value.toString()
                )
            }
        }
    }
}
abstract class RawData {
    abstract fun signals(): List<IdentificationSignal<*>>
    fun signals(version: Int, stabilityLevel: StabilityLevel) = signals()
        .filterByStabilityLevel(
            stabilityLevel
        )
        .filterByVersion(version)
}

enum class StabilityLevel {
    STABLE,
    OPTIMAL,
    UNIQUE
}

//++++++=================================================================================
fun <T> executeSafe(code: () -> T, defaultValue: T): T {
    return try {
        code()
    } catch (exception: Exception) {
        defaultValue
    }
}

private fun ByteArray.toHexString(): String {
    return this.joinToString("") {
        java.lang.String.format("%02x", it)
    }
}

fun List<IdentificationSignal<*>>.filterByStabilityLevel(stabilityLevel: StabilityLevel): List<IdentificationSignal<*>> {
    return this.filter {
        when (stabilityLevel) {
            StabilityLevel.STABLE -> {
                it.stabilityLevel == StabilityLevel.STABLE
            }
            StabilityLevel.OPTIMAL -> {
                (it.stabilityLevel == StabilityLevel.STABLE) or (it.stabilityLevel == StabilityLevel.OPTIMAL)
            }
            StabilityLevel.UNIQUE -> {
                true
            }
        }
    }
}

fun List<IdentificationSignal<*>>.filterByVersion(version: Int): List<IdentificationSignal<*>> {
    return this.filter {
        val isNotRemoved =
            ((it.removedInVersion == null) || ((it.removedInVersion > version)))
        val enabledInVersion = it.addedInVersion in 1..version
        isNotRemoved && enabledInVersion
    }
}
//Media DRM id constants
private const val WIDEWINE_UUID_MOST_SIG_BITS = -0x121074568629b532L
private const val WIDEWINE_UUID_LEAST_SIG_BITS = -0x5c37d8232ae2de13L


//GSF id Constants
private const val URI_GSF_CONTENT_PROVIDER = "content://com.google.android.gsf.gservices"
private const val ID_KEY = "android_id"



//Device id Constants
const val GSF_ID_NAME = "gsfId"
const val GSF_ID_DISPLAY_NAME = "GSF ID"

const val ANDROID_ID_NAME = "androidId"
const val ANDROID_ID_DISPLAY_NAME = "Android ID"

const val MEDIA_DRM_NAME = "mediaDrm"
const val MEDIA_DRM_DISPLAY_NAME = "Media DRM"