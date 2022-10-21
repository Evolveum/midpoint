/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.model.delta;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.util.ValueDisplayUtil;

/**
 * Preliminary version.
 */
public class ModificationDto implements Serializable {

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

    public String getAttribute() {
        return attribute;
    }

    public String getChangeType() {
        return changeType;
    }

    public boolean isPropertyDelta() {
        return isPropertyDelta;
    }

    public Object getValue() {
        return value;
    }

    public static Collection<? extends ModificationDto> createModificationDtoList(PropertyDelta delta) {

        String attribute = getItemName(delta);

        List<ModificationDto> retval = new ArrayList<>();
        if (delta.getValuesToAdd() != null) {
            for (Object valueToAdd : delta.getValuesToAdd()) {
                retval.add(new ModificationDto(attribute, ADD, ValueDisplayUtil.toStringValue((PrismPropertyValue) valueToAdd)));
            }
        }
        if (delta.getValuesToReplace() != null) {
            for (Object valueToAdd : delta.getValuesToReplace()) {
                retval.add(new ModificationDto(attribute, REPLACE, ValueDisplayUtil.toStringValue((PrismPropertyValue) valueToAdd)));
            }
        }
        if (delta.getValuesToDelete() != null) {
            for (Object valueToAdd : delta.getValuesToDelete()) {
                retval.add(new ModificationDto(attribute, DELETE, ValueDisplayUtil.toStringValue((PrismPropertyValue) valueToAdd)));
            }
        }
        return retval;
    }

    private static String getItemName(ItemDelta delta) {
        if (delta.getDefinition() != null) {
            if (delta.getDefinition().getDisplayName() != null) {
                return delta.getDefinition().getDisplayName();
            }
            if (delta.getDefinition().getItemName() != null) {
                return delta.getDefinition().getItemName().getLocalPart();
            }
        }
        ItemPath path = delta.getPath();
        List<?> segments = path.getSegments();
        for (int i = segments.size() - 1; i >= 0; i--) {
            Object component = segments.get(i);
            if (ItemPath.isName(component)) {
                StringBuilder retval = new StringBuilder(ItemPath.toName(component).getLocalPart());
                i++;
                while (i < segments.size()) {
                    Object nextComponent = segments.get(i);
                    if (ItemPath.isId(nextComponent)) {     // should always be the case
                        retval.append("[").append(ItemPath.toId(nextComponent)).append("]");
                    }
                }
                return retval.toString();
            }
        }

        return delta.toString();    // this means there's some problem there
    }

    public static Collection<? extends ModificationDto> createModificationDtoList(ReferenceDelta delta) {

        String attribute = getItemName(delta);

        List<ModificationDto> retval = new ArrayList<>();
        if (delta.getValuesToAdd() != null) {
            for (Object valueToAdd : delta.getValuesToAdd()) {
                retval.add(new ModificationDto(attribute, ADD, ValueDisplayUtil.toStringValue((PrismReferenceValue) valueToAdd)));
            }
        }
        if (delta.getValuesToReplace() != null) {
            for (Object valueToAdd : delta.getValuesToReplace()) {
                retval.add(new ModificationDto(attribute, REPLACE, ValueDisplayUtil.toStringValue((PrismReferenceValue) valueToAdd)));
            }
        }
        if (delta.getValuesToDelete() != null) {
            for (Object valueToAdd : delta.getValuesToDelete()) {
                retval.add(new ModificationDto(attribute, DELETE, ValueDisplayUtil.toStringValue((PrismReferenceValue) valueToAdd)));
            }
        }
        return retval;
    }

    public static Collection<? extends ModificationDto> createModificationDtoList(ContainerDelta delta) {

        String attribute = getItemName(delta);

        List<ModificationDto> retval = new ArrayList<>();
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
