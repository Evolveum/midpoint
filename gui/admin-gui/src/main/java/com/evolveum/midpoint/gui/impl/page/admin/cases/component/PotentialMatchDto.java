/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.cases.component;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.path.ItemPath;
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
        Item pItem = shadowAttributesType.asPrismContainerValue().findItem(path);
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
