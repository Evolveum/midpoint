/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.api.authentication;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ImageFormatType;

/**
 * Effective upload validation and image-processing policy for one item path.
 *
 * The policy contains resolved values ready for runtime use, including
 * built-in defaults and values compiled from the GUI configuration.
 */
public class EffectiveFileUploadPolicy implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final ItemPath path;
    private final boolean contentTypeCheckEnabled;
    private final List<String> allowedContentTypes;
    private final ImageFormatType convertImageTo;
    private final boolean stripMetadata;

    public EffectiveFileUploadPolicy(
            ItemPath path,
            boolean contentTypeCheckEnabled,
            List<String> allowedContentTypes,
            ImageFormatType convertImageTo,
            boolean stripMetadata) {
        this.path = path;
        this.contentTypeCheckEnabled = contentTypeCheckEnabled;
        this.allowedContentTypes = List.copyOf(allowedContentTypes);
        this.convertImageTo = convertImageTo;
        this.stripMetadata = stripMetadata;
    }

    public ItemPath getPath() {
        return path;
    }

    public boolean isContentTypeCheckEnabled() {
        return contentTypeCheckEnabled;
    }

    public List<String> getAllowedContentTypes() {
        return allowedContentTypes;
    }

    public ImageFormatType getConvertImageTo() {
        return convertImageTo;
    }

    public boolean isStripMetadata() {
        return stripMetadata;
    }
}
