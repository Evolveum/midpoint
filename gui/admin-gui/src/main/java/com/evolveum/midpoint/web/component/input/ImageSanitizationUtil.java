/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component.input;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ImageProcessingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ImageUploadProcessingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.StripExifDataType;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static com.evolveum.midpoint.common.MimeTypeUtil.MIME_IMAGE_JPEG;
import static com.evolveum.midpoint.common.MimeTypeUtil.getExtensionRaw;
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

    public static byte[] sanitizeImage(final byte[] originalBytes, final ImageUploadProcessingType imageUploadProcessingConfig)
            throws ImageSanitizationException {
        if (imageUploadProcessingConfig == null) {
            LOGGER.debug("There are no sanitization configured.");
            return originalBytes;
        }

        if (originalBytes == null) {
            LOGGER.debug("There are no file for sanitization.");
            return null;
        }

        if (ImageProcessingType.PRESERVEORIGINALFORMAT.equals(imageUploadProcessingConfig.getProcessing()) &&
                !StripExifDataType.TRUE.equals(imageUploadProcessingConfig.getStripExifData())) {
            LOGGER.debug("There are no sanitization enabled in configuration.");
            return originalBytes;
        }

        final String imageFormatName = getOutputImageFormatName(originalBytes, imageUploadProcessingConfig);
        if (imageFormatName == null) {
            throw new ImageSanitizationException("File format for sanitization is not recognized.");
        }

        // Read image (ImageIO automatically excludes metadata)
        BufferedImage image = readImage(originalBytes);

        // Write image to given format (no metadata)
        return writeImage(image, imageFormatName);
    }

    private static String getOutputImageFormatName(final byte[] originalBytes, final ImageUploadProcessingType imageUploadProcessingConfig) {
        if (ImageProcessingType.FIXEDFORMAT.equals(imageUploadProcessingConfig.getProcessing())) {
            if (imageUploadProcessingConfig.getFormat() != null) {
                return imageUploadProcessingConfig.getFormat().value();
            }
            return getExtensionRaw(MIME_IMAGE_JPEG);
        }
        return getContentTypeFromFileMagicNumber(originalBytes);
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
