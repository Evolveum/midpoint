/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OtherPrivilegesLimitationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemSelectorType;

import javax.xml.namespace.QName;
import java.util.List;

public class SchemaDeputyUtil {

    /**
     * The limitations list contains all "filters" that should be applied. This means that if a single record blocks the
     * flow, the overall answer is negative.
     */
    public static boolean limitationsAllow(List<OtherPrivilegesLimitationType> limitations, QName itemName) {
        for (OtherPrivilegesLimitationType limitation : limitations) {
            @SuppressWarnings({ "unchecked", "raw" })
            PrismContainer<WorkItemSelectorType> selector = limitation.asPrismContainerValue().findContainer(itemName);
            if (selector == null || selector.isEmpty() || !selector.getRealValue().isAll()) {
                return false;
            }
        }
        return true;
    }
}
