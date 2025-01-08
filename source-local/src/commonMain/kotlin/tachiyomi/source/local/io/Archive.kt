package tachiyomi.source.local.io

import com.hippo.unifile.UniFile
import tachiyomi.core.common.storage.extension

object Archive {

    private val SUPPORTED_ARCHIVE_TYPES = listOf("avi", "flv", "mkv", "mov", "mp4", "webm", "wmv")

    fun isSupported(file: UniFile): Boolean = file.extension in SUPPORTED_ARCHIVE_TYPES
}
