/*
 * Copyright (c) 2010-2019 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
