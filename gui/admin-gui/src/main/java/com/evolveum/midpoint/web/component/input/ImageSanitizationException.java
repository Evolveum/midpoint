/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component.input;

/**
 * @author matisovaa
 */
public class ImageSanitizationException extends Exception {

    public ImageSanitizationException(String s) {
        super(s);
    }

    public ImageSanitizationException(String s, Throwable throwable) {
        super(s, throwable);
    }
}
