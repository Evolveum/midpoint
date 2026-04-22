/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component.input.validator;

import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import static com.evolveum.midpoint.common.MimeTypeUtil.*;

/**
 * Constants related to file validation and sanitization
 *
 * @author matisovaa
 *
 */
public final class FileMagicNumberConstants {

    public static final String MAGIC_NUMBER_JPEG = "ffd8ff";
    public static final String MAGIC_NUMBER_PNG = "89504e470d0a1a0a";

    public static final byte[] MAGIC_NUMBER_BYTE_JPEG = HexFormat.of().parseHex(MAGIC_NUMBER_JPEG);
    public static final byte[] MAGIC_NUMBER_BYTE_PNG = HexFormat.of().parseHex(MAGIC_NUMBER_PNG);

    public static final List<String> ALLOWED_UPLOAD_IMAGE_CONTENT_TYPES = Arrays.asList(MIME_IMAGE_JPEG, MIME_IMAGE_PNG);

    public static final Map<String, String> CONTENT_TYPES_TO_MAGIC_NUMBERS = Map.of(
            MIME_IMAGE_JPEG, MAGIC_NUMBER_JPEG,
            MIME_IMAGE_PNG, MAGIC_NUMBER_PNG
    );

    public static final Map<byte[], String> MAGIC_NUMBERS_TO_FILE_EXTENSIONS = Map.of(
            MAGIC_NUMBER_BYTE_JPEG, getExtensionRaw(MIME_IMAGE_JPEG),
            MAGIC_NUMBER_BYTE_PNG, getExtensionRaw(MIME_IMAGE_PNG)
    );
}
