/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common;

import ch.qos.logback.core.PropertyDefinerBase;
import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;

/**
 * It was simply not possible to provide an empty default value for logback property. So this is the workaround.
 * See https://stackoverflow.com/questions/44671972/empty-default-string-for-property-in-logback-xml.
 *
 * Even
 *   <if condition='isDefined("midpoint.logging.console.prefix")'>
 *       <then> ... </then>
 *       <else>
 *           <property name="prefix" value=""/>
 *       </else>
 *   </if>
 * does not work, because the "" cannot be used as a property value.
 *
 * So, the property definer is a workaround.
 */
public class LogbackPropertyDefiner extends PropertyDefinerBase {

    private String propertyName;
    private String defaultValue;

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    protected String getDefaultValue() {
        return defaultValue != null ? defaultValue : "";
    }

    @Override
    public String getPropertyValue() {
        if (propertyName == null) {
            throw new IllegalStateException("propertyName is null");
        }
        String value = System.getProperty(propertyName);
        return value != null ? value : getDefaultValue();
    }
}
