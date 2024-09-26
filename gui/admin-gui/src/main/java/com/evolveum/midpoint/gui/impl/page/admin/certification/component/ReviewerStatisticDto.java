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

    private ObjectReferenceType reviewerRef;
    private long openNotDecidedItemsCount;
    private long allOpenItemsCount;

    public ReviewerStatisticDto(ObjectReferenceType reviewerRef, long openNotDecidedItemsCount, long allOpenItemsCount) {
        this.reviewerRef = reviewerRef;
        this.openNotDecidedItemsCount = openNotDecidedItemsCount;
        this.allOpenItemsCount = allOpenItemsCount;
    }

    public ObjectReferenceType getReviewerRef() {
        return reviewerRef;
    }

    public long getOpenNotDecidedItemsCount() {
        return openNotDecidedItemsCount;
    }

    public long getAllOpenItemsCount() {
        return allOpenItemsCount;
    }

    public float getOpenNotDecidedItemsPercentage() {
        if (allOpenItemsCount == 0) {
            return 0;
        }
        return (float) openNotDecidedItemsCount / allOpenItemsCount * 100;
    }

    public float getOpenDecidedItemsPercentage() {
        if (allOpenItemsCount == 0) {
            return 0;
        }
        return 100 - getOpenNotDecidedItemsPercentage();
    }
}
