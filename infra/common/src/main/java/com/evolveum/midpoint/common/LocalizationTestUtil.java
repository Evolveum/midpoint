/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common;

/**
 * For use in tests ONLY.
 *
 * @author mederly
 */
public class LocalizationTestUtil {

    private static LocalizationService localizationService = new LocalizationServiceImpl();

    public static LocalizationService getLocalizationService() {
        return localizationService;
    }
}
