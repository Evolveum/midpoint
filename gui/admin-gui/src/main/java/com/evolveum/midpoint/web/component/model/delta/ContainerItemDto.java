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

package com.evolveum.midpoint.web.component.model.delta;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.schema.util.ValueDisplayUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Pavol
 */
public class ContainerItemDto implements Serializable, Comparable {

    public static final String F_ATTRIBUTE = "attribute";
    public static final String F_VALUE = "value";

    private String attribute;
    private Object value;
    private int order;

    public ContainerItemDto(String attribute, Object value, int order) {
        this.attribute = attribute;
        this.value = value;
        this.order = order;
    }

    public String getAttribute() {
        return attribute;
    }

    public Object getValue() {
        return value;
    }

    public int getOrder() {
        return order;
    }

    public static Collection<? extends ContainerItemDto> createContainerValueDtoList(Item item) {

        List<ContainerItemDto> retval = new ArrayList<>();

        String attribute = getItemName(item);

        for (Object o : item.getValues()) {
            if (o instanceof PrismPropertyValue) {
                retval.add(new ContainerItemDto(attribute, ValueDisplayUtil.toStringValue((PrismPropertyValue) o), getOrder(o)));
            } else if (o instanceof PrismReferenceValue) {
                retval.add(new ContainerItemDto(attribute, ValueDisplayUtil.toStringValue((PrismReferenceValue) o), getOrder(o)));
            } else if (o instanceof PrismContainerValue) {
                retval.add(new ContainerItemDto(attribute, new ContainerValueDto((PrismContainerValue) o), getOrder(o)));
            }
        }

        return retval;
    }

    private static int getOrder(Object o) {
        if (o instanceof PrismValue) {
            PrismValue value = (PrismValue) o;
            if (value.getParent() != null && value.getParent().getDefinition() != null) {
                ItemDefinition itemDefinition = value.getParent().getDefinition();
                if (itemDefinition.getDisplayOrder() != null) {
                    return itemDefinition.getDisplayOrder();
                }
            }
        }
        return Integer.MAX_VALUE;
    }

    public static String getItemName(Item item) {
        return item.getDisplayName() != null ? item.getDisplayName() :
                (item.getElementName() != null ? item.getElementName().getLocalPart() : "?");
    }

    @Override
    public int compareTo(Object o) {
        if (o instanceof ContainerItemDto) {
            ContainerItemDto other = (ContainerItemDto) o;
            return ((Integer) this.order).compareTo(other.getOrder());
        } else {
            return -1;
        }
    }

    @Override
    public String toString() {
        return "ContainerItemDto{" +
                "attribute='" + attribute + '\'' +
                ", value=" + value +
                ", order=" + order +
                '}';
    }
}
