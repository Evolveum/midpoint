package com.evolveum.midpoint.web.component.model.delta;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.path.IdItemPathSegment;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathSegment;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Preliminary version.
 *
 * @author mederly
 */
public class ModificationDto implements Serializable {

    public static final String F_ATTRIBUTE = "attribute";
    public static final String F_CHANGE_TYPE = "changeType";
    public static final String F_VALUE = "value";
    private static final String ADD = "ADD";
    private static final String REPLACE = "REPLACE";
    private static final String DELETE = "DELETE";

    private String attribute;
    private String changeType;
    private Object value;

    private boolean isPropertyDelta;

    public ModificationDto(String attribute, String type, Object value) {
        this.attribute = attribute;
        this.changeType = type;
        this.value = value;
        isPropertyDelta = true;
    }

    public Object getValue() {
        return value;
    }

    public static Collection<? extends ModificationDto> createModificationDtoList(PropertyDelta delta) {

        String attribute = getItemName(delta);

        List<ModificationDto> retval = new ArrayList<ModificationDto>();
        if (delta.getValuesToAdd() != null) {
            for (Object valueToAdd : delta.getValuesToAdd()) {
                retval.add(new ModificationDto(attribute, ADD, ValueUtil.toStringValue((PrismPropertyValue) valueToAdd)));
            }
        }
        if (delta.getValuesToReplace() != null) {
            for (Object valueToAdd : delta.getValuesToReplace()) {
                retval.add(new ModificationDto(attribute, REPLACE, ValueUtil.toStringValue((PrismPropertyValue) valueToAdd)));
            }
        }
        if (delta.getValuesToDelete() != null) {
            for (Object valueToAdd : delta.getValuesToDelete()) {
                retval.add(new ModificationDto(attribute, DELETE, ValueUtil.toStringValue((PrismPropertyValue) valueToAdd)));
            }
        }
        return retval;
    }

    private static String getItemName(ItemDelta delta) {
        if (delta.getDefinition() != null) {
            if (delta.getDefinition().getDisplayName() != null) {
                return delta.getDefinition().getDisplayName();
            }
            if (delta.getDefinition().getName() != null) {
                return delta.getDefinition().getName().getLocalPart();
            }
        }
        ItemPath path = delta.getPath();
        for (int i = path.getSegments().size()-1; i >= 0; i--) {
            if (path.getSegments().get(i) instanceof NameItemPathSegment) {
                String retval = ((NameItemPathSegment) path.getSegments().get(i)).getName().getLocalPart();
                i++;
                while (i < path.getSegments().size()) {
                    ItemPathSegment itemPathSegment = path.getSegments().get(i);
                    if (itemPathSegment instanceof IdItemPathSegment) {     // should always be the case
                        retval += "[" + ((IdItemPathSegment) itemPathSegment).getId() + "]";
                    }
                }
                return retval;
            }
        }

        return delta.toString();    // this means there's some problem there
    }

    public static Collection<? extends ModificationDto> createModificationDtoList(ReferenceDelta delta) {

        String attribute = getItemName(delta);

        List<ModificationDto> retval = new ArrayList<ModificationDto>();
        if (delta.getValuesToAdd() != null) {
            for (Object valueToAdd : delta.getValuesToAdd()) {
                retval.add(new ModificationDto(attribute, ADD, ValueUtil.toStringValue((PrismReferenceValue) valueToAdd)));
            }
        }
        if (delta.getValuesToReplace() != null) {
            for (Object valueToAdd : delta.getValuesToReplace()) {
                retval.add(new ModificationDto(attribute, REPLACE, ValueUtil.toStringValue((PrismReferenceValue) valueToAdd)));
            }
        }
        if (delta.getValuesToDelete() != null) {
            for (Object valueToAdd : delta.getValuesToDelete()) {
                retval.add(new ModificationDto(attribute, DELETE, ValueUtil.toStringValue((PrismReferenceValue) valueToAdd)));
            }
        }
        return retval;
    }

    public static Collection<? extends ModificationDto> createModificationDtoList(ContainerDelta delta) {

        String attribute = getItemName(delta);

        List<ModificationDto> retval = new ArrayList<ModificationDto>();
        if (delta.getValuesToAdd() != null) {
            for (Object valueToAdd : delta.getValuesToAdd()) {
                retval.add(new ModificationDto(attribute, ADD, new ContainerValueDto((PrismContainerValue) valueToAdd)));
            }
        }
        if (delta.getValuesToReplace() != null) {
            for (Object valueToReplace : delta.getValuesToReplace()) {
                retval.add(new ModificationDto(attribute, REPLACE, new ContainerValueDto((PrismContainerValue) valueToReplace)));
            }
        }
        if (delta.getValuesToDelete() != null) {
            for (Object valueToDelete : delta.getValuesToDelete()) {
                retval.add(new ModificationDto(attribute, DELETE, new ContainerValueDto((PrismContainerValue) valueToDelete)));
            }
        }
        return retval;
    }

}
