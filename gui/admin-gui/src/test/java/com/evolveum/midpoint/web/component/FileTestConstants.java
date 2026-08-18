/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

/**
 * Small generated fixtures used by file validation and sanitization tests.
 */
public final class FileTestConstants {

    public static final byte[] UNKNOWN_BINARY = new byte[] {
            0, 1, 2, 3, 4, 5, 6, 7
    };

    public static final byte[] XML_START_ARRAY = new byte[] {
            60, 114, 111, 108, 101, 32, 120, 109, 108, 110, 115, 61
    };

    private FileTestConstants() {
    }

    public static byte[] jpegBytes() throws IOException {
        return imageBytes("jpg", BufferedImage.TYPE_INT_RGB, Color.RED);
    }

    public static byte[] jpegBytesWithFakeExif() throws IOException {
        byte[] jpeg = jpegBytes();
        byte[] app1Payload = "Exif\0\0midpoint-test-metadata".getBytes(StandardCharsets.ISO_8859_1);
        int segmentLength = app1Payload.length + 2;
        byte[] segment = new byte[app1Payload.length + 4];
        segment[0] = (byte) 0xff;
        segment[1] = (byte) 0xe1;
        segment[2] = (byte) (segmentLength >> 8);
        segment[3] = (byte) segmentLength;
        System.arraycopy(app1Payload, 0, segment, 4, app1Payload.length);

        byte[] result = Arrays.copyOf(jpeg, jpeg.length + segment.length);
        System.arraycopy(segment, 0, result, 2, segment.length);
        System.arraycopy(jpeg, 2, result, 2 + segment.length, jpeg.length - 2);
        return result;
    }

    public static byte[] pngBytes() throws IOException {
        return imageBytes("png", BufferedImage.TYPE_INT_ARGB, new Color(0, 128, 255, 255));
    }

    public static byte[] pngBytesWithTransparency() throws IOException {
        return imageBytes("png", BufferedImage.TYPE_INT_ARGB, new Color(0, 255, 0, 80));
    }

    public static byte[] minimalPdfBytes() {
        return """
                %PDF-1.4
                1 0 obj
                << /Type /Catalog >>
                endobj
                %%EOF
                """.getBytes(StandardCharsets.US_ASCII);
    }

    public static byte[] docxBytes() throws IOException {
        try (XWPFDocument document = new XWPFDocument();
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            document.createParagraph().createRun().setText("test");
            document.write(out);
            return out.toByteArray();
        }
    }

    public static byte[] xlsxBytes() throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            workbook.createSheet("test").createRow(0).createCell(0).setCellValue(1);
            workbook.write(out);
            return out.toByteArray();
        }
    }

    public static byte[] legacyXlsBytes() throws IOException {
        try (HSSFWorkbook workbook = new HSSFWorkbook();
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            workbook.createSheet("test").createRow(0).createCell(0).setCellValue(1);
            workbook.write(out);
            return out.toByteArray();
        }
    }

    /**
     * ODF container with the {@code mimetype} entry first and uncompressed,
     * as required by the ODF specification and written by OpenOffice/LibreOffice.
     */
    public static byte[] odtBytes() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            byte[] mimeType = "application/vnd.oasis.opendocument.text".getBytes(StandardCharsets.US_ASCII);
            ZipEntry mimeTypeEntry = new ZipEntry("mimetype");
            mimeTypeEntry.setMethod(ZipEntry.STORED);
            mimeTypeEntry.setSize(mimeType.length);
            CRC32 crc = new CRC32();
            crc.update(mimeType);
            mimeTypeEntry.setCrc(crc.getValue());
            zip.putNextEntry(mimeTypeEntry);
            zip.write(mimeType);
            zip.closeEntry();

            zip.putNextEntry(new ZipEntry("content.xml"));
            zip.write(("<?xml version=\"1.0\"?>"
                    + "<office:document-content xmlns:office=\"urn:oasis:names:tc:opendocument:xmlns:office:1.0\"/>")
                    .getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return out.toByteArray();
    }

    private static byte[] imageBytes(String format, int imageType, Color color) throws IOException {
        BufferedImage image = new BufferedImage(3, 2, imageType);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(color);
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        } finally {
            graphics.dispose();
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            if (!ImageIO.write(image, format, out)) {
                throw new IOException("No ImageIO writer available for " + format);
            }
            return out.toByteArray();
        }
    }
}
