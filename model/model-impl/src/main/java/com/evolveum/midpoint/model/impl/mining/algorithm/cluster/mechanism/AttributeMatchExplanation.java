/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.mining.algorithm.cluster.mechanism;

import java.util.Objects;

public class AttributeMatchExplanation {

    private String attributePath;
    private String attributeValue;

    public AttributeMatchExplanation(String attributePath, String attributeValue) {
        this.attributePath = attributePath;
        this.attributeValue = attributeValue;
    }

    public String getAttributePath() {
        return attributePath;
    }

    public void setAttributePath(String attributePath) {
        this.attributePath = attributePath;
    }

    public String getAttributeValue() {
        return attributeValue;
    }

    public void setAttributeValue(String attributeValue) {
        this.attributeValue = attributeValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {return true;}
        if (o == null || getClass() != o.getClass()) {return false;}
        AttributeMatchExplanation that = (AttributeMatchExplanation) o;
        return Objects.equals(attributePath, that.attributePath) &&
                Objects.equals(attributeValue, that.attributeValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(attributePath, attributeValue);
    }
}
