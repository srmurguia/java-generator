package main.writers

import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException

class FileWriter {
    fun writeToFile(content: String, path: String) {
        var writer: BufferedWriter? = null
        try {
            val file = File(path)
            file.createNewFile()

            writer = BufferedWriter(FileWriter(file))

            writer.write(content)

        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                writer?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    fun mkdir(path: String) {
        File(path).mkdir()
    }
}