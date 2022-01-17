/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.cases.component;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IdMatchCorrelationPotentialMatchType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAttributesType;
import com.evolveum.prism.xml.ns._public.types_3.RawType;

import java.io.Serializable;

public class PotentialMatchDto implements Serializable {

    private ShadowAttributesType shadowAttributesType;
    private String referenceId;
    private String uri;
    private boolean origin = false;

    public PotentialMatchDto(IdMatchCorrelationPotentialMatchType potentialMatchType) {
        this.referenceId = potentialMatchType.getReferenceId();
        this.uri = potentialMatchType.getUri();
        this.shadowAttributesType = potentialMatchType.getAttributes();
    }

    public void setOrigin(boolean origin) {
        this.origin = origin;
    }

    public Serializable getAttributeValue(ItemPath path) {
        Item pItem = findMatchingItem(path);
        Serializable value = pItem == null ? null : (Serializable) pItem.getRealValue();
//                        String realValue;
        if (value instanceof RawType) {
            try {
                value = ((RawType) value).getParsedRealValue(String.class);
            } catch (SchemaException e) {
                ///TODO
            }
        }
        return value;
    }

    private Item findMatchingItem(ItemPath path) {
        Item exactMatch = shadowAttributesType.asPrismContainerValue().findItem(path);
        if (exactMatch != null) {
            return exactMatch;
        }
        ItemName name = path.asSingleName();
        if (name == null) {
            // Not bothering with the general case; we don't have nested attributes anyway
            return null;
        }

        for (Item<?, ?> item : ((PrismContainerValue<?>) shadowAttributesType.asPrismContainerValue()).getItems()) {
            if (QNameUtil.match(name, item.getElementName(), true)) {
                return item;
            }
        }
        return null;
    }

    public ShadowAttributesType getShadowAttributesType() {
        return shadowAttributesType;
    }

    public boolean isOrigin() {
        return origin;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public String getUri() {
        return uri;
    }
}
