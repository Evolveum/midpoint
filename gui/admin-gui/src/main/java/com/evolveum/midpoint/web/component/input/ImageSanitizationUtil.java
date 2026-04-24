/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component.input;

import static com.evolveum.midpoint.common.MimeTypeUtil.MIME_IMAGE_JPEG;
import static com.evolveum.midpoint.common.MimeTypeUtil.getExtensionRaw;
import static com.evolveum.midpoint.web.component.input.validator.FileMagicNumberConstants.MAGIC_NUMBERS_TO_FILE_EXTENSIONS;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

import org.apache.commons.lang3.BooleanUtils;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ImageProcessingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ImageUploadProcessingType;

/**
 * Handle sanitization if images. Sanitization is configurable by input ImageUploadProcessingType configuration.
 * Possible sanitization options are e.g. remove EXIF data or convert to fixed format.
 *
 * @author matisovaa
 *
 */
public final class ImageSanitizationUtil {

    private static final Trace LOGGER = TraceManager.getTrace(ImageSanitizationUtil.class);

    private static final Color BACKGROUND_COLOR = Color.WHITE;

    /**
     * Determines file extension by comparing first bytes of file byte array with known magic numbers.
     *
     * @param fileBites file byte array to determine file extension
     * @return file extension or null if file extension was not possible to determine
     */
    public static String getFileExtensionFromFileMagicNumber(byte[] fileBites) {
        magicNumbersFor:
        for (final byte[] magicNumber : MAGIC_NUMBERS_TO_FILE_EXTENSIONS.keySet()) {
            if (fileBites == null || fileBites.length < magicNumber.length) {
                return null;
            }
            for (int i = 0; i < magicNumber.length; i++) {
                if (magicNumber[i] != fileBites[i]) {
                    continue magicNumbersFor;
                }
            }
            return MAGIC_NUMBERS_TO_FILE_EXTENSIONS.get(magicNumber);
        }
        return null;
    }

    /**
     * Sanitize image based on ImageUploadProcessingType configuration.
     *
     * @param originalBytes image to sanitize
     * @param config configuration what conversion is needed with input image
     * e.g. remove EXIF data or convert to fixed format
     * @return image updated based on given configuration
     * @throws ImageSanitizationException if there was error during sanitization process
     */
    public static byte[] sanitizeImage(byte[] originalBytes, ImageUploadProcessingType config)
            throws ImageSanitizationException {
        if (config == null) {
            LOGGER.debug("There are no sanitization configured.");
            return originalBytes;
        }

        if (originalBytes == null) {
            LOGGER.debug("There are no file for sanitization.");
            return null;
        }

        if (ImageProcessingType.FIXED != config.getProcessing() && BooleanUtils.isNotTrue(config.getStripExifData())) {
            LOGGER.debug("There are no sanitization enabled in configuration.");
            return originalBytes;
        }

        final String imageFormatName = getOutputImageFormatName(originalBytes, config);
        if (imageFormatName == null) {
            throw new ImageSanitizationException("File format for sanitization is not recognized.");
        }

        // Read image (ImageIO automatically excludes metadata)
        BufferedImage image = readImage(originalBytes);

        // Write image to given format (no metadata)
        return writeImage(image, imageFormatName);
    }

    private static String getOutputImageFormatName(byte[] originalBytes, ImageUploadProcessingType config) {
        if (ImageProcessingType.FIXED == config.getProcessing()) {
            if (config.getFormat() != null) {
                return config.getFormat().value();
            }
            return getExtensionRaw(MIME_IMAGE_JPEG);
        }
        return getFileExtensionFromFileMagicNumber(originalBytes);
    }

    /**
     * Reads input byte array to BufferedImage
     *
     * @param imageBytes to convert to BufferedImage
     * @return image as BufferedImage
     * @throws ImageSanitizationException if read of image ends with error
     */
    private static BufferedImage readImage(byte[] imageBytes) throws ImageSanitizationException {
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

    /**
     * Writes input BufferedImage to byte array of given output image file format.
     *
     * @param image to convert to byte array
     * @param outputImageFormatName name of output image format
     * @return image as byte array of given output image file format
     * @throws ImageSanitizationException if write of image ends with error
     */
    private static byte[] writeImage(BufferedImage image, String outputImageFormatName)
            throws ImageSanitizationException {
        try {
            final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

            if (ImageIO.write(image, outputImageFormatName, byteArrayOutputStream)) {
                return byteArrayOutputStream.toByteArray();
            }

            // try to handle PNG to JPG conversion
            if (getExtensionRaw(MIME_IMAGE_JPEG).equals(outputImageFormatName)
                    && ImageIO.write(handleTransparency(image), outputImageFormatName, byteArrayOutputStream)) {
                return byteArrayOutputStream.toByteArray();
            }

            throw new ImageSanitizationException("No " + outputImageFormatName + " writer available.");
        } catch (IOException e) {
            throw new ImageSanitizationException("Failed to write " + outputImageFormatName + " image for sanitization.", e);
        }
    }

    /**
     * Draw the original image onto the new RGB canvas to remove transparent parts.
     * Use Color.WHITE as a background to fill any transparent parts.
     *
     * @param inputImage for which we need to fill any transparent parts
     * @return image where originally transparent parts was replaced by Color.WHITE
     */
    private static BufferedImage handleTransparency(BufferedImage inputImage) {
        // Create a new blank RGB image (no transparency)
        BufferedImage outputImage = new BufferedImage(
                inputImage.getWidth(),
                inputImage.getHeight(),
                BufferedImage.TYPE_INT_RGB
        );

        // Draw the original image onto the new RGB canvas
        // Use Color.WHITE as a background to fill any transparent parts
        outputImage.createGraphics().drawImage(inputImage, 0, 0, BACKGROUND_COLOR, null);

        return outputImage;
    }
}
