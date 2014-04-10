package com.evolveum.midpoint.repo.sql.data.common.any;

import com.evolveum.midpoint.repo.sql.data.common.type.RAssignmentExtensionType;

/**
 * @author lazyman
 */
public interface RAExtValue extends RAnyValue {

    String ANY_CONTAINER = "anyContainer";

    RAssignmentExtension getAnyContainer();

    void setAnyContainer(RAssignmentExtension extension);

    RAssignmentExtensionType getExtensionType();

    void setExtensionType(RAssignmentExtensionType type);
}
