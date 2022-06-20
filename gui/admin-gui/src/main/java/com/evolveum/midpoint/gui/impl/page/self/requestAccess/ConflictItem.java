/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.self.requestAccess;

import java.io.Serializable;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ConflictItem implements Serializable {

    private ObjectReferenceType ref;

    boolean oldAssignment = false;

    public ConflictItem(PrismObject obj, boolean oldAssignment) {
        this.oldAssignment = oldAssignment;

        if (obj == null) {
            return;
        }

        ObjectReferenceType ref = new ObjectReferenceType();
        ref.setOid(obj.getOid());
        ref.setType(ObjectTypes.getObjectType(obj.getCompileTimeClass()).getTypeQName());
        ref.setTargetName(obj.asObjectable().getName());

        this.ref = ref;
    }

    public ConflictItem(ObjectReferenceType ref) {
        this.ref = ref;
    }

    public String getName() {
        return WebComponentUtil.getName(ref);
    }
}
