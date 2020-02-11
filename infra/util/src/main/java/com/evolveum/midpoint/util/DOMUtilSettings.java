/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.util;

/**
 * Created by Viliam Repan (lazyman).
 */
public class DOMUtilSettings {

    private static boolean addTransformerFactorySystemProperty = true;

    public static boolean isAddTransformerFactorySystemProperty() {
        return addTransformerFactorySystemProperty;
    }

    /**
     * Method used by MidPoint Studio to disable setting system property during {@link DOMUtil} initialization.
     * Not used within MidPoint as the default "true" value doesn't change the initialization behaviour.
     *
     * @param addTransformerFactorySystemProperty
     */
    public static void setAddTransformerFactorySystemProperty(boolean addTransformerFactorySystemProperty) {
        DOMUtilSettings.addTransformerFactorySystemProperty = addTransformerFactorySystemProperty;
    }
}
