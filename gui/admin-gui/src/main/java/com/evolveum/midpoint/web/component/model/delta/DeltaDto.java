/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component.model.delta;

import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class DeltaDto implements Serializable {

    public static final String F_CHANGE_TYPE = "changeType";
    public static final String F_OID = "oid";
    public static final String F_OBJECT_TO_ADD = "objectToAdd";
    public static final String F_MODIFICATIONS = "modifications";

    private String changeType;
    private String oid;
    private List<ModificationDto> modifications;
    private ContainerValueDto objectToAdd;
    private boolean add;

    public DeltaDto(ObjectDelta<?> delta) {

        changeType = "" + delta.getChangeType();
        oid = delta.getOid();
        if (delta.getObjectToAdd() != null) {
            objectToAdd = new ContainerValueDto(delta.getObjectToAdd().getValue());
        }

        add = delta.isAdd();

        modifications = new ArrayList<>();
        for (Object itemDelta : delta.getModifications()) {
            if (itemDelta instanceof PropertyDelta) {
                modifications.addAll(ModificationDto.createModificationDtoList((PropertyDelta) itemDelta));
            } else if (itemDelta instanceof ReferenceDelta) {
                modifications.addAll(ModificationDto.createModificationDtoList((ReferenceDelta) itemDelta));
            } else if (itemDelta instanceof ContainerDelta) {
                modifications.addAll(ModificationDto.createModificationDtoList((ContainerDelta) itemDelta));
            } else {
                throw new IllegalStateException("Unknown kind of itemDelta: " + itemDelta);
            }
        }
    }

    public String getChangeType() {
        return changeType;
    }

    public String getOid() {
        return oid;
    }

    public List<ModificationDto> getModifications() {
        return modifications;
    }

    public ContainerValueDto getObjectToAdd() {
        return objectToAdd;
    }

    public boolean isAdd() {
        return add;
    }

    public void setAdd(boolean add) {
        this.add = add;
    }
}
