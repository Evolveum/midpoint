package com.evolveum.midpoint.repo.sql.data.common.any;

/**
 * @author lazyman
 */
public interface RExtensionValue extends RAnyValue {

    RAnyContainer getAnyContainer();

    void setAnyContainer(RAnyContainer extension);
}
