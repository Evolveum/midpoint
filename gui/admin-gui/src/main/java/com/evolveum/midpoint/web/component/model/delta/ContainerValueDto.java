/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component.model.delta;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainerValue;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Pavol
 */
public class ContainerValueDto implements Serializable {

    public static final String F_ITEM_LIST = "itemList";

    private List<ContainerItemDto> itemList;

    public ContainerValueDto(PrismContainerValue value) {

        itemList = new ArrayList<>();

        for (Object o : value.getItems()) {
            itemList.addAll(ContainerItemDto.createContainerValueDtoList((Item) o));
        }

        Collections.sort(itemList);
    }
}
