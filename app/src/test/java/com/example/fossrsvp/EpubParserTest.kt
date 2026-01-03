package com.example.fossrsvp

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class EpubParserTest {

    @Test
    fun `test epub parsing follows spine order`() {
        // Create a temporary EPUB file
        val tempFile = File.createTempFile("test_book", ".epub")
        tempFile.deleteOnExit()

        ZipOutputStream(FileOutputStream(tempFile)).use { zip ->
            // 1. mimetype
            zip.putNextEntry(ZipEntry("mimetype"))
            zip.write("application/epub+zip".toByteArray())
            zip.closeEntry()

            // 2. META-INF/container.xml
            zip.putNextEntry(ZipEntry("META-INF/container.xml"))
            val containerXml = """
                <?xml version="1.0"?>
                <container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
                    <rootfiles>
                        <rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/>
                    </rootfiles>
                </container>
            """.trimIndent()
            zip.write(containerXml.toByteArray())
            zip.closeEntry()

            // 3. OEBPS/content.opf
            zip.putNextEntry(ZipEntry("OEBPS/content.opf"))
            val opfContent = """
                <?xml version="1.0" encoding="UTF-8"?>
                <package xmlns="http://www.idpf.org/2007/opf" unique-identifier="BookId" version="2.0">
                    <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
                        <dc:title>Test Book</dc:title>
                    </metadata>
                    <manifest>
                        <item id="ch1" href="chapter1.html" media-type="application/xhtml+xml"/>
                        <item id="ch2" href="chapter2.html" media-type="application/xhtml+xml"/>
                        <item id="ncx" href="toc.ncx" media-type="application/x-dtbncx+xml"/>
                    </manifest>
                    <spine toc="ncx">
                        <itemref idref="ch1"/>
                        <itemref idref="ch2"/>
                    </spine>
                </package>
            """.trimIndent()
            zip.write(opfContent.toByteArray())
            zip.closeEntry()

            // 4. Content files
            zip.putNextEntry(ZipEntry("OEBPS/chapter2.html"))
            val ch2Content = "<html><body><p>World</p></body></html>"
            zip.write(ch2Content.toByteArray())
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("OEBPS/chapter1.html"))
            val ch1Content = "<html><body><p>Hello</p></body></html>"
            zip.write(ch1Content.toByteArray())
            zip.closeEntry()
        }

        val result = parseEpubFile(tempFile)
        val normalizedResult = result.trim().replace(Regex("\\s+"), " ")
        assertEquals("Hello World", normalizedResult)
    }

    @Test
    fun `test epub parsing with attribute variations`() {
        val tempFile = File.createTempFile("test_book_attributes", ".epub")
        tempFile.deleteOnExit()

        ZipOutputStream(FileOutputStream(tempFile)).use { zip ->
            zip.putNextEntry(ZipEntry("mimetype"))
            zip.write("application/epub+zip".toByteArray())
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("META-INF/container.xml"))
            // Single quotes, extra spaces, mixed attribute order
            val containerXml = """
                <?xml version="1.0"?>
                <container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
                    <rootfiles>
                        <rootfile media-type='application/oebps-package+xml' full-path='OEBPS/content.opf' />
                    </rootfiles>
                </container>
            """.trimIndent()
            zip.write(containerXml.toByteArray())
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("OEBPS/content.opf"))
            // Mixed attribute order, single quotes
            val opfContent = """
                <?xml version="1.0" encoding="UTF-8"?>
                <package xmlns="http://www.idpf.org/2007/opf" unique-identifier="BookId" version="2.0">
                    <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
                        <dc:title>Test Book</dc:title>
                    </metadata>
                    <manifest>
                        <item href='chapter1.html' id='ch1' media-type='application/xhtml+xml'/>
                    </manifest>
                    <spine toc="ncx">
                        <itemref idref='ch1'/>
                    </spine>
                </package>
            """.trimIndent()
            zip.write(opfContent.toByteArray())
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("OEBPS/chapter1.html"))
            val ch1Content = "<html><body><p>Attributes Work</p></body></html>"
            zip.write(ch1Content.toByteArray())
            zip.closeEntry()
        }

        val result = parseEpubFile(tempFile)
        val normalizedResult = result.trim().replace(Regex("\\s+"), " ")
        assertEquals("Attributes Work", normalizedResult)
    }
}
