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

import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author mederly
 */
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

    public DeltaDto(ObjectDelta delta) {

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
