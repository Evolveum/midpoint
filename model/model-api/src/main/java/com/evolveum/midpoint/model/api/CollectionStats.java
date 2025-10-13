/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.api;

/**
 * Stats (summary information) about a specific collection.
 *
 * @author semancik
 */
public class CollectionStats {

    private Integer objectCount;
    private Integer domainCount;

    public Integer getObjectCount() {
        return objectCount;
    }

    public void setObjectCount(Integer objectCount) {
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
