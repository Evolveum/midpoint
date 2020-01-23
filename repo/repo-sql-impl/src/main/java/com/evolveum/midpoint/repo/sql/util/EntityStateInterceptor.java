/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.util;

import com.evolveum.midpoint.repo.sql.data.common.RFocusPhoto;
import com.evolveum.midpoint.repo.sql.data.common.RObjectReference;
import com.evolveum.midpoint.repo.sql.data.common.any.RAExtValue;
import com.evolveum.midpoint.repo.sql.data.common.any.RAssignmentExtension;
import com.evolveum.midpoint.repo.sql.data.common.any.ROExtValue;
import com.evolveum.midpoint.repo.sql.data.common.container.RAssignmentReference;
import com.evolveum.midpoint.repo.sql.data.common.container.RContainerReference;
import org.hibernate.EmptyInterceptor;

/**
 * @author lazyman
 */
public class EntityStateInterceptor extends EmptyInterceptor {

    @Override
    public Boolean isTransient(Object entity) {
        if (entity instanceof EntityState) {
            return isTransient((EntityState) entity);
        }

        if (entity instanceof RObjectReference) {
            RObjectReference ref = (RObjectReference) entity;
            return isTransient(ref.getOwner());
        } else if (entity instanceof RAssignmentReference) {
            RContainerReference ref = (RContainerReference) entity;
            return isTransient(ref.getOwner());
        } else if (entity instanceof RAssignmentExtension) {
            RAssignmentExtension ext = (RAssignmentExtension) entity;
            return isTransient(ext.getOwner());
        } else if (entity instanceof RAExtValue) {
            RAExtValue val = (RAExtValue) entity;
            RAssignmentExtension ext = val.getAnyContainer();
            return ext != null ? isTransient(ext.getOwner()) : null;
        } else if (entity instanceof ROExtValue) {
            ROExtValue val = (ROExtValue) entity;
            return isTransient(val.getOwner());
        } else if (entity instanceof RFocusPhoto) {
            RFocusPhoto photo = (RFocusPhoto) entity;
            return isTransient(photo.getOwner());
        }

        return null;
    }

    private Boolean isTransient(EntityState object) {
        return isTransient(object, false);
    }

    private Boolean isTransient(EntityState object, boolean isObjectMyParent) {
        Boolean trans = object != null ? object.isTransient() : null;
        if (!isObjectMyParent) {
            return trans;
        }
        if (Boolean.TRUE.equals(trans)) {
            return true;
        }

        return null;
    }
}
