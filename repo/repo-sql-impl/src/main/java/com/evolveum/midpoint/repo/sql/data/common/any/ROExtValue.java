/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sql.data.common.any;

import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.id.ROExtBaseId;
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

    ROExtBaseId createId();
}
