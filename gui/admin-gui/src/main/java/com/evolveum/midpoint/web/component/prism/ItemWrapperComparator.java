/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.component.prism;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinition;

import org.apache.commons.lang.StringUtils;

import java.io.Serializable;
import java.util.Comparator;

/**
 * @author lazyman
 */
public class ItemWrapperComparator implements Comparator<ItemWrapper>, Serializable {

    @Override
    public int compare(ItemWrapper p1, ItemWrapper p2) {
        ItemDefinition def1 = p1.getItemDefinition();
        ItemDefinition def2 = p2.getItemDefinition();

        if (isMainContainer(p1)) {
            return -1;
        }
        if (isMainContainer(p2)) {
            return 1;
        }
        
        if (def1 instanceof PrismContainerDefinition) {
        	return 1;
        }
        
        if (def2 instanceof PrismContainerDefinition) {
        	return 1;
        }
        
        Integer index1 = null;
        if (def1 != null) {
        	index1 = def1.getDisplayOrder();
        } 
        
        Integer index2 = def2.getDisplayOrder();
        if (index1 != null && index2 != null) {
            return index1 - index2;
        } else if (index1 != null && index2 == null) {
            return -1;
        } else if (index1 == null && index2 != null) {
            return 1;
        } else if (index1 == index2) {
        	return 0;
        }

        return String.CASE_INSENSITIVE_ORDER.compare(getDisplayName(def1), getDisplayName(def2));
    }

    private String getDisplayName(ItemDefinition def) {
        String displayName = def.getDisplayName();

        if (StringUtils.isNotEmpty(displayName)) {
            return displayName;
        }

        return def.getName().getLocalPart();
    }

    private boolean isMainContainer(ItemWrapper wrapper) {
        if (!(wrapper instanceof ContainerWrapper)) {
            return false;
        }

        ContainerWrapper container = (ContainerWrapper) wrapper;
        return container.isMain();
    }
}
