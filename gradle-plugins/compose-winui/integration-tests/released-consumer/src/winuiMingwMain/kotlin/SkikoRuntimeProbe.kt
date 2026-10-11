import org.jetbrains.skia.Surface

fun main() {
    val surface = Surface.makeRasterN32Premul(4, 4)
    try {
        surface.canvas.clear(0xff123456.toInt())
        val image = surface.makeImageSnapshot()
        try {
            check(image.width == 4 && image.height == 4)
            println("SKIKO_MINGW_RUNTIME_OK")
        } finally {
            image.close()
        }
    } finally {
        surface.close()
    }
}
