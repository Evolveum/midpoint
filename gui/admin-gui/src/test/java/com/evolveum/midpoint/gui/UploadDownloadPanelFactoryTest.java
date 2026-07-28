/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui;

import static com.evolveum.midpoint.web.component.FileTestConstants.jpegBytes;
import static com.evolveum.midpoint.web.component.FileTestConstants.pngBytes;
import static org.testng.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import com.evolveum.midpoint.gui.impl.factory.panel.UploadDownloadPanelFactory;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.web.component.input.ImageSanitizationUtil;
import com.evolveum.midpoint.web.component.input.UploadDownloadPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ImageFormatType;

/**
 * Tests download filename resolution for uploaded binary values based on the
 * detected content type of the stored data.
 */
public class UploadDownloadPanelFactoryTest {

    @Test
    public void testStoredPngBytesProducePngDownloadFilename() throws Exception {
        UploadDownloadPanel panel = panelWithStoredBytes(pngBytes());

        String fileName = UploadDownloadPanelFactory.getDownloadFileName(
                ItemName.fromQName(FocusType.F_JPEG_PHOTO),
                panel.getDownloadContentType());

        assertEquals(fileName, "jpegPhoto.png");
    }

    @Test
    public void testStoredJpegBytesProduceJpegDownloadFilename() throws Exception {
        UploadDownloadPanel panel = panelWithStoredBytes(jpegBytes());

        String fileName = UploadDownloadPanelFactory.getDownloadFileName(
                ItemName.fromQName(FocusType.F_JPEG_PHOTO),
                panel.getDownloadContentType());

        assertEquals(fileName, "jpegPhoto.jpg");
    }

    @Test
    public void testConvertedOutputBytesDetermineDownloadFilename() throws Exception {
        byte[] converted = ImageSanitizationUtil.sanitizeImage(jpegBytes(), ImageFormatType.PNG, false);
        UploadDownloadPanel panel = panelWithStoredBytes(converted);

        String fileName = UploadDownloadPanelFactory.getDownloadFileName(
                ItemName.fromQName(FocusType.F_JPEG_PHOTO),
                panel.getDownloadContentType());

        assertEquals(fileName, "jpegPhoto.png");
    }

    private UploadDownloadPanel panelWithStoredBytes(byte[] bytes) {
        return new UploadDownloadPanel("upload", false) {
            @Override
            public InputStream getInputStream() {
                return new ByteArrayInputStream(bytes);
            }
        };
    }
}
