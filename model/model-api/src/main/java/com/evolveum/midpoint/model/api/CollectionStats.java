/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.api;

/**
 * Stats (summary information) about a specific collection.
 *
 * @author semancik
 */
public class CollectionStats {

    private int objectCount;
    private Integer domainCount;

    public int getObjectCount() {
        return objectCount;
    }

    public void setObjectCount(int objectCount) {
        this.objectCount = objectCount;
    }

    public Integer getDomainCount() {
        return domainCount;
    }

    public void setDomainCount(Integer domainCount) {
        this.domainCount = domainCount;
    }

    public Float computePercentage() {
        if (domainCount == null) {
            return null;
        }
        return ((float)objectCount * 100f) / ((float)domainCount);
    }

    @Override
    public String toString() {
        return "CollectionStats(" + objectCount + "/" + domainCount + ")";
    }

}
