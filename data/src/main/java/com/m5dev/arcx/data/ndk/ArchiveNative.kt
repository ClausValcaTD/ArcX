package com.m5dev.arcx.data.ndk

object ArchiveNative {
    init {
        System.loadLibrary("arcx_native")
    }

    external fun listArchiveContents(path: String): Array<String>
    external fun extractArchive(archivePath: String, destPath: String): Boolean
    external fun extractArchiveWithPassword(archivePath: String, destPath: String, password: String): Boolean
}
