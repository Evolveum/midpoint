/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component.input;

import static com.evolveum.midpoint.common.MimeTypeUtil.MIME_IMAGE_JPEG;
import static com.evolveum.midpoint.common.MimeTypeUtil.getExtension;
import static com.evolveum.midpoint.web.component.input.validator.FileMagicNumberConstants.MIME_TO_MAGIC_NUMBER_BYTE;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.function.Supplier;

import javax.imageio.ImageIO;

import com.evolveum.midpoint.common.MimeTypeUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ImageFormatType;

/**
 * Provides optional image sanitization, including format conversion and
 * metadata removal.
 *
 * Sanitization rewrites the image using {@link ImageIO}. The rewrite does
 * not preserve the original image metadata.
 *
 * @author matisovaa
 *
 */
public final class ImageSanitizationUtil {

    private static final Trace LOGGER = TraceManager.getTrace(ImageSanitizationUtil.class);

    private static final Color BACKGROUND_COLOR = Color.WHITE;

    private ImageSanitizationUtil() {
    }

    /**
     * Determines file extension by comparing first bytes of file byte array with known magic numbers.
     *
     * @param fileBytes file byte array to determine file extension
     * @return file extension or null if file extension was not possible to determine
     */
    public static String getFileExtensionFromFileMagicNumber(byte[] fileBytes) {
        if (fileBytes == null) {
            return null;
        }

        magicNumbersFor:
        for (Map.Entry<String, byte[]> entry : MIME_TO_MAGIC_NUMBER_BYTE.entrySet()) {

            byte[] magicNumber = entry.getValue();

            if (fileBytes.length < magicNumber.length) {
                continue;
            }
            for (int i = 0; i < magicNumber.length; i++) {
                if (magicNumber[i] != fileBytes[i]) {
                    continue magicNumbersFor;
                }
            }
            String mime = entry.getKey();
            return MimeTypeUtil.getExtension(mime);
        }
        return null;
    }

    /**
     * Applies the requested image processing.
     *
     * If neither conversion nor metadata stripping is requested, the
     * original byte array is returned unchanged. Otherwise, the image is read
     * and written again. When no target format is specified, the original
     * format detected from the magic number is preserved.
     *
     * @param originalBytes image content to process
     * @param convertImageTo requested output format, or {@code null} to preserve
     *        the detected original format
     * @param stripMetadata whether the image should be rewritten to remove metadata
     * @return processed image content, original content if processing is not
     *         requested, or {@code null} if the input is {@code null}
     * @throws ImageSanitizationException if the image format cannot be determined
     *         or the image cannot be read or written
     */
    public static byte[] sanitizeImage(
            byte[] originalBytes,
            ImageFormatType convertImageTo,
            boolean stripMetadata)
            throws ImageSanitizationException {

        if (originalBytes == null) {
            LOGGER.debug("There is no image for sanitization.");
            return null;
        }

        if (convertImageTo == null && !stripMetadata) {
            LOGGER.debug("There is no image sanitization enabled.");
            return originalBytes;
        }

        String outputFormatName = getOutputImageFormatName(originalBytes, convertImageTo);

        if (outputFormatName == null) {
            throw new ImageSanitizationException("File format for sanitization is not recognized.");
        }

        // ImageIO reading and writing excludes the original metadata.
        BufferedImage image = readImage(originalBytes);
        return writeImage(image, outputFormatName);
    }

    /**
     * Determines the format in which the processed image should be written.
     */
    private static String getOutputImageFormatName(
            byte[] originalBytes,
            ImageFormatType convertImageTo) {

        if (convertImageTo != null) {
            return convertImageTo.value();
        }
        return getFileExtensionFromFileMagicNumber(originalBytes);
    }

    /**
     * Reads an image into a {@link BufferedImage}.
     *
     * @param imageBytes image content
     * @return decoded image
     * @throws ImageSanitizationException if the image cannot be read
     */
    private static BufferedImage readImage(byte[] imageBytes) throws ImageSanitizationException {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (image == null) {
                throw new ImageSanitizationException("Failed to read image for sanitization.");
            }
            return image;
        } catch (IOException e) {
            throw new ImageSanitizationException("Failed to read image for sanitization.", e);
        }
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
            byte[] bytes = imageToBytes(() -> image, outputImageFormatName);
            if (bytes != null) {
                return bytes;
            }

            // try to handle PNG to JPG conversion (transparency must be removed first)
            if (getExtension(MIME_IMAGE_JPEG).equals(outputImageFormatName)) {
                bytes = imageToBytes(() -> handleTransparency(image), outputImageFormatName);
                if (bytes != null) {
                    return bytes;
                }
            }

            throw new ImageSanitizationException("No " + outputImageFormatName + " writer available.");
        } catch (IOException e) {
            throw new ImageSanitizationException("Failed to write " + outputImageFormatName + " image for sanitization.", e);
        }
    }

    private static byte[] imageToBytes(Supplier<BufferedImage> imageSupplier, String outputImageFormatName) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            if (ImageIO.write(imageSupplier.get(), outputImageFormatName, bos)) {
                return bos.toByteArray();
            }
        }

        return null;
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
        Graphics g = outputImage.createGraphics();
        try {
            g.drawImage(inputImage, 0, 0, BACKGROUND_COLOR, null);
        } finally {
            g.dispose();
        }

        return outputImage;
    }
}
