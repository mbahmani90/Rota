package com.majidbahmani.rota.core.data.remote.google

import android.content.Context
import android.content.pm.PackageManager
import java.security.MessageDigest

/** SHA-1 of the certificate this app is signed with, as Google expects it: uppercase hex, no colons. */
fun Context.signingCertificateSha1(): String? = try {
    packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        .signingInfo
        ?.apkContentsSigners
        ?.firstOrNull()
        ?.toByteArray()
        ?.sha1Hex()
} catch (e: PackageManager.NameNotFoundException) {
    null
}

fun ByteArray.sha1Hex(): String = MessageDigest.getInstance("SHA-1").digest(this).joinToString("") { "%02X".format(it) }
