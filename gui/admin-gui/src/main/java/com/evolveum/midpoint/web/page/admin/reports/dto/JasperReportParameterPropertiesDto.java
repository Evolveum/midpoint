/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.reports.dto;

import java.io.Serializable;

import net.sf.jasperreports.engine.JRPropertiesMap;

public class JasperReportParameterPropertiesDto implements Serializable{
    private static final String PROPERTY_KEY = "key";
    private static final String PROPERTY_LABEL = "label";
    private static final String PROPERTY_TARGET_TYPE = "targetType";
    private static final String PROPERTY_MULTIVALUE = "multivalue";
    private static final String MANDATORY_KEY = "mandatory";


    private JRPropertiesMap propertiesMap;

    public JasperReportParameterPropertiesDto(JRPropertiesMap propertiesMap) {
        this.propertiesMap = propertiesMap;
    }

    public String getLabel() {
        if (propertiesMap == null) {
            return null;
        }

        return propertiesMap.getProperty(PROPERTY_LABEL);
    }

    public void setLabel(String label) {
        if (propertiesMap == null) {
            propertiesMap = new JRPropertiesMap();
        }

        propertiesMap.setProperty(PROPERTY_LABEL, label);
    }

//    public boolean getMultivalue() {
//        if (propertiesMap == null) {
//            return false;
//        }
//
//        return Boolean.parseBoolean(propertiesMap.getProperty(PROPERTY_MULTIVALUE));
//    }
//
//    public void setMultivalue(boolean isMultiValue) {
//        if (propertiesMap == null) {
//            propertiesMap = new JRPropertiesMap();
//        }
//
//        propertiesMap.setProperty(PROPERTY_MULTIVALUE, String.valueOf(isMultiValue));
//    }

    public String getTargetType() {
        if (propertiesMap == null) {
            return null;
        }

        return propertiesMap.getProperty(PROPERTY_TARGET_TYPE);
    }

    public void setTargetType(String targetType) {
        if (propertiesMap == null) {
            propertiesMap = new JRPropertiesMap();
        }

        propertiesMap.setProperty(PROPERTY_TARGET_TYPE, targetType);
    }


    public String getKey() {
        if (propertiesMap == null) {
            return null;
        }

        return propertiesMap.getProperty(PROPERTY_KEY);
    }

    public void setKey(String key) {
        if (propertiesMap == null) {
            propertiesMap = new JRPropertiesMap();
        }

        propertiesMap.setProperty(PROPERTY_KEY, key);
    }

    public boolean getMandatory() {
        if (propertiesMap == null) {
            return false;
        }
        String isMandatory = propertiesMap.getProperty(MANDATORY_KEY);
        return Boolean.parseBoolean(isMandatory);
    }

    public void setMandatory(boolean isMandatory) {
        if (propertiesMap == null) {
            propertiesMap = new JRPropertiesMap();
        }

        propertiesMap.setProperty(MANDATORY_KEY, String.valueOf(isMandatory));
    }

    public JRPropertiesMap getPropertiesMap() {
        return propertiesMap;
    }
}
