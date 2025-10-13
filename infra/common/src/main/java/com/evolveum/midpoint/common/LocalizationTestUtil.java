/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.common;

/**
 * For use in tests ONLY.
 */
public class LocalizationTestUtil {

    private static final LocalizationService SERVICE_INSTANCE = new LocalizationServiceImpl();

    public static LocalizationService getLocalizationService() {
        return SERVICE_INSTANCE;
    }
}
