/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.data.common.any;

import com.evolveum.midpoint.repo.sql.data.common.type.RAssignmentExtensionType;

/**
 * @author lazyman
 */
public interface RAExtValue<T> extends RAnyValue<T> {

    String ANY_CONTAINER = "anyContainer";

    RAssignmentExtension getAnyContainer();

    void setAnyContainer(RAssignmentExtension extension);

    // not persistent (yet)
    RAssignmentExtensionType getExtensionType();

    void setExtensionType(RAssignmentExtensionType type);
}
