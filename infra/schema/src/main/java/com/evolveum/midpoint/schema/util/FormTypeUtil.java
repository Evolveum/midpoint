/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.util;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.JAXBElement;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractFormItemType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FormItemsType;

public class FormTypeUtil {

    public static List<AbstractFormItemType> getFormItems(FormItemsType formItemsProperty) {
        List<AbstractFormItemType> items = new ArrayList<>();
        if (formItemsProperty != null) {
            for (JAXBElement<? extends AbstractFormItemType> formItem : formItemsProperty.getFormItem()) {
                AbstractFormItemType item = formItem.getValue();
                items.add(item);
            }
        }
        return items;
    }

}
