/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.icf.dummy.resource;

/**
 * @author Radovan Semancik
 *
 */
public class DummyAttributeDefinition {

    private String attributeName;
    private Class<?> attributeType;
    private boolean isRequired;
    private boolean isMulti;
    private boolean isReturnedByDefault = true;
    private boolean isReturnedAsIncomplete;
    // setting to sensitive will cause this attr to be presented as GuardedString
    private boolean sensitive;
    private String description;

    public DummyAttributeDefinition(String attributeName, Class<?> attributeType) {
        this.attributeName = attributeName;
        this.attributeType = attributeType;
        isRequired = false;
        isMulti = false;
        this.description = null;
    }

    public DummyAttributeDefinition(
            String attributeName, Class<?> attributeType, boolean isRequired, boolean isMulti) {
        this(attributeName, attributeType, isRequired, isMulti, null);
    }

    public DummyAttributeDefinition(
            String attributeName, Class<?> attributeType, boolean isRequired, boolean isMulti, String description) {
        this.attributeName = attributeName;
        this.attributeType = attributeType;
        this.isRequired = isRequired;
        this.isMulti = isMulti;
        this.description = description;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public void setAttributeName(String attributeName) {
        this.attributeName = attributeName;
    }

    public Class<?> getAttributeType() {
        return attributeType;
    }

    public void setAttributeType(Class<?> attributeType) {
        this.attributeType = attributeType;
    }

    public boolean isRequired() {
        return isRequired;
    }

    public void setRequired(boolean isRequired) {
        this.isRequired = isRequired;
    }

    public boolean isMulti() {
        return isMulti;
    }

    public void setMulti(boolean isMulti) {
        this.isMulti = isMulti;
    }

    public boolean isReturnedByDefault() {
        return isReturnedByDefault;
    }

    public void setReturnedByDefault(boolean isReturnedByDefault) {
        this.isReturnedByDefault = isReturnedByDefault;
    }

    public boolean isReturnedAsIncomplete() {
        return isReturnedAsIncomplete;
    }

    public void setReturnedAsIncomplete(boolean returnedAsIncomplete) {
        isReturnedAsIncomplete = returnedAsIncomplete;
    }

    public boolean isSensitive() {
        return sensitive;
    }

    public void setSensitive(boolean sensitive) {
        this.sensitive = sensitive;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
