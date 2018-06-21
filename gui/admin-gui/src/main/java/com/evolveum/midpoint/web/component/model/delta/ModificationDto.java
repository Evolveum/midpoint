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
import com.evolveum.midpoint.schema.util.ValueDisplayUtil;

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
