package com.evolveum.midpoint.web.component.model.delta;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReferenceValue;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Pavol
 */
public class ContainerItemDto implements Serializable {

    public static final String F_ATTRIBUTE = "attribute";
    public static final String F_VALUE = "value";

    private String attribute;
    private Object value;

    public ContainerItemDto(String attribute, Object value) {
        this.attribute = attribute;
        this.value = value;
    }

    public String getAttribute() {
        return attribute;
    }

    public Object getValue() {
        return value;
    }

    public static Collection<? extends ContainerItemDto> createContainerValueDtoList(Item item) {

        List<ContainerItemDto> retval = new ArrayList<ContainerItemDto>();

        String attribute = getItemName(item);

        for (Object o : item.getValues()) {
            if (o instanceof PrismPropertyValue) {
                retval.add(new ContainerItemDto(attribute, ValueUtil.toStringValue((PrismPropertyValue) o)));
            } else if (o instanceof PrismReferenceValue) {
                retval.add(new ContainerItemDto(attribute, ValueUtil.toStringValue((PrismReferenceValue) o)));
            } else if (o instanceof PrismContainerValue) {
                retval.add(new ContainerItemDto(attribute, new ContainerValueDto((PrismContainerValue) o)));
            }
        }

        return retval;
    }

    public static String getItemName(Item item) {
        return item.getDisplayName() != null ? item.getDisplayName() :
                (item.getName() != null ? item.getName().getLocalPart() : "?");
    }

}
