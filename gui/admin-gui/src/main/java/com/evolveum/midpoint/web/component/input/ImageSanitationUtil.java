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

import static com.evolveum.midpoint.web.component.input.validator.FileMagicNumberConstants.MAGIC_NUMBERS_TO_CONTENT_TYPES;

/**
 * @author matisovaa
 *
 */
public final class ImageSanitationUtil {
    private static final Trace LOGGER = TraceManager.getTrace(ImageSanitationUtil.class);

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

    public static byte[] removeEXIF(final byte[] originalBytes, final boolean removalEnabled) { // TODO argument ImageUploadProcessingType
        if (originalBytes == null) {
            LOGGER.debug("There are no file for exif data removal.");
            return null;
        }

        if (!removalEnabled) { // TODO if stripExifData
            LOGGER.debug("Removal of exif data is not enabled.");
            return originalBytes;
        }

        final String imageFormatName = getContentTypeFromFileMagicNumber(originalBytes);
        if (imageFormatName == null) {
            LOGGER.error("File format for removal of exif data is not recognized, so no exif data was removed.");
            return originalBytes;
        }

        // Read image (ImageIO automatically excludes metadata)
        BufferedImage image = readImage(originalBytes);
        if (image == null) {
            return originalBytes;
        }

        // Write image is same as input image format (no metadata)
        byte[] outputBytes = writeImage(image, imageFormatName); // TODO if fixedFormat
        if (outputBytes == null) {
            return originalBytes;
        }

        return outputBytes;
    }

    private static BufferedImage readImage(final byte[] imageBytes) {
        BufferedImage image;
        try {
            image = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (image == null) {
                LOGGER.error("Failed to read image for removal of exif data, so no exif data was removed.");
                return null;
            }
        } catch (IOException e) {
            LOGGER.error("Failed to read image for removal of exif data, so no exif data was removed: {}", e.getMessage());
            return null;
        }
        return image;
    }

    private static byte[] writeImage(final BufferedImage image, final String outputImageFormatName) {
        byte[] outputBytes;
        try {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            if (!ImageIO.write(image, outputImageFormatName, baos)) {
                LOGGER.error("No {} writer available, so no exif data was removed.", outputImageFormatName);
                return null;
            }
            outputBytes = baos.toByteArray();
        } catch (IOException e) {
            LOGGER.error("Failed to write {} image for removal of exif data, so no exif data was removed: {}", outputImageFormatName, e.getMessage());
            return null;
        }
        return outputBytes;
    }
}
