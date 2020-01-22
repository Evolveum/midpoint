/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
