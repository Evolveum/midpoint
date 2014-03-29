package com.evolveum.midpoint.repo.sql.data.common.any;

import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.other.RObjectType;

/**
 * @author lazyman
 */
public interface ROExtValue extends RAnyValue {

    RObject getOwner();

    void setOwner(RObject object);

    RObjectType getOwnerType();

    void setOwnerType(RObjectType type);
}
