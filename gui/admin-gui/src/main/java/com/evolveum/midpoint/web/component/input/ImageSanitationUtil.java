/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component.input;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

/**
 * @author matisovaa
 *
 */
public final class ImageSanitationUtil {
    private static final Trace LOGGER = TraceManager.getTrace(ImageSanitationUtil.class);

    public static final Map<byte[], String> MAGIC_NUMBERS_TO_CONTENT_TYPES = Map.of(
            new byte[] { -1, -40, -1 }, "jpg",
            new byte[] { -119, 80, 78, 71, 13, 10, 26, 10 }, "png"
    );

    public static String getContentTypeFromFileMagicNumber(final byte[] fileBites) {
        magicNumbersFor:
        for (final byte[] magicNumber : MAGIC_NUMBERS_TO_CONTENT_TYPES.keySet()) {
            if (fileBites == null || fileBites.length < magicNumber.length) {
                return null;
            }
            for (int i = 0; i < magicNumber.length; i++) {
                if (magicNumber[i] != fileBites[i]) {
                    continue magicNumbersFor;
                }
            }
            return MAGIC_NUMBERS_TO_CONTENT_TYPES.get(magicNumber);
        }
        return null;
    }

    //TODO make more smaller methods
    public static byte[] removeEXIF(final byte[] originalBytes, final boolean removalEnabled) {
        if (originalBytes == null) {
            LOGGER.debug("There are no file for exif data removal.");
            return null;
        }

        if (!removalEnabled) {
            LOGGER.debug("Removal of exif data is not enabled.");
            return originalBytes;
        }

        final String imageFormatName = getContentTypeFromFileMagicNumber(originalBytes);
        if (imageFormatName == null) {
            LOGGER.warn("File format for removal of exif data is not recognized, so no exif data was removed.");
            return originalBytes;
        }

        // Read image (ImageIO automatically excludes metadata)
        BufferedImage image;
        try {
            image = ImageIO.read(new ByteArrayInputStream(originalBytes));
            if (image == null) {
                LOGGER.warn("Failed to read image for removal of exif data, so no exif data was removed.");
                return originalBytes;
            }
            LOGGER.debug("Read {} image: {} bytes", imageFormatName, originalBytes.length);
        } catch (IOException e) {
            LOGGER.error("Failed to read image for removal of exif data, so no exif data was removed: {}", e.getMessage());
            return originalBytes;
        }

        // Write image is same as input image format (no metadata)
        byte[] outputBytes;
        try {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            if (!ImageIO.write(image, imageFormatName, baos)) {
                LOGGER.warn("No {} writer available, so no exif data was removed.", imageFormatName);
                return originalBytes;
            }
            outputBytes = baos.toByteArray();
            LOGGER.debug("Wrote {} image: {} bytes", imageFormatName, outputBytes.length);
        } catch (IOException e) {
            LOGGER.error("Failed to write {} image for removal of exif data, so no exif data was removed: {}", imageFormatName, e.getMessage());
            return originalBytes;
        }

        return outputBytes;
    }
}
