package org.jetbrains.compose.resources

import kotlinx.cinterop.*
import platform.windows.*

internal actual fun getPlatformResourceReader(): ResourceReader = WindowsResourceReader

/** Reads the directory staged by sync*ComposeResources beside the native executable. */
internal object WindowsResourceReader : ResourceReader {
    override suspend fun read(path: String): ByteArray = readFile(path, 0, null)

    override suspend fun readPart(path: String, offset: Long, size: Long): ByteArray {
        require(offset >= 0 && size in 0..Int.MAX_VALUE.toLong()) { "Invalid resource byte range" }
        return readFile(path, offset, size)
    }

    override fun getUri(path: String): String = memScoped {
        val diskPath = diskPath(path)
        if (GetFileAttributesW(diskPath) == INVALID_FILE_ATTRIBUTES) throw MissingResourceException(path)
        windowsFileUri(diskPath)
    }

    private fun diskPath(path: String): String = memScoped {
        require(path.isNotEmpty() && !path.startsWith('/') && ':' !in path && '\\' !in path &&
            path.split('/').none { it == ".." }) { "Invalid resource path: $path" }
        val buffer = allocArray<UShortVar>(32768)
        val length = GetModuleFileNameW(null, buffer, 32768u)
        check(length in 1u..32767u) { "Cannot locate executable: ${GetLastError()}" }
        buffer.toKStringFromUtf16().substringBeforeLast('\\') + "\\compose-resources\\" + path.replace('/', '\\')
    }

    private fun readFile(path: String, offset: Long, requestedSize: Long?): ByteArray = memScoped {
        val file = CreateFileW(diskPath(path), GENERIC_READ, FILE_SHARE_READ.toUInt(),
            null, OPEN_EXISTING.toUInt(), FILE_ATTRIBUTE_NORMAL.toUInt(), null)
        if (file == INVALID_HANDLE_VALUE || file == null) throw MissingResourceException(path)
        try {
            val high = alloc<UIntVar>()
            SetLastError(0u)
            val low = GetFileSize(file, high.ptr)
            check(low != INVALID_FILE_SIZE || GetLastError() == 0u) { "Cannot get resource size: $path" }
            val length = (high.value.toLong() shl 32) or low.toLong()
            require(offset <= length) { "Resource offset exceeds file size: $path" }
            val count = requestedSize ?: length
            require(count <= Int.MAX_VALUE && count <= length - offset) { "Resource range exceeds file size: $path" }
            val positionHigh = alloc<IntVar> { value = (offset ushr 32).toInt() }
            SetLastError(0u)
            val position = SetFilePointer(file, offset.toInt(), positionHigh.ptr, FILE_BEGIN.toUInt())
            check(position != INVALID_SET_FILE_POINTER || GetLastError() == 0u) { "Cannot seek resource: $path" }
            val bytes = ByteArray(count.toInt())
            if (bytes.isNotEmpty()) bytes.usePinned { pinned ->
                val read = alloc<UIntVar>()
                var total = 0
                while (total < bytes.size) {
                    check(ReadFile(file, pinned.addressOf(total), (bytes.size - total).toUInt(), read.ptr, null) != 0 &&
                        read.value > 0u) { "Cannot read resource: $path" }
                    total += read.value.toInt()
                }
            }
            bytes
        } finally {
            CloseHandle(file)
        }
    }
}
