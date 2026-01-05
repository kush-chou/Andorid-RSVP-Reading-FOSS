package com.example.fossrsvp

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class EpubParserTest {

    @Test
    fun testParseEpubFromStream() {
        val epubContent = createMockEpub("<h1>Chapter 1</h1><p>Hello World</p>", "<h1>Chapter 2</h1><p>This is a test.</p>")
        val inputStream = ByteArrayInputStream(epubContent)

        val result = parseEpubFromStream(inputStream)

        assertTrue(result.contains("Hello World"))
        assertTrue(result.contains("This is a test."))
    }

    private fun createMockEpub(vararg chapters: String): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        val zipOutputStream = ZipOutputStream(byteArrayOutputStream)

        chapters.forEachIndexed { index, content ->
            val entry = ZipEntry("chapter_$index.xhtml")
            zipOutputStream.putNextEntry(entry)
            zipOutputStream.write(content.toByteArray(Charsets.UTF_8))
            zipOutputStream.closeEntry()
        }

        // Add mimetype file (required for valid EPUB, but our parser might not check it rigorously yet)
        val mimetypeEntry = ZipEntry("mimetype")
        zipOutputStream.putNextEntry(mimetypeEntry)
        zipOutputStream.write("application/epub+zip".toByteArray(Charsets.UTF_8))
        zipOutputStream.closeEntry()

        zipOutputStream.close()
        return byteArrayOutputStream.toByteArray()
    }
}
