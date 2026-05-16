package de.visustruct.other

import java.awt.Desktop
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.net.URI
import javax.swing.Icon
import javax.swing.ImageIcon

object Helpers {

    @JvmStatic
    fun openWebsite(uri: String) {
        try {
            Desktop.getDesktop().browse(URI(uri))
        } catch (e: java.io.IOException) {
            throw IllegalStateException("failed to open uri $uri", e)
        } catch (e: java.net.URISyntaxException) {
            throw IllegalStateException("failed to open uri $uri", e)
        }
    }

    @JvmStatic
    fun readTextFile(path: String): Array<String> {
        BufferedReader(InputStreamReader(FileInputStream(File(path)))).use { reader ->
            val lines = mutableListOf<String>()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                lines.add(line!!)
            }
            return lines.toTypedArray()
        }
    }

    @JvmStatic
    fun getIcon(resourcePath: String): Icon =
        ImageIcon(Helpers::class.java.getResource(resourcePath))
}
