package com.evolveum.midpoint.repo.sql.data.common.any;

import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.type.RObjectExtensionType;

/**
 * @author lazyman
 */
public interface ROExtValue<T> extends RAnyValue<T> {

    String OWNER = "owner";

    RObject getOwner();

    void setOwner(RObject object);

    RObjectExtensionType getOwnerType();

    void setOwnerType(RObjectExtensionType type);
}
