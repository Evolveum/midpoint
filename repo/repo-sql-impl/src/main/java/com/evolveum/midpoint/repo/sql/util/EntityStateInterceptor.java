package com.evolveum.midpoint.repo.sql.util;

import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.RObjectReference;
import com.evolveum.midpoint.repo.sql.data.common.RUserPhoto;
import com.evolveum.midpoint.repo.sql.data.common.any.RAExtValue;
import com.evolveum.midpoint.repo.sql.data.common.any.RAssignmentExtension;
import com.evolveum.midpoint.repo.sql.data.common.any.ROExtValue;
import com.evolveum.midpoint.repo.sql.data.common.container.Container;
import com.evolveum.midpoint.repo.sql.data.common.container.RAssignmentReference;
import org.hibernate.EmptyInterceptor;

/**
 * @author lazyman
 */
public class EntityStateInterceptor extends EmptyInterceptor {

    @Override
    public Boolean isTransient(Object entity) {
        if (entity instanceof EntityState) {
            return ((EntityState) entity).isTransient();
        }

        if (entity instanceof RObjectReference) {
            RObjectReference ref = (RObjectReference) entity;
            return isTransient(ref.getOwner());
        } else if (entity instanceof RAssignmentReference) {
            RAssignmentReference ref = (RAssignmentReference) entity;
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
        } else if (entity instanceof RUserPhoto) {
            RUserPhoto photo = (RUserPhoto) entity;
            return isTransient(photo.getOwner());
        }

        return null;
    }

    private Boolean isTransient(EntityState object) {
        Boolean trans = object != null ? object.isTransient() : null;
        if (Boolean.TRUE.equals(trans)) {
            return true;
        }

        return null;
    }
}
