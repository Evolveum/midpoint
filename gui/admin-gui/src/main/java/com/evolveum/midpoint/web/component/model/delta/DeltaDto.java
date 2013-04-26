/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2013 [name of copyright owner]
 */

package com.evolveum.midpoint.web.component.model.delta;

import com.evolveum.midpoint.model.api.context.ModelState;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.*;

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

        modifications = new ArrayList<ModificationDto>();
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
