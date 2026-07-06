package com.evolveum.midpoint.model.api.util;

import com.evolveum.midpoint.util.exception.CommonException;

@FunctionalInterface
public interface SmartIntegrationOperationExecutor<T, R> {

    R execute(T target)
            throws CommonException;
}
