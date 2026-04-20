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
public final class ImageSanitizationUtil {
    private static final Trace LOGGER = TraceManager.getTrace(ImageSanitizationUtil.class);

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

    public static byte[] sanitizeImage(final byte[] originalBytes, final boolean removalEnabled) // TODO argument ImageUploadProcessingType
            throws ImageSanitizationException {
        if (originalBytes == null) {
            LOGGER.debug("There are no file for sanitization.");
            return null;
        }

        if (!removalEnabled) { // TODO if stripExifData
            LOGGER.debug("Sanitization is not enabled.");
            return originalBytes;
        }

        final String imageFormatName = getContentTypeFromFileMagicNumber(originalBytes);
        if (imageFormatName == null) {
            throw new ImageSanitizationException("File format for sanitization is not recognized.");
        }

        // Read image (ImageIO automatically excludes metadata)
        BufferedImage image = readImage(originalBytes);

        // Write image is same as input image format (no metadata)
        return writeImage(image, imageFormatName); // TODO if fixedFormat
    }

    private static BufferedImage readImage(final byte[] imageBytes)
            throws ImageSanitizationException {
        BufferedImage image;
        try {
            image = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (image == null) {
                throw new ImageSanitizationException("Failed to read image for sanitization.");
            }
        } catch (IOException e) {
            throw new ImageSanitizationException("Failed to read image for sanitization.", e);
        }
        return image;
    }

    private static byte[] writeImage(final BufferedImage image, final String outputImageFormatName)
            throws ImageSanitizationException {
        byte[] outputBytes;
        try {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            if (!ImageIO.write(image, outputImageFormatName, baos)) {
                throw new ImageSanitizationException("No " + outputImageFormatName + " writer available.");
            }
            outputBytes = baos.toByteArray();
        } catch (IOException e) {
            throw new ImageSanitizationException("Failed to write " + outputImageFormatName + " image for sanitization.", e);
        }
        return outputBytes;
    }
}
