/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.api.prism.wrapper;

import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.prism.PrismContainerValue;

import java.io.Serializable;

@FunctionalInterface
public interface ItemMandatoryHandler extends Serializable {

    boolean isMandatory(ItemWrapper<?, ?> itemWrapper);

    /**
     * In some cases it is necessary to skip validation. E.g. on the generated form panel, when empty (parent) container
     * is displayed, and no change was applied to this container, there is no need to require for an itemWrapper value
     * to be present/mandatory.
     * Connected to #10210
     * @param itemWrapper
     * @return
     */
    default boolean skipValidation(ItemWrapper<?, ?> itemWrapper) {
        if (itemWrapper == null) {
             return false;
        }
        PrismContainerValueWrapper parentContainer = itemWrapper.getParent();
        if (parentContainer != null && parentContainer.getNewValue() != null
                && parentContainer.getParent() != null && !parentContainer.getParent().isMultiValue()) {
            PrismContainerValue cleanedUpValue =
                    WebPrismUtil.cleanupEmptyContainerValue(parentContainer.getNewValue().clone());
            return cleanedUpValue == null || cleanedUpValue.isEmpty();
        }
        return false;
    }

}
