package com.m5dev.arcx.data.ndk

fun interface ExtractionProgressListener {
    fun onProgress(current: Int, total: Int, fileName: String): Boolean
}

fun interface CompressionProgressListener {
    fun onProgress(current: Int, total: Int, fileName: String): Boolean
}

object ArchiveNative {
    init {
        System.loadLibrary("arcx_native")
    }

    external fun listArchiveContents(path: String): Array<String>
    external fun extractArchive(archivePath: String, destPath: String): Boolean
    external fun extractArchiveWithPassword(archivePath: String, destPath: String, password: String): Boolean
    external fun extractArchiveWithProgress(
        archivePath: String,
        destPath: String,
        password: String?,
        listener: ExtractionProgressListener?
    ): Boolean

    external fun createArchiveWithProgress(
        sourcePaths: Array<String>,
        destArchivePath: String,
        format: String,
        level: String,
        password: String?,
        encryptionMethod: String?,
        listener: CompressionProgressListener?
    ): Boolean

    external fun create7zArchive(
        sourcePaths: Array<String>,
        destArchivePath: String,
        level: String,
        password: String?,
        listener: CompressionProgressListener?
    ): Boolean
}
