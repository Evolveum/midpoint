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

import javax.imageio.ImageIO;

/**
 * Small generated fixtures used by file validation and sanitization tests.
 */
public final class FileTestConstants {

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
