package com.evolveum.midpoint.web.component.model.delta;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Pavol
 */
public class ContainerValueDto implements Serializable {

    public static final String F_ITEM_LIST = "itemList";

    private List<ContainerItemDto> itemList;

    public ContainerValueDto(PrismContainerValue value) {

        itemList = new ArrayList<ContainerItemDto>();

        for (Object o : value.getItems()) {
            itemList.addAll(ContainerItemDto.createContainerValueDtoList((Item) o));
        }
    }

    public ContainerValueDto(PrismContainer container) {
        this(container.getValue());
    }
}
