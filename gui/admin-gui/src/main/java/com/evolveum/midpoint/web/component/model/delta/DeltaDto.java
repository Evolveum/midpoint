/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.model.delta;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;

public class DeltaDto implements Serializable {

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
