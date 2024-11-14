/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.certification.component;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import java.io.Serializable;

public class ReviewerStatisticDto implements Serializable {

    private final ObjectReferenceType reviewerRef;
    private final long notDecidedItemsCount;
    private final long allItemsCount;

    public ReviewerStatisticDto(ObjectReferenceType reviewerRef, long notDecidedItemsCount, long allItemsCount) {
        this.reviewerRef = reviewerRef;
        this.notDecidedItemsCount = notDecidedItemsCount;
        this.allItemsCount = allItemsCount;
    }

    public ObjectReferenceType getReviewerRef() {
        return reviewerRef;
    }

    public long getNotDecidedItemsCount() {
        return notDecidedItemsCount;
    }

    public long getAllItemsCount() {
        return allItemsCount;
    }

    public float getOpenNotDecidedItemsPercentage() {
        if (allItemsCount == 0) {
            return 0;
        }
        return (float) notDecidedItemsCount / allItemsCount * 100;
    }

    public float getOpenDecidedItemsPercentage() {
        if (allItemsCount == 0) {
            return 0;
        }
        return 100 - getOpenNotDecidedItemsPercentage();
    }
}
