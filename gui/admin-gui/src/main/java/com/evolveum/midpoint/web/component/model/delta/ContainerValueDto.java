/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.model.delta;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainerValue;

/**
 * @author Pavol
 */
public class ContainerValueDto implements Serializable {

    private List<ContainerItemDto> itemList;

    public ContainerValueDto(PrismContainerValue value) {
        itemList = new ArrayList<>();

        for (Object o : value.getItems()) {
            itemList.addAll(ContainerItemDto.createContainerValueDtoList((Item) o));
        }

        Collections.sort(itemList);
    }

    public List<ContainerItemDto> getItemList() {
        return itemList;
    }
}
