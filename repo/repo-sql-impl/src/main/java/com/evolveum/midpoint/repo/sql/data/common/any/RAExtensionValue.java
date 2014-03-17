package com.evolveum.midpoint.repo.sql.data.common.any;

/**
 * @author lazyman
 */
public interface RAExtensionValue extends RAnyValue {

    RAssignmentExtension getAnyContainer();

    void setAnyContainer(RAssignmentExtension extension);
}
