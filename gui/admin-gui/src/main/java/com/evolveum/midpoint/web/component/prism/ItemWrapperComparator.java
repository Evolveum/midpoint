/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.web.component.prism;

import com.evolveum.midpoint.prism.ItemDefinition;
import org.apache.commons.lang.StringUtils;

import java.util.Comparator;

/**
 * @author lazyman
 */
public class ItemWrapperComparator implements Comparator<ItemWrapper> {

    @Override
    public int compare(ItemWrapper p1, ItemWrapper p2) {
        ItemDefinition def1 = p1.getItem().getDefinition();
        ItemDefinition def2 = p2.getItem().getDefinition();

        if (isMainContainer(p1)) {
            return -1;
        }
        if (isMainContainer(p2)) {
            return 1;
        }

        Integer index1 = def1.getDisplayOrder();
        Integer index2 = def2.getDisplayOrder();
        if (index1 != null && index2 != null) {
            return index1 - index2;
        } else if (index1 != null && index2 == null) {
            return -1;
        } else if (index1 == null && index2 != null) {
            return 1;
        }

        return String.CASE_INSENSITIVE_ORDER.compare(getDisplayName(def1), getDisplayName(def2));
    }

    private String getDisplayName(ItemDefinition def) {
        String displayName = def.getDisplayName();

        if (StringUtils.isNotEmpty(displayName)) {
            return displayName;
        }

        return def.getNameOrDefaultName().getLocalPart();
    }

    private boolean isMainContainer(ItemWrapper wrapper) {
        if (!(wrapper instanceof ContainerWrapper)) {
            return false;
        }

        ContainerWrapper container = (ContainerWrapper) wrapper;
        return container.isMain();
    }
}
